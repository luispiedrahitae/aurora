package com.finanzen.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.AccountTypeAvatar
import com.finanzen.ui.components.AddFab
import com.finanzen.ui.components.ArchivedSwipeRow
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.BottomSnackbarHost
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.DiscardChangesDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.ExpandableSection
import com.finanzen.ui.components.FabClearance
import com.finanzen.ui.components.FadeSection
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.KindAvatar
import com.finanzen.ui.components.LoadingState
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.Overline
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.ProgressBar
import com.finanzen.ui.components.ProgressTone
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.SageButtonKind
import com.finanzen.ui.components.SageTextField
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SelectorPill
import com.finanzen.ui.components.SheetHeader
import com.finanzen.ui.components.SwipeToArchive
import com.finanzen.ui.components.accountTypeLabel
import com.finanzen.ui.components.balanceColor
import com.finanzen.ui.components.kindLabel
import com.finanzen.ui.components.progressColor
import com.finanzen.ui.components.rememberGuardedSheetState
import com.finanzen.ui.components.signedAmountAndColor
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
import com.finanzen.viewmodel.AccountsViewModel
import com.finanzen.viewmodel.CategoriesViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsTabScreen(
    vm: AccountsViewModel = koinViewModel(),
    // Solo para leer categorías (flujo ya existente) sin ampliar el constructor de AccountsViewModel.
    categoriesVm: CategoriesViewModel = koinViewModel(),
) {
    val accounts by vm.accounts.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val categories by categoriesVm.categories.collectAsState()
    // Categoría de cada movimiento bajo su monto, con la misma regla que Movimientos: la principal si es subcategoría.
    val categoryLabels = remember(categories) {
        val byId = categories.associateBy { it.id }
        categories.associate { c -> c.id to (c.parentId?.let { byId[it] } ?: c).name }
    }
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

    // Restaurar avisa igual que archivar: sin esto la fila desaparecía de "Archivadas" sin explicación.
    fun restore(account: Account) {
        vm.unarchive(account.id)
        scope.launch { snackbarHostState.showSnackbar("\"${account.name}\" restaurada", duration = SnackbarDuration.Short) }
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
            currentAmountMinor = editing?.let { acc ->
                acc.openingBalanceMinor + transactionsByAccount[acc.id].orEmpty()
                    .filter { it.kind == "ADJUSTMENT" }
                    .sumOf { it.amountMinor }
            } ?: 0L,
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
        ConfirmDialog(
            title = "¿Eliminar \"${account.name}\"?",
            body = "Se eliminarán también sus movimientos y planes de cuotas asociados. Esta acción no se puede deshacer.",
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.delete(account.id)
                accountPendingHardDelete = null
            },
            onDismiss = { accountPendingHardDelete = null },
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
                scope.launch { snackbarHostState.showSnackbar("Tarjeta \"${account.name}\" pagada", duration = SnackbarDuration.Short) }
            },
            onDismiss = { payingOffAccount = null },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MainTabHeader(title = "Cuentas")
            if (!loaded) {
                LoadingState(Modifier.padding(horizontal = spacing.lg))
            } else if (accounts.isEmpty() && archivedAccounts.isEmpty()) {
                EmptyState(
                    icon = LucideIcons.Wallet,
                    title = "Sin cuentas",
                    subtitle = "Crea tu primera cuenta para empezar a registrar movimientos.",
                    actionLabel = "Crear cuenta",
                    onAction = { showForm = true },
                )
            } else {
                val (nonCreditAccounts, creditAccounts) = remember(accounts) { accounts.partition { it.type != "CREDIT" } }
                val accountRow: @Composable (Account) -> Unit = { account ->
                    val isCredit = account.type == "CREDIT"
                    AccountRow(
                        account = account,
                        balanceMinor = balances[account.id] ?: account.openingBalanceMinor,
                        card = if (isCredit) cards[account.id] else null,
                        plans = if (isCredit) plansByAccount[account.id].orEmpty() else emptyList(),
                        unbilledMinor = if (isCredit) unbilledByAccount[account.id] ?: 0L else 0L,
                        movements = transactionsByAccount[account.id].orEmpty(),
                        categoryLabels = categoryLabels,
                        expanded = expandedAccountIds[account.id] == true,
                        onToggleExpand = { expandedAccountIds[account.id] = expandedAccountIds[account.id] != true },
                        month = month,
                        onArchive = { archiveWithUndo(account) },
                        onEdit = { editingAccount = account },
                        onPayOff = { payingOffAccount = account },
                    )
                }
                ScreenList(gap = spacing.xl, bottomPadding = FabClearance) {
                    item(key = "summary") {
                        val cash = nonCreditAccounts.sumOf { balances[it.id] ?: it.openingBalanceMinor }
                        val debt = creditAccounts.sumOf { balances[it.id] ?: it.openingBalanceMinor }
                        AccountsSummary(cash, debt, vm.baseCurrency)
                    }
                    item(key = "month") {
                        Column {
                            MonthSelector(
                                label = formatMesAnio(month, LocalDateLocale.current),
                                onPrev = { month = month.plus(DatePeriod(months = -1)) },
                                onNext = { month = month.plus(DatePeriod(months = 1)) },
                                onLabelClick = { month = LocalDate(today.year, today.month, 1) },
                            )
                            Text(
                                "Abre una cuenta para ver sus movimientos de este mes. Mantén pulsado para editarla; desliza para archivarla.",
                                style = LocalSageType.current.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (nonCreditAccounts.isNotEmpty()) {
                        item(key = "accounts_header") { SectionHeader("Cuentas") }
                        items(nonCreditAccounts, key = { it.id }) { accountRow(it) }
                    }
                    if (creditAccounts.isNotEmpty()) {
                        item(key = "credit_header") { SectionHeader("Tarjetas de crédito") }
                        items(creditAccounts, key = { it.id }) { accountRow(it) }
                    }
                    if (archivedAccounts.isNotEmpty()) {
                        item(key = "archived_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(role = Role.Button, onClickLabel = if (archivedExpanded) "Contraer" else "Expandir") {
                                        archivedExpanded = !archivedExpanded
                                    }
                                    .padding(vertical = spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            ) {
                                SectionHeader("Archivadas (${archivedAccounts.size})", Modifier.weight(1f))
                                Icon(
                                    if (archivedExpanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (archivedExpanded) {
                            items(archivedAccounts, key = { it.id }) { account ->
                                ArchivedAccountRow(
                                    account = account,
                                    balanceMinor = balances[account.id] ?: account.openingBalanceMinor,
                                    onUnarchive = { restore(account) },
                                    onHardDelete = { accountPendingHardDelete = account },
                                )
                            }
                        }
                    }
                }
            }
        }
        AddFab(
            label = "Nueva cuenta",
            onClick = { showForm = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
        BottomSnackbarHost(snackbarHostState, aboveFab = true)
    }
}

/** Tarjeta resumen: saldo en cuentas (sin tarjetas), deuda en tarjetas y lo que queda neto. */
@Composable
private fun AccountsSummary(cashMinor: Long, debtMinor: Long, currency: String) {
    FinanceCard(Modifier.fillMaxWidth(), size = BentoTileSize.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline("Saldo en cuentas")
            MoneyText(cashMinor, currency, style = LocalSageType.current.amountHero)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Deuda en tarjetas", style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(debtMinor, currency, style = LocalSageType.current.amountMd, signed = true, colorOverride = if (debtMinor < 0) LocalFinanceColors.current.expense else null)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Te queda neto", style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(cashMinor + debtMinor, currency, style = LocalSageType.current.amountMd)
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
    categoryLabels: Map<Long, String>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    month: LocalDate,
    onArchive: () -> Unit,
    onEdit: () -> Unit,
    onPayOff: () -> Unit,
) {
    val isCredit = account.type == "CREDIT"
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    val haptic = LocalHapticFeedback.current

    // Deslizar para archivar: libera toda la fila para nombre/saldo en vez de un ícono siempre visible.
    SwipeToArchive(onArchive = onArchive) {
        // Como el día en Movimientos (DayHeader): tocar la cabecera abre/cierra los movimientos del mes,
        // mantener pulsado edita y archivar es swipe. Misma estructura que InvestmentItem.
        FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClickLabel = if (expanded) "Contraer" else "Expandir",
                            onLongClickLabel = "Editar",
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEdit()
                            },
                            onClick = onToggleExpand,
                        )
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        AccountTypeAvatar(account.type)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.name, style = LocalSageType.current.titleCard)
                            Text(
                                accountTypeLabel(account.type),
                                style = LocalSageType.current.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // Como el DayHeader de Movimientos: abierto, el saldo baja al pie del historial.
                        FadeSection(!expanded) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    fmt.format(balanceMinor, account.currency),
                                    style = LocalSageType.current.amountMd,
                                    color = if (balanceMinor < 0) LocalFinanceColors.current.expense else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (isCredit) "Deuda" else "Saldo",
                                    style = LocalSageType.current.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // CreditActions no pinta nada sin cupo ni fechas: sin esto quedaría un divider suelto.
                if (isCredit && card != null && ((card.creditLimitMinor ?: 0L) > 0 || card.cutoffDay != null || card.dueDay != null)) {
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        CreditActions(card, balanceMinor, unbilledMinor, account.currency, onPayOff)
                    }
                }
                ExpandableSection(expanded) {
                    // Sin divisor bajo la cabecera, como la tarjeta de día en Movimientos.
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = spacing.md, top = spacing.md)) {
                        AccountMovementsSection(
                            movements = movements,
                            categoryLabels = categoryLabels,
                            month = month,
                            currency = account.currency,
                            includeIncome = !isCredit,
                            plansById = if (isCredit) remember(plans) { plans.associateBy { it.id } } else emptyMap(),
                            installmentIndex = if (isCredit) remember(movements) { AccountsViewModel.installmentIndexByTransaction(movements) } else emptyMap(),
                        ) {
                            MovementSummaryRow(
                                if (isCredit) "Deuda" else "Saldo",
                                balanceMinor,
                                account.currency,
                                colorOverride = balanceColor(balanceMinor),
                                signed = false,
                                modifier = movementTextIndent(),
                            )
                            // Editar visible (además de mantener pulsado, que nadie descubre solo).
                            Row(movementTextIndent()) { CardAction("Editar", trailing = LucideIcons.Pencil, onClick = onEdit) }
                        }
                    }
                }
            }
        }
    }
}

