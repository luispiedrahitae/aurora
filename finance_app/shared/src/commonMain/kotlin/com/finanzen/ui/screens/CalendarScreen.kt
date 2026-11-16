package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.components.signedAmountAndColor
import com.finanzen.ui.format.formatFechaLarga
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private val WEEKDAYS = listOf("L", "M", "X", "J", "V", "S", "D")

private fun LocalDate.epochDay(): Long = toEpochDays().toLong()

private fun daysInMonth(firstOfMonth: LocalDate): Int = (firstOfMonth.plus(DatePeriod(months = 1)).toEpochDays() - firstOfMonth.toEpochDays())

@Composable
fun CalendarScreen(
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val rows by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val dateLocale = LocalDateLocale.current
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    // Guardados como epochDay: sobreviven a rotación/proceso (LocalDate no es Saveable por sí solo).
    var monthDay by rememberSaveable { mutableStateOf(LocalDate(today.year, today.month, 1).toEpochDays()) }
    var selectedDay by rememberSaveable { mutableStateOf(today.toEpochDays()) }
    val month = LocalDate.fromEpochDays(monthDay)
    val selected = LocalDate.fromEpochDays(selectedDay)

    // Al cambiar de mes, la selección se muda con él (hoy si es el mes actual, si no el día 1): antes
    // la lista seguía mostrando un día de un mes que ya no estaba en la grilla.
    fun goToMonth(m: LocalDate) {
        monthDay = m.toEpochDays()
        selectedDay = (if (m.year == today.year && m.month == today.month) today else m).toEpochDays()
    }

    val byDay = remember(rows) { rows.groupBy { it.date } }
    // Neto por día (ingreso − gasto; las transferencias no cuentan) para el color del punto.
    val netByDay = remember(rows) {
        rows.groupBy { it.date }.mapValues { (_, list) ->
            list.sumOf {
                when (it.kind) {
                    "INCOME" -> it.amountMinor
                    "EXPENSE" -> -it.amountMinor
                    else -> 0L
                }
            }
        }
    }
    val selectedRows = byDay[selected.epochDay()].orEmpty()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Calendario", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthSelector(
                    label = formatMesAnio(month, dateLocale),
                    onPrev = { goToMonth(month.plus(DatePeriod(months = -1))) },
                    onNext = { goToMonth(month.plus(DatePeriod(months = 1))) },
                    onLabelClick = { goToMonth(LocalDate(today.year, today.month, 1)) },
                )
            }

            item {
                // Padding horizontal reducido: deja cada celda en ~45dp de ancho en un teléfono de 360dp.
                FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            WEEKDAYS.forEach { d ->
                                Text(
                                    d,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        MonthGrid(
                            month = month,
                            today = today,
                            selected = selected,
                            netByDay = netByDay,
                            onSelect = { selectedDay = it.toEpochDays() },
                        )
                    }
                }
            }

            // Qué día se está viendo y su neto: sin esto la lista de abajo no decía a qué fecha pertenecía.
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatFechaLarga(selected, dateLocale),
                        modifier = Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    netByDay[selected.epochDay()]?.let { net ->
                        MoneyText(net, currency = selectedRows.firstOrNull()?.currency ?: "", signed = true)
                    }
                }
            }

            if (selectedRows.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = LucideIcons.Receipt,
                            title = "Sin movimientos este día",
                            subtitle = "Toca otro día del calendario o registra uno con el botón + de Movimientos.",
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }
            } else {
                items(selectedRows, key = { it.id }) { row ->
                    val subcategory = categoriesById[row.categoryId]
                    DayTxRow(
                        row,
                        category = subcategory,
                        parentCategory = subcategory?.parentId?.let { categoriesById[it] },
                        onClick = { onEdit(row.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    netByDay: Map<Long, Long>,
    onSelect: (LocalDate) -> Unit,
) {
    val finance = LocalFinanceColors.current
    val dateLocale = LocalDateLocale.current
    val leadingBlanks = month.dayOfWeek.isoDayNumber - 1 // lunes = 0
    val total = daysInMonth(month)
    // Lista de celdas: nulls iniciales para alinear el primer día a su columna.
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..total).toList()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                // Rellena la última semana para que las celdas conserven el ancho.
                val padded = week + List(7 - week.size) { null }
                padded.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f).height(48.dp))
                    } else {
                        val date = LocalDate(month.year, month.month, day)
                        val isSelected = date == selected
                        val isToday = date == today
                        val net = netByDay[date.epochDay()]
                        // El punto de color no puede ser la única codificación: la celda anuncia el
                        // estado del día (sin movimientos / neto positivo / negativo / en cero).
                        val dayState = when {
                            net == null -> "sin movimientos"
                            net > 0L -> "neto positivo"
                            net < 0L -> "neto negativo"
                            else -> "neto en cero"
                        }
                        Box(
                            // El área táctil es la celda completa (~45×48dp); el círculo de 40dp queda
                            // como tratamiento visual de selección/hoy.
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .selectable(selected = isSelected, role = Role.Button) { onSelect(date) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = listOfNotNull(
                                        formatFechaLarga(date, dateLocale),
                                        "hoy".takeIf { isToday },
                                        dayState,
                                    ).joinToString(", ")
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                        } else if (isToday) {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    ),
                            ) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                val dotColor = when {
                                    net == null -> Color.Transparent
                                    net > 0 -> finance.income
                                    net < 0 -> finance.expense
                                    else -> finance.neutral
                                }
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTxRow(row: TransactionRow, category: Category?, parentCategory: Category?, onClick: () -> Unit) {
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val isTransfer = row.kind == "TRANSFER"
    val (signedAmount, amountColor) = signedAmountAndColor(row.kind, row.amountMinor, finance)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = "Editar", role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (category != null) {
            CategoryAvatar(name = category.name, size = 40.dp)
        } else {
            KindAvatar(kind = row.kind)
        }
        Column(Modifier.weight(1f)) {
            // Título = subcategoría (lo que el usuario eligió), como en Movimientos; nunca "(sin nota)".
            Text(
                if (isTransfer) "Transferencia" else category?.name ?: row.note.ifBlank { kindLabel(row.kind) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                parentCategory?.name ?: kindLabel(row.kind),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (isTransfer) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    LucideIcons.ArrowLeftRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    fmt.format(row.amountMinor, row.currency),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                fmt.format(signedAmount, row.currency, signed = true),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = amountColor,
            )
        }
    }
}
