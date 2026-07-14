package com.finanzen.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.AccountTypeAvatar
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.LabeledDropdown
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.accountTypeLabel
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.format.monthPeriod
import com.finanzen.ui.format.periodOfEpochDay
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.AccountsViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsTabScreen(vm: AccountsViewModel = koinViewModel()) {
    val accounts by vm.accounts.collectAsState()
    val archivedAccounts by vm.archivedAccounts.collectAsState()
    val balances by vm.balances.collectAsState()
    val cards by vm.cards.collectAsState()
    val plansByAccount by vm.plansByAccount.collectAsState()
    val unbilledByAccount by vm.unbilledCommitmentByAccount.collectAsState()
    val transactionsByAccount by vm.transactionsByAccount.collectAsState()
    val spacing = LocalSpacing.current
    var showForm by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var accountPendingHardDelete by remember { mutableStateOf<Account?>(null) }
    var archivedExpanded by remember { mutableStateOf(false) }
    var payingOffAccount by remember { mutableStateOf<Account?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var month by remember { mutableStateOf(LocalDate(today.year, today.month, 1)) }
    // Estado por cuenta (hoisted, no `remember` por ítem, para no perderse al reciclar filas del LazyColumn).
    val expandedAccountIds = remember { mutableStateMapOf<Long, Boolean>() }

    fun archiveWithUndo(account: Account) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.archive(account.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                "\"${account.name}\" archivada",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.unarchive(account.id)
        }
    }

    if (showForm || editingAccount != null) {
        val editing = editingAccount
        val existingNames = remember(accounts, archivedAccounts, editing) {
            (accounts + archivedAccounts).filter { it.id != editing?.id }
                .mapTo(mutableSetOf()) { it.name.trim().lowercase() }
        }
        AccountFormDialog(
            types = vm.accountTypes,
            baseCurrency = vm.baseCurrency,
            existing = editing,
            existingCard = editing?.let { cards[it.id] },
            existingNames = existingNames,
            onDismiss = {
                showForm = false
                editingAccount = null
            },
            onConfirm = { type, name, amount, limit, cutoff, due, interest ->
                if (editing == null) {
                    vm.addAccount(type, name, amount, limit, cutoff, due, interest)
                } else {
                    vm.updateAccount(editing.id, name, amount)
                    val cardId = cards[editing.id]?.id
                    if (editing.type == "CREDIT" && cardId != null) {
                        vm.updateCard(cardId, limit, cutoff, due, interest)
                    }
                }
                showForm = false
                editingAccount = null
            },
        )
    }

    accountPendingHardDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountPendingHardDelete = null },
            title = { Text("¿Eliminar \"${account.name}\"?") },
            text = { Text("Se eliminarán también sus movimientos y planes de cuotas asociados. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.delete(account.id)
                        accountPendingHardDelete = null
                    },
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { accountPendingHardDelete = null }) { Text("Cancelar") } },
        )
    }

    payingOffAccount?.let { account ->
        val debt = (-(balances[account.id] ?: 0L)).coerceAtLeast(0L)
        PayOffCardSheet(
            debtMinor = debt,
            currency = account.currency,
            sourceOptions = accounts.filter { it.id != account.id },
            onConfirm = { sourceAccountId, amount ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                cards[account.id]?.let { vm.payOffCard(it.id, sourceAccountId, amount) }
                payingOffAccount = null
            },
            onDismiss = { payingOffAccount = null },
        )
    }

    Scaffold(
        // El tope lo aporta MainTabHeader (única fuente del inset superior); el Scaffold se queda solo
        // por el FAB, así que neutralizamos sus insets de contenido.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Cuenta") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            MainTabHeader(title = "Cuentas")
            MonthSelector(
                label = formatMesAnio(month, LocalDateLocale.current),
                onPrev = { month = month.plus(DatePeriod(months = -1)) },
                onNext = { month = month.plus(DatePeriod(months = 1)) },
                modifier = Modifier.padding(horizontal = spacing.lg),
            )
            if (accounts.isEmpty() && archivedAccounts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Sin cuentas",
                        subtitle = "Usa el botón + para crear tu primera cuenta.",
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                val (nonCreditAccounts, creditAccounts) = remember(accounts) { accounts.partition { it.type != "CREDIT" } }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    items(nonCreditAccounts, key = { it.id }) { account ->
                        AccountRow(
                            account = account,
                            balanceMinor = balances[account.id] ?: account.openingBalanceMinor,
                            card = null,
                            plans = emptyList(),
                            unbilledMinor = 0L,
                            movements = transactionsByAccount[account.id].orEmpty(),
                            expanded = expandedAccountIds[account.id] == true,
                            onToggleExpand = { expandedAccountIds[account.id] = expandedAccountIds[account.id] != true },
                            month = month,
                            onArchive = { archiveWithUndo(account) },
                            onEdit = { editingAccount = account },
                            onPayOff = { payingOffAccount = account },
                        )
                    }

                    if (creditAccounts.isNotEmpty()) {
                        item(key = "credit_header") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xs))
                            Text(
                                "Tarjetas de crédito",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = spacing.xs),
                            )
                        }
                        items(creditAccounts, key = { it.id }) { account ->
                            AccountRow(
                                account = account,
                                balanceMinor = balances[account.id] ?: account.openingBalanceMinor,
                                card = cards[account.id],
                                plans = plansByAccount[account.id].orEmpty(),
                                unbilledMinor = unbilledByAccount[account.id] ?: 0L,
                                movements = transactionsByAccount[account.id].orEmpty(),
                                expanded = expandedAccountIds[account.id] == true,
                                onToggleExpand = { expandedAccountIds[account.id] = expandedAccountIds[account.id] != true },
                                month = month,
                                onArchive = { archiveWithUndo(account) },
                                onEdit = { editingAccount = account },
                                onPayOff = { payingOffAccount = account },
                            )
                        }
                    }

                    if (archivedAccounts.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { archivedExpanded = !archivedExpanded }
                                    .padding(vertical = spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            ) {
                                Icon(
                                    if (archivedExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = if (archivedExpanded) "Contraer" else "Expandir",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "Archivadas (${archivedAccounts.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (archivedExpanded) {
                            items(archivedAccounts, key = { it.id }) { account ->
                                ArchivedAccountRow(
                                    account = account,
                                    balanceMinor = balances[account.id] ?: account.openingBalanceMinor,
                                    onUnarchive = { vm.unarchive(account.id) },
                                    onHardDelete = { accountPendingHardDelete = account },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AccountRow(
    account: Account,
    balanceMinor: Long,
    card: Card?,
    plans: List<InstallmentPlan>,
    unbilledMinor: Long,
    movements: List<TransactionRow>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    month: LocalDate,
    onArchive: () -> Unit,
    onEdit: () -> Unit,
    onPayOff: () -> Unit,
) {
    val isCredit = account.type == "CREDIT"
    val finance = LocalFinanceColors.current
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current

    // Deslizar para archivar (mismo patrón que TransactionsScreen para eliminar movimientos):
    // libera toda la fila para nombre/saldo en vez de un ícono siempre visible.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onArchive()
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
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = spacing.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Archive, contentDescription = "Archivar", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        },
    ) {
        FinanceCard(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    AccountTypeAvatar(account.type)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            accountTypeLabel(account.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            fmt.format(balanceMinor, account.currency),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (balanceMinor < 0) finance.expense else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (isCredit) "deuda" else "saldo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "Contraer" else "Expandir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (isCredit && card != null) {
                    CreditDetails(card, balanceMinor, unbilledMinor, account.currency, onPayOff)
                }

                if (expanded) {
                    HorizontalDivider()
                    AccountMovementsSection(
                        movements = movements,
                        month = month,
                        currency = account.currency,
                        includeIncome = !isCredit,
                        plansById = if (isCredit) remember(plans) { plans.associateBy { it.id } } else emptyMap(),
                        installmentIndex = if (isCredit) remember(movements) { AccountsViewModel.installmentIndexByTransaction(movements) } else emptyMap(),
                    )
                }
            }
        }
    }
}

/** Sección de movimientos mensuales de una cuenta (lista + subtotal); el mes lo controla el selector global de la pantalla. */
@Composable
private fun AccountMovementsSection(
    movements: List<TransactionRow>,
    month: LocalDate,
    currency: String,
    includeIncome: Boolean,
    plansById: Map<Long, InstallmentPlan> = emptyMap(),
    installmentIndex: Map<Long, Long> = emptyMap(),
) {
    val spacing = LocalSpacing.current
    val period = remember(month) { monthPeriod(month) }
    val filtered = remember(movements, period, includeIncome) {
        movements.filter { periodOfEpochDay(it.date) == period && (includeIncome || it.kind == "EXPENSE") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (filtered.isEmpty()) {
            Text(
                "Sin movimientos este mes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                filtered.forEach { row ->
                    val plan = row.installmentPlanId?.let { plansById[it] }
                    val idx = row.installmentPlanId?.let { installmentIndex[row.id] }
                    val cuotaLabel = if (plan != null && idx != null) "Cuota $idx/${plan.installments}" else null
                    AccountMovementRow(row, cuotaLabel)
                }
            }
            HorizontalDivider()
            val finance = LocalFinanceColors.current
            if (includeIncome) {
                val income = filtered.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
                val expense = filtered.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
                MovementSummaryRow("Ingreso", income, currency, colorOverride = finance.income)
                MovementSummaryRow("Gasto", -expense, currency, colorOverride = finance.expense)
            } else {
                val expense = filtered.sumOf { it.amountMinor }
                MovementSummaryRow("Total", -expense, currency, colorOverride = finance.expense)
            }
        }
    }
}

@Composable
private fun MovementSummaryRow(label: String, amountMinor: Long, currency: String, colorOverride: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(
            amountMinor = amountMinor,
            currency = currency,
            style = MaterialTheme.typography.bodyMedium,
            signed = true,
            colorOverride = colorOverride,
        )
    }
}

/** Fila de movimiento de solo lectura dentro de la cuenta expandida (sin swipe/editar: solo consulta).
 * [cuotaLabel] (ej. "cuota 2/5") se agrega junto a la fecha cuando el gasto pertenece a un plan de cuotas. */
@Composable
private fun AccountMovementRow(row: TransactionRow, cuotaLabel: String? = null) {
    val dateLocale = LocalDateLocale.current
    val finance = LocalFinanceColors.current
    val isIncome = row.kind == "INCOME"
    val signedAmount = if (isIncome) row.amountMinor else -row.amountMinor
    val dateLine = formatDiaMes(row.date, dateLocale) + (cuotaLabel?.let { " · $it" } ?: "")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
    ) {
        KindAvatar(kind = row.kind, size = 32.dp)
        Column(Modifier.weight(1f)) {
            Text(row.note.ifBlank { kindLabel(row.kind) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(dateLine, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MoneyText(
            amountMinor = signedAmount,
            currency = row.currency,
            style = MaterialTheme.typography.bodyMedium,
            signed = true,
            colorOverride = if (isIncome) finance.income else finance.expense,
        )
    }
}

/** Fila de una cuenta archivada: avatar, nombre/tipo, saldo y las dos acciones (restaurar / borrado duro). */
@Composable
private fun ArchivedAccountRow(
    account: Account,
    balanceMinor: Long,
    onUnarchive: () -> Unit,
    onHardDelete: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            AccountTypeAvatar(account.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(
                    accountTypeLabel(account.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                fmt.format(balanceMinor, account.currency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onUnarchive) {
                Icon(Icons.Outlined.Unarchive, contentDescription = "Restaurar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onHardDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar permanentemente", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Cupo/disponible de una tarjeta de crédito como mini-barra + corte/pago/interés en una línea. */
@Composable
private fun CreditDetails(card: Card, balanceMinor: Long, unbilledMinor: Long, currency: String, onPayOff: () -> Unit) {
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val limit = card.creditLimitMinor
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("¿Cómo se calcula el disponible?") },
            text = {
                Text(
                    "Tu cupo disponible ya descuenta el total de tus compras a cuotas activas, no solo lo " +
                        "cobrado hasta ahora — así se refleja el compromiso real con el banco.",
                )
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Entendido") } },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (limit != null && limit > 0) {
            val debt = (-balanceMinor).coerceAtLeast(0L)
            val available = limit + balanceMinor - unbilledMinor // = cupo − deuda facturada − compromiso sin facturar
            val frac = ((debt + unbilledMinor).toFloat() / limit.toFloat()).coerceIn(0f, 1f)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Disponible ${fmt.format(available, currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    IconButton(onClick = { showInfo = true }, modifier = Modifier.size(18.dp)) {
                        Icon(
                            Icons.Outlined.HelpOutline,
                            contentDescription = "¿Cómo se calcula el disponible?",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    "Cupo ${fmt.format(limit, currency)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth(),
                color = if (frac >= 0.9f) finance.expense else MaterialTheme.colorScheme.primary,
                drawStopIndicator = {},
            )
            if (available < limit) {
                TextButton(onClick = onPayOff, modifier = Modifier.fillMaxWidth()) { Text("Pagar tarjeta") }
            }
        }
        val meta = buildList {
            card.cutoffDay?.let { add("Corte $it") }
            card.dueDay?.let { add("Pago $it") }
            card.interestRate?.let { if (it > 0.0) add("$it%") }
        }
        if (meta.isNotEmpty()) {
            Text(
                meta.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hoja para pagar la tarjeta: transfiere el saldo adeudado desde otra cuenta y libera el cupo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayOffCardSheet(
    debtMinor: Long,
    currency: String,
    sourceOptions: List<Account>,
    onConfirm: (sourceAccountId: Long, amountMinor: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    var sourceAccountId by remember { mutableStateOf(sourceOptions.firstOrNull()?.id) }
    val selectedSource = sourceOptions.firstOrNull { it.id == sourceAccountId }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text("Pagar tarjeta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Se transferirá ${fmt.format(debtMinor, currency)} desde la cuenta elegida para saldar tu " +
                    "tarjeta. Las cuotas pendientes de tus compras a plazos dejarán de cobrarse " +
                    "automáticamente y tu cupo quedará disponible.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PickerField(
                label = "Cuenta origen",
                options = sourceOptions,
                selected = selectedSource,
                optionLabel = { "${it.name} (${it.currency})" },
                trailingLabel = { accountTypeLabel(it.type) },
                onSelect = { sourceAccountId = it.id },
                placeholder = "Selecciona cuenta",
                emptyHint = "Necesitas otra cuenta para pagar la tarjeta.",
            )
            Button(
                onClick = { selectedSource?.let { onConfirm(it.id, debtMinor) } },
                enabled = selectedSource != null && debtMinor > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Confirmar pago") }
        }
    }
}

/** Alta/edición de cuenta: el formulario cambia según el tipo elegido. ModalBottomSheet en vez de
 * AlertDialog porque son hasta 6 campos — un modal angosto los aprieta demasiado. Abre expandido del
 * todo (`skipPartiallyExpanded`) para que los 6 campos de crédito no queden a medias sin arrastrar.
 *
 * Si [existing] no es null, el formulario abre en modo edición con todos los campos precargados
 * (nombre, monto inicial, y para CREDIT cupo/corte/pago/tasa), igual que al crear — salvo el tipo de
 * cuenta, que queda bloqueado: cambiarlo dejaría huérfana la `Card` asociada o volvería inconsistente
 * el historial de movimientos ya registrados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFormDialog(
    types: List<String>,
    baseCurrency: String,
    existing: Account? = null,
    existingCard: Card? = null,
    existingNames: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (type: String, name: String, amountMinor: Long, limit: Long?, cutoff: Long?, due: Long?, interest: Double?) -> Unit,
) {
    val isEditing = existing != null
    var type by remember { mutableStateOf(existing?.type ?: types.first()) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amountMinor by remember { mutableStateOf(existing?.openingBalanceMinor ?: 0L) }
    var limitMinor by remember { mutableStateOf(existingCard?.creditLimitMinor ?: 0L) }
    var cutoff by remember { mutableStateOf(existingCard?.cutoffDay?.toString() ?: "") }
    var due by remember { mutableStateOf(existingCard?.dueDay?.toString() ?: "") }
    var interest by remember { mutableStateOf(existingCard?.interestRate?.toString() ?: "") }

    val isCredit = type == "CREDIT"
    val nameTaken = name.isNotBlank() && name.trim().lowercase() in existingNames
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (isEditing) "Editar cuenta" else "Nueva cuenta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (isEditing) {
                Text(
                    "Tipo: ${accountTypeLabel(type)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LabeledDropdown(
                    label = "Tipo",
                    options = types,
                    selected = type,
                    optionLabel = { accountTypeLabel(it) },
                    onSelect = { type = it },
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                isError = nameTaken,
                supportingText = if (nameTaken) {
                    { Text("Ya existe una cuenta con ese nombre") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (isCredit) {
                MoneyField(limitMinor, { limitMinor = it }, currencyCode = baseCurrency, label = "Cupo")
                NumberField(cutoff, "Día de corte (opcional, 1–31)", maxLen = 2) { cutoff = it }
                NumberField(due, "Día de pago (opcional, 1–31)", maxLen = 2) { due = it }
                DecimalField(interest, "Tasa de interés % (opcional)") { interest = it }
            } else {
                MoneyField(amountMinor, { amountMinor = it }, currencyCode = baseCurrency, label = "Monto inicial")
            }
            Button(
                onClick = {
                    onConfirm(
                        type,
                        name,
                        if (isCredit) 0 else amountMinor,
                        if (isCredit) limitMinor else null,
                        if (isCredit) cutoff.toLongOrNull() else null,
                        if (isCredit) due.toLongOrNull() else null,
                        if (isCredit) interest.toDoubleOrNull() else null,
                    )
                },
                enabled = name.isNotBlank() && !nameTaken,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isEditing) "Guardar cambios" else "Crear cuenta") }
        }
    }
}

@Composable
private fun DecimalField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(value: String, label: String, maxLen: Int, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(maxLen)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