/** Sección de movimientos mensuales de una cuenta (lista + subtotal); el mes lo controla el selector global de la pantalla.
 * [footer] (Saldo/Deuda) va pegado a los totales, con las mismas separaciones que el pie del día en Movimientos. */
@Composable
private fun AccountMovementsSection(
    movements: List<TransactionRow>,
    categoryLabels: Map<Long, String>,
    month: LocalDate,
    currency: String,
    includeIncome: Boolean,
    plansById: Map<Long, InstallmentPlan> = emptyMap(),
    installmentIndex: Map<Long, Long> = emptyMap(),
    footer: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current
    val period = remember(month) { monthPeriod(month) }
    val filtered = remember(movements, period, includeIncome) {
        movements.filter { periodOfEpochDay(it.date) == period && (includeIncome || it.kind == "EXPENSE") }
    }

    val indent = movementTextIndent()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (filtered.isEmpty()) {
            Text(
                "Sin movimientos este mes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            filtered.forEach { row ->
                val plan = row.installmentPlanId?.let { plansById[it] }
                val idx = row.installmentPlanId?.let { installmentIndex[row.id] }
                val cuotaLabel = if (plan != null && idx != null) "Cuota $idx/${plan.installments}" else null
                AccountMovementRow(row, cuotaLabel, amountCaption = categoryLabels[row.categoryId])
            }
        }
        // Como DaySummaryFooter: divisor, un respiro y los totales pegados entre sí.
        Column {
            HorizontalDivider(indent)
            Spacer(Modifier.height(spacing.sm))
            val finance = LocalFinanceColors.current
            if (filtered.isNotEmpty() && includeIncome) {
                val income = filtered.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
                val expense = filtered.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
                MovementSummaryRow("Ingreso", income, currency, colorOverride = finance.income, modifier = indent)
                MovementSummaryRow("Gasto", -expense, currency, colorOverride = finance.expense, modifier = indent)
            } else if (filtered.isNotEmpty()) {
                MovementSummaryRow("Total", -filtered.sumOf { it.amountMinor }, currency, colorOverride = finance.expense, modifier = indent)
            }
            footer()
        }
    }
}

