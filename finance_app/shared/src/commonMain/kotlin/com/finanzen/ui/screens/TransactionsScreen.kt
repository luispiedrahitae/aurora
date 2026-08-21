package com.finanzen.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.components.signedAmountAndColor
import com.finanzen.ui.format.diaSemana
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.format.monthPeriod
import com.finanzen.ui.format.periodOfEpochDay
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

private fun dayIncome(rows: List<TransactionRow>): Long = rows.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }

private fun dayExpense(rows: List<TransactionRow>): Long = rows.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

// Tarjeta de día armada con 3 piezas (cabecera, filas, resumen) que comparten el mismo
// fondo (surfaceContainer) pero solo redondean sus bordes externos, para que juntas se
// vean como una sola tarjeta redondeada en vez de una lista plana.
private val CardRadius = 16.dp // mismo radio que MaterialTheme.shapes.medium
private val DayCardTopShape = RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius)
private val DayCardBottomShape = RoundedCornerShape(bottomStart = CardRadius, bottomEnd = CardRadius)
private val DayCardFullShape = RoundedCornerShape(CardRadius)

@Composable
fun TransactionsScreen(
    onEdit: (Long) -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val rows by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.allAccounts.collectAsState()
    val spacing = LocalSpacing.current
    val dateLocale = LocalDateLocale.current
    var query by remember { mutableStateOf("") }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val todayEpochDay = remember(today) { today.toEpochDays().toLong() }
    var month by remember { mutableStateOf(LocalDate(today.year, today.month, 1)) }
    // Días expandidos (acordeón). Por defecto solo el día de hoy abre; el resto queda
    // colapsado. Ausente en el mapa = usa el default; una vez tocado, el valor explícito manda.
    val expandedDays = remember { mutableStateMapOf<Long, Boolean>() }
    var pendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var blockedDelete by remember { mutableStateOf<TransactionsViewModel.DeleteBlock?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val period = monthPeriod(month)

    val groups = remember(rows, query, period, pendingDeleteIds) {
        rows.asSequence()
            .filter { it.id !in pendingDeleteIds }
            .filter { periodOfEpochDay(it.date) == period }
            .filter { query.isBlank() || it.note.contains(query, ignoreCase = true) }
            .groupBy { it.date }
            .entries.sortedByDescending { it.key }
    }
    val searching = query.isNotBlank()

    fun requestDelete(id: Long) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        pendingDeleteIds = pendingDeleteIds + id
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Movimiento eliminado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                pendingDeleteIds = pendingDeleteIds - id
            } else {
                vm.delete(id)
                pendingDeleteIds = pendingDeleteIds - id
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            MainTabHeader(title = "Movimientos")
            Column(Modifier.fillMaxSize().padding(horizontal = spacing.lg)) {
                MonthSelector(
                    label = formatMesAnio(month, dateLocale),
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
                    // ponytail: alineado al shape.small (12dp) del resto de inputs; una unificación
                    // vía wrapper FinanZenTextField queda pendiente si aparecen más outliers.
                    shape = MaterialTheme.shapes.small,
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
                            val expanded = searching || (expandedDays[day] ?: (day == todayEpochDay))
                            val d = LocalDate.fromEpochDays(day.toInt())
                            item(key = "h_$day") {
                                DayHeader(
                                    dayNumber = d.dayOfMonth.toString().padStart(2, '0'),
                                    weekday = diaSemana(day),
                                    count = dayRows.size,
                                    incomeMinor = dayIncome(dayRows),
                                    expenseMinor = dayExpense(dayRows),
                                    currency = dayRows.first().currency,
                                    expanded = expanded,
                                    onToggle = { expandedDays[day] = !(expandedDays[day] ?: (day == todayEpochDay)) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            if (expanded) {
                                items(dayRows, key = { it.id }) { row ->
                                    val subcategory = categoriesById[row.categoryId]
                                    TransactionItem(
                                        row = row,
                                        category = subcategory,
                                        parentCategory = subcategory?.parentId?.let { categoriesById[it] },
                                        account = accountsById[row.accountId],
                                        onClick = { onEdit(row.id) },
                                        onSwipeToDelete = {
                                            val block = vm.deleteBlockReason(row.id)
                                            if (block != null) {
                                                blockedDelete = block
                                                false
                                            } else {
                                                requestDelete(row.id)
                                                true
                                            }
                                        },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                                item(key = "f_$day") {
                                    DaySummaryFooter(
                                        incomeMinor = dayIncome(dayRows),
                                        expenseMinor = dayExpense(dayRows),
                                        currency = dayRows.first().currency,
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                            item(key = "sp_$day") { Spacer(Modifier.height(spacing.sm)) }
                        }
                    }
                }
            }
        }
    }

    blockedDelete?.let { block ->
        AlertDialog(
            onDismissRequest = { blockedDelete = null },
            title = { Text("No se puede eliminar") },
            text = {
                Text(
                    when (block) {
                        is TransactionsViewModel.DeleteBlock.Subscription ->
                            "\"${block.name}\" es una suscripción periódica. Elimínala desde la pantalla de Suscripciones — una vez eliminada, podrás borrar este movimiento aquí."
                        is TransactionsViewModel.DeleteBlock.Investment ->
                            "\"${block.name}\" es una inversión periódica. Gestiónala desde la pantalla de Inversiones."
                    },
                )
            },
            confirmButton = { TextButton(onClick = { blockedDelete = null }) { Text("Entendido") } },
        )
    }
}

/** Cabecera de la tarjeta de día: número de día + nombre del día + cantidad de movimientos. */
@Composable
private fun DayHeader(
    dayNumber: String,
    weekday: String,
    count: Int,
    incomeMinor: Long,
    expenseMinor: Long,
    currency: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(if (expanded) DayCardTopShape else DayCardFullShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onToggle)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            dayNumber,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(Modifier.weight(1f)) {
            Text(
                weekday.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (count == 1) "1 movimiento" else "$count movimientos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!expanded) {
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    amountMinor = incomeMinor,
                    currency = currency,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    signed = true,
                    colorOverride = finance.income,
                )
                MoneyText(
                    amountMinor = -expenseMinor,
                    currency = currency,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    signed = true,
                    colorOverride = finance.expense,
                )
            }
        }
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Contraer" else "Expandir",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Resumen del día (Ingreso/Gasto), visible solo cuando la tarjeta está expandida. */
@Composable
private fun DaySummaryFooter(
    incomeMinor: Long,
    expenseMinor: Long,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(DayCardBottomShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(spacing.sm))
        SummaryRow("Ingreso", incomeMinor, currency, colorOverride = finance.income)
        SummaryRow("Gasto", -expenseMinor, currency, colorOverride = finance.expense)
    }
}

@Composable
private fun SummaryRow(label: String, amountMinor: Long, currency: String, colorOverride: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(
            amountMinor = amountMinor,
            currency = currency,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            signed = true,
            colorOverride = colorOverride,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    row: TransactionRow,
    category: Category?,
    parentCategory: Category?,
    account: Account?,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val isTransfer = row.kind == "TRANSFER"
    val isSubscription = row.subscriptionId != null
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    val (signedAmount, amountColor) = signedAmountAndColor(row.kind, row.amountMinor, finance)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onSwipeToDelete()
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            when {
                // La suscripción se distingue aun si tiene categoría (igual que una transferencia
                // nunca se pierde detrás de su categoría) — de ahí que vaya antes del chequeo de categoría.
                isSubscription -> KindAvatar(kind = row.kind, icon = Icons.Outlined.Autorenew)
                category != null -> CategoryAvatar(icon = category.icon, size = 40.dp)
                else -> KindAvatar(kind = row.kind)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (isTransfer) row.note.ifBlank { "Transferencia" } else row.note.ifBlank { "(sin nota)" },
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Text(
                    parentCategory?.name ?: category?.name ?: kindLabel(row.kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isTransfer) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            LocalMoneyFormat.current.format(row.amountMinor, row.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (isSubscription) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Outlined.Autorenew,
                            contentDescription = null,
                            tint = finance.expense,
                            modifier = Modifier.size(16.dp),
                        )
                        MoneyText(
                            amountMinor = signedAmount,
                            currency = row.currency,
                            style = MaterialTheme.typography.titleMedium,
                            signed = true,
                            colorOverride = finance.expense,
                        )
                    }
                } else {
                    MoneyText(
                        amountMinor = signedAmount,
                        currency = row.currency,
                        style = MaterialTheme.typography.titleMedium,
                        signed = true,
                        colorOverride = amountColor,
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
