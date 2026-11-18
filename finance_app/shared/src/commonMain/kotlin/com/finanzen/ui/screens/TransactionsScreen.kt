package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finanzen.data.CurrencyLocaleInfo
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.AvatarTone
import com.finanzen.ui.components.BottomSnackbarHost
import com.finanzen.ui.components.CategoryAvatar
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FabClearance
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.ListRow
import com.finanzen.ui.components.LoadingState
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.Overline
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.SageTextField
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SegmentedControl
import com.finanzen.ui.components.SwipeToDeleteRow
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.components.signedAmountAndColor
import com.finanzen.ui.format.diaSemana
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.format.monthPeriod
import com.finanzen.ui.format.periodOfEpochDay
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.TransactionsViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private fun dayIncome(rows: List<TransactionRow>): Long = rows.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }

private fun dayExpense(rows: List<TransactionRow>): Long = rows.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

@Composable
fun TransactionsScreen(
    onEdit: (Long) -> Unit,
    vm: TransactionsViewModel = koinViewModel(),
) {
    val rows by vm.transactions.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.allAccounts.collectAsState()
    val spacing = LocalSpacing.current
    val dateLocale = LocalDateLocale.current
    var query by remember { mutableStateOf("") }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val todayEpochDay = remember(today) { today.toEpochDays() }
    var month by remember { mutableStateOf(LocalDate(today.year, today.month, 1)) }
    var searchOpen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(TxFilter.All) }
    var pendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var blockedDelete by remember { mutableStateOf<TransactionsViewModel.DeleteBlock?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val period = monthPeriod(month)

    val groups = remember(rows, query, period, pendingDeleteIds, filter) {
        rows.asSequence()
            .filter { it.id !in pendingDeleteIds }
            .filter { periodOfEpochDay(it.date) == period }
            .filter {
                when (filter) {
                    TxFilter.All -> true
                    TxFilter.Income -> it.kind == "INCOME"
                    TxFilter.Expense -> it.kind == "EXPENSE"
                }
            }
            .filter { query.isBlank() || it.note.contains(query, ignoreCase = true) }
            .groupBy { it.date }
            .entries.sortedByDescending { it.key }
    }
    val searching = query.isNotBlank()

    fun requestDelete(id: Long) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        pendingDeleteIds = pendingDeleteIds + id
        scope.launch {
            // finally: salir de la pantalla cancela la corrutina antes de que el snackbar termine;
            // el borrado se confirma igual salvo que el usuario haya tocado "Deshacer".
            var undone = false
            try {
                undone = snackbarHostState.showSnackbar(
                    message = "Movimiento eliminado",
                    actionLabel = "Deshacer",
                    duration = SnackbarDuration.Short,
                ) == SnackbarResult.ActionPerformed
            } finally {
                if (!undone) vm.delete(id)
                pendingDeleteIds = pendingDeleteIds - id
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MainTabHeader(
                title = "Movimientos",
                action = {
                    IconButton(onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    }) {
                        Icon(if (searchOpen) LucideIcons.X else LucideIcons.Search, contentDescription = if (searchOpen) "Cerrar búsqueda" else "Buscar")
                    }
                },
            )
            ScreenList(gap = spacing.lg, bottomPadding = FabClearance) {
                item(key = "filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (searchOpen) {
                            SageTextField(
                                value = query,
                                onValueChange = { query = it },
                                label = "Buscar",
                                placeholder = "Descripción del movimiento",
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        SegmentedControl(
                            options = TxFilter.entries.toList(),
                            selected = filter,
                            onSelect = { filter = it },
                            label = { it.label },
                        )
                        MonthSelector(
                            label = formatMesAnio(month, dateLocale),
                            onPrev = { month = month.plus(DatePeriod(months = -1)) },
                            onNext = { month = month.plus(DatePeriod(months = 1)) },
                            onLabelClick = { month = LocalDate(today.year, today.month, 1) },
                        )
                    }
                }
                when {
                    !loaded -> item(key = "loading") { LoadingState(rows = 5) }

                    groups.isEmpty() -> item(key = "empty") {
                        EmptyState(
                            icon = LucideIcons.Receipt,
                            title = if (searching || filter != TxFilter.All) "Sin resultados" else "Sin movimientos",
                            subtitle = if (searching || filter != TxFilter.All) {
                                "Prueba con otra búsqueda, otro filtro o cambia de mes."
                            } else {
                                "Usa el botón + para registrar tu primer movimiento."
                            },
                        )
                    }

                    else -> groups.forEach { (day, dayRows) ->
                        item(key = "day_$day") {
                            Column(Modifier.fillMaxWidth().animateItem(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DayHeader(day, todayEpochDay, dayRows, dateLocale)
                                GroupCard {
                                    dayRows.forEachIndexed { index, row ->
                                        key(row.id) {
                                            if (index > 0) RowDivider()
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
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        BottomSnackbarHost(snackbarHostState, aboveFab = true)
    }

    blockedDelete?.let { block ->
        ConfirmDialog(
            title = "No se puede eliminar",
            body = when (block) {
                is TransactionsViewModel.DeleteBlock.Subscription ->
                    "\"${block.name}\" es una suscripción periódica. Elimínala desde Suscripciones; después podrás borrar este movimiento aquí."

                is TransactionsViewModel.DeleteBlock.Investment ->
                    "\"${block.name}\" es un aporte periódico a un fondo. Gestiónalo desde Fondos de inversión."
            },
            confirmLabel = "Entendido",
            onConfirm = { blockedDelete = null },
            onDismiss = { blockedDelete = null },
            destructive = false,
        )
    }
}

/** Filtro por tipo de movimiento. */
private enum class TxFilter(val label: String) {
    All("Todos"),
    Income("Ingresos"),
    Expense("Gastos"),
}

/** Cabecera de un día: "HOY · 16 NOV" (o "SÁBADO · 14 NOV") y el neto del día a la derecha. */
@Composable
private fun DayHeader(day: Long, todayEpochDay: Long, dayRows: List<TransactionRow>, dateLocale: CurrencyLocaleInfo) {
    val label = when (day) {
        todayEpochDay -> "Hoy"
        todayEpochDay - 1 -> "Ayer"
        else -> diaSemana(day)
    }
    val net = dayIncome(dayRows) - dayExpense(dayRows)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Overline("$label · ${formatDiaMes(day, dateLocale)}", modifier = Modifier.semantics { heading() })
        if (net != 0L) {
            MoneyText(
                amountMinor = net,
                currency = dayRows.first().currency,
                style = LocalSageType.current.amountSm,
                signed = true,
                colorOverride = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
    val isAdjustment = row.kind == "ADJUSTMENT"
    val isInvestment = row.investmentId != null
    val finance = LocalFinanceColors.current
    val (signedAmount, amountColor) = signedAmountAndColor(row.kind, row.amountMinor, finance)
    val tone = when {
        isTransfer || isAdjustment || isInvestment -> AvatarTone.Accent
        row.kind == "INCOME" -> AvatarTone.Income
        else -> AvatarTone.Expense
    }
    val subtitle = listOfNotNull(
        parentCategory?.name ?: category?.name ?: kindLabel(row.kind),
        account?.name,
        "Periódico".takeIf { isSubscription },
    ).joinToString(" · ")

    SwipeToDeleteRow(onSwipeToDelete = onSwipeToDelete, modifier = modifier, shape = RectangleShape) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            ListRow(
                title = if (isTransfer) row.note.ifBlank { "Transferencia" } else row.note.ifBlank { category?.name ?: kindLabel(row.kind) },
                subtitle = subtitle,
                onClick = onClick,
                leading = {
                    when {
                        // La suscripción se distingue aun si tiene categoría.
                        isSubscription -> KindAvatar(kind = row.kind, icon = Icons.Outlined.Autorenew)

                        // Transferencias, ajustes e inversiones usan una categoría del sistema: su
                        // inicial no aporta, así que se usa la de la nota.
                        isTransfer || isAdjustment || isInvestment -> CategoryAvatar(name = row.note.ifBlank { kindLabel(row.kind) }, tone = tone)

                        category != null -> CategoryAvatar(name = category.name, tone = tone)

                        else -> KindAvatar(kind = row.kind)
                    }
                },
                trailing = {
                    if (isTransfer) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(LucideIcons.ArrowLeftRight, contentDescription = "Transferencia", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Text(
                                LocalMoneyFormat.current.format(row.amountMinor, row.currency),
                                style = LocalSageType.current.amountMd,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        MoneyText(
                            amountMinor = signedAmount,
                            currency = row.currency,
                            style = LocalSageType.current.amountMd,
                            signed = true,
                            colorOverride = amountColor,
                        )
                    }
                },
            )
        }
    }
}