/** Sangría del avatar de [AccountMovementRow] (40dp + su spacedBy): líneas y totales arrancan donde el texto
 * (= nombre de la cuenta), como el pie de la tarjeta de día en Movimientos. */
@Composable
internal fun movementTextIndent(): Modifier = Modifier.padding(start = 40.dp + LocalSpacing.current.md)

/** Fila de total del pie (cuenta e inversión). [pct] (rendimiento) va como "(+N%)" antes del monto. */
@Composable
internal fun MovementSummaryRow(
    label: String,
    amountMinor: Long,
    currency: String,
    colorOverride: Color,
    signed: Boolean = true,
    modifier: Modifier = Modifier,
    pct: Double? = null,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = LocalSageType.current.titleCard, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (pct != null) {
            Text("(${if (pct >= 0) "+" else ""}${pct.toInt()}%)", style = MaterialTheme.typography.bodySmall, color = colorOverride)
        }
        MoneyText(
            amountMinor = amountMinor,
            currency = currency,
            style = LocalSageType.current.amountMd,
            signed = signed,
            colorOverride = colorOverride,
        )
    }
}

/** Fila de movimiento de solo lectura dentro de la cuenta expandida (sin swipe/editar: solo consulta).
 * [cuotaLabel] (ej. "cuota 2/5") se agrega junto a la fecha cuando el gasto pertenece a un plan de cuotas.
 * También la usa el historial de Inversiones, con [title] ("Aporte"/"Retiro") en lugar de la nota. */
