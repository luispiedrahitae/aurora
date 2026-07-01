package com.finanzen.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.domain.Money
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.categoryColor
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val MESES_LARGOS = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

private fun dayHeaderLabel(epochDay: Long): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    return "${d.dayOfMonth} ${MESES[d.monthNumber - 1]}"
}

private fun monthPeriod(d: LocalDate): Long = (d.year * 100 + d.monthNumber).toLong()

private fun periodOfEpochDay(epochDay: Long): Long = monthPeriod(LocalDate.fromEpochDays(epochDay.toInt()))

/** Balance del día = ingresos − gastos (las transferencias no cuentan). */
private fun dayBalance(rows: List<TransactionRow>): Long = rows.sumOf {
    when (it.kind) {
        "INCOME" -> it.amountMinor
        "EXPENSE" -> -it.amountMinor
        else -> 0L
    }
}

private fun kindLabel(kind: String): String = when (kind) {
    "INCOME" -> "Ingreso"
    "TRANSFER" -> "Transferencia"
    else -> "Gasto"
}

@Composable
fun TransactionsScreen(
    onEdit: (Long) -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val rows by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val spacing = LocalSpacing.current
    var query by remember { mutableStateOf("") }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var month by remember { mutableStateOf(LocalDate(today.year, today.month, 1)) }
    // Días expandidos (acordeón). Ausente = cerrado; todos cerrados por defecto.
    val expandedDays = remember { mutableStateMapOf<Long, Boolean>() }

    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val period = monthPeriod(month)

    val groups = remember(rows, query, period) {
        rows.asSequence()
            .filter { periodOfEpochDay(it.date) == period }
            .filter { query.isBlank() || it.note.contains(query, ignoreCase = true) }
            .groupBy { it.date }
            .entries.sortedByDescending { it.key }
    }
    val searching = query.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        MainTabHeader(title = "Movimientos")
        Column(Modifier.fillMaxSize().padding(horizontal = spacing.lg)) {
            MonthSelector(
                label = "${MESES_LARGOS[month.monthNumber - 1]} ${month.year}",
                onPrev = { month = month.plus(DatePeriod(months = -1)) },
                onNext = { month = month.plus(DatePeriod(months = 1)) },
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = spacing.sm),
                placeholder = { Text("Buscar por nota") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            if (groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = if (searching) "Sin resultados" else "Sin movimientos",
                        subtitle = if (searching) "Prueba con otra búsqueda o cambia de mes." else "Usa el botón + para registrar tu primer movimiento.",
                        modifier = Modifier.padding(spacing.xl),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp, top = spacing.xs),
                ) {
                    groups.forEach { (day, dayRows) ->
                        // Con búsqueda activa se muestran expandidos para ver los resultados.
                        val expanded = searching || (expandedDays[day] == true)
                        item(key = "h_$day") {
                            DayHeader(
                                label = dayHeaderLabel(day),
                                balanceMinor = dayBalance(dayRows),
                                currency = dayRows.first().currency,
                                expanded = expanded,
                                onToggle = { expandedDays[day] = !(expandedDays[day] == true) },
                            )
                        }
                        if (expanded) {
                            items(dayRows, key = { it.id }) { row ->
                                TransactionItem(
                                    row = row,
                                    category = row.categoryId?.let { categoriesById[it] },
                                    account = accountsById[row.accountId],
                                    onClick = { onEdit(row.id) },
                                    onDelete = { vm.delete(row.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = LocalSpacing.current.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mes anterior")
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Mes siguiente")
        }
    }
}

/** Cabecera de día (pestaña del acordeón): fecha + balance del día + chevron. */
@Composable
private fun DayHeader(
    label: String,
    balanceMinor: Long,
    currency: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Contraer" else "Expandir",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        MoneyText(
            amountMinor = balanceMinor,
            currency = currency,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            signed = true,
            showCurrency = false,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    row: TransactionRow,
    category: Category?,
    account: Account?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val isTransfer = row.kind == "TRANSFER"
    val isIncome = row.kind == "INCOME"
    val signedAmount = if (isIncome) row.amountMinor else -row.amountMinor
    val spacing = LocalSpacing.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = spacing.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onClick)
                .padding(vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (category != null) {
                CategoryAvatar(icon = category.icon, color = categoryColor(category.name, category.color))
            } else {
                KindAvatar(kind = row.kind)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (isTransfer) row.note.ifBlank { "Transferencia" } else row.note.ifBlank { "(sin nota)" },
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Text(
                    category?.name ?: kindLabel(row.kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isTransfer) {
                    Text(
                        "⇄ ${Money(row.amountMinor, row.currency).format()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MoneyText(
                        amountMinor = signedAmount,
                        currency = row.currency,
                        style = MaterialTheme.typography.titleMedium,
                        signed = true,
                        showCurrency = false,
                    )
                }
                if (account != null) {
                    // Cuenta usada, con el mismo estilo que la descripción del movimiento.
                    Text(
                        account.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