@Composable
internal fun AccountMovementRow(
    row: TransactionRow,
    cuotaLabel: String? = null,
    title: String? = null,
    amountCaption: String? = null,
    modifier: Modifier = Modifier,
) {
    val dateLocale = LocalDateLocale.current
    val finance = LocalFinanceColors.current
    val (signedAmount, amountColor) = signedAmountAndColor(row.kind, row.amountMinor, finance)
    val dateLine = formatDiaMes(row.date, dateLocale) + (cuotaLabel?.let { " · $it" } ?: "")
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        // Avatar de Movimientos (32dp) centrado en la columna del icono de la cuenta (40dp), con su misma
        // separación: el texto queda bajo el nombre de la cuenta.
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
    ) {
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            KindAvatar(kind = row.kind, size = 32.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title ?: row.note.ifBlank { kindLabel(row.kind) }, style = LocalSageType.current.titleCard, maxLines = 1)
            Text(dateLine, style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // [amountCaption] (categoría en Cuentas, cuenta de origen en aportes) bajo el monto, como "Saldo" bajo el saldo.
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                amountMinor = signedAmount,
                currency = row.currency,
                style = LocalSageType.current.amountMd,
                signed = true,
                colorOverride = amountColor,
            )
            if (amountCaption != null) {
                Text(amountCaption, style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Fila de una cuenta archivada: avatar, nombre/tipo, saldo. Restaurar y borrado permanente son
 * gestos de deslizamiento (derecha = restaurar, izquierda = borrado permanente), sin íconos fijos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivedAccountRow(
    account: Account,
    balanceMinor: Long,
    onUnarchive: () -> Unit,
    onHardDelete: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current

    ArchivedSwipeRow(onRestore = onUnarchive, onHardDelete = onHardDelete) {
        FinanceCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                AccountTypeAvatar(account.type)
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.name, style = LocalSageType.current.titleCard)
                    Text(
                        accountTypeLabel(account.type),
                        style = LocalSageType.current.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        fmt.format(balanceMinor, account.currency),
                        // Sage.dc.html: la fila archivada no es negrita, a diferencia de la activa —
                        // Body Regular (sin peso extra) ya es el estilo por defecto para dinero no destacado.
                        style = MaterialTheme.typography.bodyLarge,
                        color = balanceColor(balanceMinor),
                    )
                    Text(
                        if (account.type == "CREDIT") "Deuda" else "Saldo",
                        style = LocalSageType.current.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Detalle de la tarjeta de crédito dentro de su fila: cupo usado en barra (aviso al pasar del 85 %),
 * fechas de corte y pago, y el botón para pagarla cuando hay deuda.
 */
@Composable
private fun CreditActions(card: Card, balanceMinor: Long, unbilledMinor: Long, currency: String, onPayOff: () -> Unit) {
    val fmt = LocalMoneyFormat.current
    val limit = card.creditLimitMinor
    val hasLimit = limit != null && limit > 0
    // Usado = deuda facturada + compromiso sin facturar (cuotas futuras de compras a plazos).
    val used = (-balanceMinor).coerceAtLeast(0L) + unbilledMinor
    val dates = listOfNotNull(card.cutoffDay?.let { "Corte el día $it" }, card.dueDay?.let { "pago el día $it" }).joinToString(" · ")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasLimit) {
            val fraction = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
            val tone = when {
                fraction >= 1f -> ProgressTone.Over
                fraction >= CREDIT_WARNING_FRACTION -> ProgressTone.Warning
                else -> ProgressTone.Normal
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cupo usado", style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${fmt.format(used, currency)} de ${fmt.format(limit, currency)}",
                    style = LocalSageType.current.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressBar(fraction, color = progressColor(tone))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                dates.ifEmpty { "Sin fechas de corte ni pago" },
                style = LocalSageType.current.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (balanceMinor < 0) SageButton("Pagar", onPayOff, kind = SageButtonKind.Secondary, small = true)
        }
    }
}

private const val CREDIT_WARNING_FRACTION = 0.85f

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

    // Mismo chrome que el resto de hojas (AccountFormDialog, BudgetFormDialog, TransactionFormScreen):
    // sin drag handle, SheetHeader con "Cerrar" y padding de 24dp.
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = null, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SheetHeader(title = "Pagar tarjeta", onDismiss = onDismiss)
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
            SageButton(
                "Confirmar pago",
                onClick = { selectedSource?.let { onConfirm(it.id, debtMinor) } },
                enabled = selectedSource != null && debtMinor > 0,
                modifier = Modifier.fillMaxWidth(),
            )
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
    // Monto actualmente asignado a la cuenta: openingBalanceMinor (seed, nunca reescrito) + suma de
    // los movimientos ADJUSTMENT ya registrados (ver AccountsViewModel.updateAccount) — no solo
    // `existing.openingBalanceMinor`, que para cuentas creadas después de este cambio queda en 0.
    currentAmountMinor: Long = existing?.openingBalanceMinor ?: 0L,
    onDismiss: () -> Unit,
    onConfirm: (type: String, name: String, amountMinor: Long, limit: Long?, cutoff: Long?, due: Long?, interest: Double?) -> Unit,
) {
    val isEditing = existing != null
    var type by remember { mutableStateOf(existing?.type ?: types.first()) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amountMinor by remember { mutableStateOf(currentAmountMinor) }
    var limitMinor by remember { mutableStateOf(existingCard?.creditLimitMinor ?: 0L) }
    var cutoff by remember { mutableStateOf(existingCard?.cutoffDay?.toString() ?: "") }
    var due by remember { mutableStateOf(existingCard?.dueDay?.toString() ?: "") }
    var interest by remember { mutableStateOf(existingCard?.interestRate?.toString() ?: "") }

    val isCredit = type == "CREDIT"
    val nameTaken = name.isNotBlank() && name.trim().lowercase() in existingNames
    // Días 1..31 y tasa aceptando coma decimal ("2,5"): antes "2,5" se perdía en silencio.
    val cutoffInvalid = cutoff.isNotEmpty() && cutoff.toIntOrNull() !in 1..31
    val dueInvalid = due.isNotEmpty() && due.toIntOrNull() !in 1..31
    val interestValue = interest.replace(',', '.').toDoubleOrNull()
    val interestInvalid = interest.isNotBlank() && (interestValue == null || interestValue < 0)
    val creditInvalid = isCredit && (cutoffInvalid || dueInvalid || interestInvalid)
    val dirty = type != (existing?.type ?: types.first()) ||
        name != (existing?.name ?: "") ||
        amountMinor != currentAmountMinor ||
        limitMinor != (existingCard?.creditLimitMinor ?: 0L) ||
        cutoff != (existingCard?.cutoffDay?.toString() ?: "") ||
        due != (existingCard?.dueDay?.toString() ?: "") ||
        interest != (existingCard?.interestRate?.toString() ?: "")
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestDismiss = { if (dirty) confirmDiscard = true else onDismiss() }
    if (confirmDiscard) DiscardChangesDialog(onDiscard = onDismiss, onKeepEditing = { confirmDiscard = false })
    val sheetState = rememberGuardedSheetState(dirty) { confirmDiscard = true }
    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(
                title = if (isEditing) "Editar cuenta" else "Nueva cuenta",
                onDismiss = requestDismiss,
            )
            if (isEditing) {
                // El tipo queda bloqueado una vez creada la cuenta (ver KDoc arriba) — se muestra
                // como un PickerField deshabilitado (mismo look que "Subcategoría") en vez de texto
                // plano, para que el usuario siga viendo qué tipo es sin poder cambiarlo.
                PickerField(
                    label = "Tipo",
                    options = types,
                    selected = type,
                    optionLabel = { accountTypeLabel(it) },
                    onSelect = {},
                    enabled = false,
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { t ->
                        SelectorPill(
                            text = accountTypeLabel(t),
                            selected = type == t,
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                            modifier = Modifier.weight(1f),
                        ) { type = t }
                    }
                }
            }
            AccountNameField(name, { name = it }, nameTaken)
            if (isCredit) {
                MoneyField(limitMinor, { limitMinor = it }, currencyCode = baseCurrency, label = "Cupo")
                NumberField(cutoff, "Fecha de corte", "Día del mes (1–31)", maxLen = 2, error = DAY_ERROR.takeIf { cutoffInvalid }) { cutoff = it }
                NumberField(due, "Fecha de pago", "Día del mes (1–31)", maxLen = 2, error = DAY_ERROR.takeIf { dueInvalid }) { due = it }
                DecimalField(
                    interest,
                    "Tasa de interés mensual (%)",
                    "0,0",
                    error = "Escribe un número, por ejemplo 2,5".takeIf { interestInvalid },
                ) { interest = it }
            } else {
                MoneyField(amountMinor, { amountMinor = it }, currencyCode = baseCurrency, label = "Monto inicial")
            }
            SageButton(
                text = if (isEditing) "Guardar cambios" else "Crear cuenta",
                onClick = {
                    onConfirm(
                        type,
                        name,
                        if (isCredit) 0 else amountMinor,
                        if (isCredit) limitMinor else null,
                        if (isCredit) cutoff.toLongOrNull() else null,
                        if (isCredit) due.toLongOrNull() else null,
                        if (isCredit) interestValue else null,
                    )
                },
                enabled = name.isNotBlank() && !nameTaken && !creditInvalid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AccountNameField(value: String, onChange: (String) -> Unit, nameTaken: Boolean) {
    SageTextField(
        value = value,
        onValueChange = onChange,
        label = "Nombre",
        placeholder = "Ej.: Cuenta nómina",
        error = "Ya existe una cuenta con ese nombre.".takeIf { nameTaken },
    )
}

@Composable
private fun DecimalField(value: String, label: String, placeholder: String, error: String? = null, onChange: (String) -> Unit) {
    SageTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        placeholder = placeholder,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun NumberField(value: String, label: String, placeholder: String, maxLen: Int, error: String? = null, onChange: (String) -> Unit) {
    SageTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(maxLen)) },
        label = label,
        placeholder = placeholder,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private const val DAY_ERROR = "Debe ser un día entre 1 y 31"
