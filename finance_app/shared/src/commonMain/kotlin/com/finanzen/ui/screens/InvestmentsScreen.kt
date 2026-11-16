package com.finanzen.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.db.Investment
import com.finanzen.db.TransactionRow
import com.finanzen.ui.components.AddFab
import com.finanzen.ui.components.ArchivedSwipeRow
import com.finanzen.ui.components.BottomSnackbarHost
import com.finanzen.ui.components.ConfirmDialog
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.ExpandableSection
import com.finanzen.ui.components.FadeSection
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IconAvatar
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SelectorPill
import com.finanzen.ui.components.SheetHeader
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwipeToArchive
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.viewmodel.InvestmentsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onBack: () -> Unit,
    vm: InvestmentsViewModel = koinViewModel(),
) {
    val open by vm.openInvestments.collectAsState()
    val closed by vm.closedInvestments.collectAsState()
    val archived by vm.archivedInvestments.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val contributionsByInvestment by vm.contributionsByInvestment.collectAsState()
    var showFundSheet by remember { mutableStateOf(false) }
    var pendingMovementDelete by remember { mutableStateOf<TransactionRow?>(null) }
    var pendingHardDelete by remember { mutableStateOf<Investment?>(null) }
    var archivedExpanded by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    // Cerrar una inversión mueve el fondo a "Cerradas" y fija su rendimiento: se confirma antes.
    var pendingClose by remember { mutableStateOf<Pair<Investment, Long>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val spacing = LocalSpacing.current
    val accountNameById = remember(accounts) { accounts.associate { it.id to it.name } }

    // Mismo patrón que AccountsTabScreen.archiveWithUndo.
    fun archiveWithUndo(inv: Investment) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.archive(inv.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                "\"${inv.name}\" archivada",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.unarchive(inv.id)
        }
    }

    fun reopenWithUndo(inv: Investment) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val closedDate = inv.closedDate
        vm.reopen(inv.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                "\"${inv.name}\" reabierta",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && closedDate != null) vm.closeInvestment(inv.id, 0, closedDate)
        }
    }

    pendingHardDelete?.let { inv ->
        ConfirmDialog(
            title = "¿Eliminar \"${inv.name}\"?",
            body = "Se eliminarán también sus aportes y retiros de Movimientos. Esta acción no se puede deshacer.",
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.deletePermanently(inv.id)
                pendingHardDelete = null
            },
            onDismiss = { pendingHardDelete = null },
        )
    }

    if (showFundSheet && open.isNotEmpty()) {
        FundActionSheet(
            investments = open,
            accounts = accounts,
            accountNameById = accountNameById,
            contributionsByInvestment = contributionsByInvestment,
            onDismiss = { showFundSheet = false },
            onConfirm = { id, action, amountMinor, accountId ->
                when (action) {
                    FundAction.Contribute -> vm.contribute(id, amountMinor, accountId)

                    FundAction.Withdraw -> vm.withdraw(id, amountMinor)

                    FundAction.Close -> open.firstOrNull { it.id == id }?.let { inv ->
                        pendingClose = inv to amountMinor
                    }
                }
                showFundSheet = false
            },
        )
    }

    pendingClose?.let { (inv, amountMinor) ->
        val amountText = LocalMoneyFormat.current.format(amountMinor, inv.currency)
        ConfirmDialog(
            title = "¿Cerrar \"${inv.name}\"?",
            body = "Se registrará el retiro final de $amountText y la inversión pasará a Cerradas con su rendimiento. " +
                "Puedes reabrirla después deslizándola a la derecha.",
            confirmLabel = "Cerrar inversión",
            destructive = false,
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.closeInvestment(inv.id, amountMinor)
                pendingClose = null
            },
            onDismiss = { pendingClose = null },
        )
    }

    pendingMovementDelete?.let { tx ->
        val label = if (tx.kind == "INCOME") "retiro" else "aporte"
        ConfirmDialog(
            title = "Eliminar $label",
            body = "Se eliminará este $label del historial y de Movimientos.",
            onConfirm = {
                vm.deleteMovement(tx.id)
                pendingMovementDelete = null
            },
            onDismiss = { pendingMovementDelete = null },
        )
    }

    editingId?.let { id ->
        TransactionFormScreen(transactionId = null, investmentId = id, onDismiss = { editingId = null })
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // Mismo botón que "+ Movimiento": Aportar/Retirar/Cerrar se eligen en la hoja.
        floatingActionButton = {
            if (open.isNotEmpty()) AddFab(label = "Aportar, retirar o cerrar", showLabel = false, onClick = { showFundSheet = true })
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SubScreenHeader(title = "Inversiones", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Abajo 96dp para que el FAB no tape la última tarjeta (igual que Movimientos).
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SectionHeader("Abiertas") }
                if (open.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = LucideIcons.TrendingUp,
                                title = "Sin inversiones abiertas",
                                subtitle = "Créala desde Nuevo movimiento; los aportes se registran en Movimientos.",
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    }
                } else {
                    items(open, key = { it.id }) { inv ->
                        SwipeToArchive(onArchive = { archiveWithUndo(inv) }) {
                            InvestmentItem(
                                investment = inv,
                                accountName = accountNameById[inv.accountId] ?: "",
                                accountNameById = accountNameById,
                                movements = contributionsByInvestment[inv.id].orEmpty(),
                                onDeleteMovement = { pendingMovementDelete = it },
                                onEdit = { editingId = inv.id },
                            )
                        }
                    }
                }
                if (closed.isNotEmpty()) {
                    item { SectionHeader("Cerradas") }
                    items(closed, key = { it.id }) { inv ->
                        SwipeToArchive(onArchive = { archiveWithUndo(inv) }, onSwipeRight = { reopenWithUndo(inv) }, swipeRightLabel = "Reabrir") {
                            InvestmentItem(
                                investment = inv,
                                accountName = accountNameById[inv.accountId] ?: "",
                                accountNameById = accountNameById,
                                movements = contributionsByInvestment[inv.id].orEmpty(),
                                onDeleteMovement = null,
                                onEdit = null,
                            )
                        }
                    }
                }
                if (archived.isNotEmpty()) {
                    // Plegable, igual que "Archivadas" en Cuentas.
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClickLabel = if (archivedExpanded) "Contraer" else "Expandir") {
                                    archivedExpanded = !archivedExpanded
                                }
                                .padding(vertical = spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            Icon(
                                if (archivedExpanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                                contentDescription = if (archivedExpanded) "Contraer" else "Expandir",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            SectionHeader("Archivadas (${archived.size})")
                        }
                    }
                    if (archivedExpanded) {
                        items(archived, key = { it.id }) { inv ->
                            ArchivedSwipeRow(onRestore = { vm.unarchive(inv.id) }, onHardDelete = { pendingHardDelete = inv }) {
                                ArchivedInvestmentItem(
                                    investment = inv,
                                    accountName = accountNameById[inv.accountId] ?: "",
                                    movements = contributionsByInvestment[inv.id].orEmpty(),
                                )
                            }
                        }
                    }
                }
            }
        }
        BottomSnackbarHost(snackbarHostState, aboveFab = true)
    }
}

/** Fila compacta de una inversión archivada: encabezado común + capital (si estaba abierta) o
 * rendimiento con color (si estaba cerrada). Restaurar/eliminar son gestos de [ArchivedSwipeRow]. */
@Composable
private fun ArchivedInvestmentItem(investment: Investment, accountName: String, movements: List<TransactionRow>) {
    val finance = LocalFinanceColors.current
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        InvestmentHeader(investment.name, accountName) {
            if (investment.closedDate != null) {
                val yieldMinor = investment.yieldMinor ?: 0L
                MoneyText(
                    amountMinor = yieldMinor,
                    currency = investment.currency,
                    style = MaterialTheme.typography.bodyLarge,
                    signed = true,
                    colorOverride = if (yieldMinor >= 0) finance.income else finance.expense,
                )
            } else {
                val totals = InvestmentsViewModel.fundTotals(movements)
                MoneyText(amountMinor = totals.contributedMinor - totals.withdrawnMinor, currency = investment.currency, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private enum class FundAction { Contribute, Withdraw, Close }

/**
 * Sheet del FAB "Movimiento": elige inversión abierta, acción y monto. Aportar y Retirar piden un
 * monto > 0 y no cierran nada. Cerrar pide el retiro final (0 si ya se retiró todo) y muestra en vivo
 * el rendimiento resultante: (retirado acumulado + final) − aportado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FundActionSheet(
    investments: List<Investment>,
    accounts: List<Account>,
    accountNameById: Map<Long, String>,
    contributionsByInvestment: Map<Long, List<TransactionRow>>,
    onDismiss: () -> Unit,
    onConfirm: (investmentId: Long, action: FundAction, amountMinor: Long, accountId: Long?) -> Unit,
) {
    var selectedId by remember { mutableStateOf(investments.first().id) }
    var action by remember { mutableStateOf(FundAction.Contribute) }
    var amountMinor by remember { mutableStateOf(0L) }
    // Si la elegida se cerró/archivó mientras la hoja estaba abierta, cae a la primera.
    val investment = investments.firstOrNull { it.id == selectedId } ?: investments.first()
    // Cuenta de origen del aporte: solo de la moneda del fondo; arranca (y se reinicia al cambiar de fondo) en la del fondo.
    val sourceAccounts = remember(accounts, investment.currency) { accounts.filter { it.currency == investment.currency } }
    var sourceAccountId by remember(investment.id) { mutableStateOf(investment.accountId) }
    val sourceAccount = sourceAccounts.firstOrNull { it.id == sourceAccountId }
    val totals = InvestmentsViewModel.fundTotals(contributionsByInvestment[investment.id].orEmpty())
    val currencyCode = investment.currency
    val finance = LocalFinanceColors.current
    val (label, confirmLabel) = when (action) {
        FundAction.Contribute -> "Monto del aporte" to "Confirmar aporte"
        FundAction.Withdraw -> "Monto retirado" to "Confirmar retiro"
        FundAction.Close -> "Retiro final" to "Cerrar inversión"
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(title = "Mover fondos", onDismiss = onDismiss)
            // Mismas pastillas que Gasto/Ingreso/Transferencia en TransactionFormScreen.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectorPill("Aportar", action == FundAction.Contribute, Modifier.weight(1f)) { action = FundAction.Contribute }
                SelectorPill("Retirar", action == FundAction.Withdraw, Modifier.weight(1f)) { action = FundAction.Withdraw }
                SelectorPill("Cerrar", action == FundAction.Close, Modifier.weight(1f)) { action = FundAction.Close }
            }
            PickerField(
                label = "Inversión",
                options = investments,
                selected = investment,
                optionLabel = { it.name },
                trailingLabel = { accountNameById[it.accountId] },
                onSelect = { selectedId = it.id },
                modifier = Modifier.fillMaxWidth(),
            )
            if (action == FundAction.Contribute && sourceAccount != null) {
                PickerField(
                    label = "Cuenta",
                    options = sourceAccounts,
                    selected = sourceAccount,
                    optionLabel = { it.name },
                    onSelect = { sourceAccountId = it.id },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MoneyField(
                amountMinor = amountMinor,
                onAmountChange = { amountMinor = it },
                currencyCode = currencyCode,
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
            if (action == FundAction.Close) {
                Text("Pon 0 si ya retiraste todo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AmountRow("Aportado", totals.contributedMinor, currencyCode)
            AmountRow("Retirado", totals.withdrawnMinor, currencyCode)
            if (action == FundAction.Close) {
                val withdrawn = totals.withdrawnMinor + amountMinor
                val yieldMinor = InvestmentsViewModel.yieldMinor(withdrawn, totals.contributedMinor)
                AmountRow(
                    "Rendimiento",
                    yieldMinor,
                    currencyCode,
                    signed = true,
                    colorOverride = if (yieldMinor >= 0) finance.income else finance.expense,
                    pct = if (totals.contributedMinor != 0L) yieldMinor * 100.0 / totals.contributedMinor else null,
                )
            }
            Button(
                onClick = { onConfirm(investment.id, action, amountMinor, sourceAccount?.id ?: investment.accountId) },
                enabled = action == FundAction.Close || amountMinor > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(confirmLabel, fontFamily = displayFontFamily()) }
        }
    }
}

/** Tarjeta de inversión, abierta o cerrada: icono · nombre + subtítulo (cuenta, periodicidad) ·
 * monto inicial (abierta) o rendimiento (cerrada) con su etiqueta debajo — al expandir baja al pie —
 * e historial desplegable con Inicial/Aportes/Retirado/Rendimiento (+ %) al pie, como la cuenta.
 * Tocar abre/cierra, deslizar archiva (o reabre). Solo la abierta se edita (mantener pulsado) y deja
 * borrar movimientos: en la cerrada desincronizaría el rendimiento guardado al cerrarla. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InvestmentItem(
    investment: Investment,
    accountName: String,
    accountNameById: Map<Long, String>,
    movements: List<TransactionRow>,
    onDeleteMovement: ((TransactionRow) -> Unit)?,
    onEdit: (() -> Unit)?,
) {
    val spacing = LocalSpacing.current
    val haptic = LocalHapticFeedback.current
    val dateLocale = LocalDateLocale.current
    val finance = LocalFinanceColors.current
    val totals = InvestmentsViewModel.fundTotals(movements)
    var showHistory by remember { mutableStateOf(false) }
    val next = investment.nextContributionDate?.takeIf { investment.periodic == 1L }
    val subtitle = accountName + (next?.let { " · próximo ${formatDiaMes(it, dateLocale)}" } ?: "")
    // Rendimiento solo con el fondo cerrado: abierto y sin retiros daría un −100% engañoso.
    val closed = investment.closedDate != null
    val yieldMinor = investment.yieldMinor ?: 0L
    val contributed = (investment.withdrawnAmountMinor ?: 0L) - yieldMinor
    val yieldPct = if (closed && contributed != 0L) yieldMinor * 100.0 / contributed else null
    val yieldColor = if (yieldMinor >= 0) finance.income else finance.expense

    // Como el día en Movimientos (DayHeader): tocar la cabecera abre/cierra el historial, mantener
    // pulsado edita. Aportar/Retirar/Cerrar viven en el FAB de la pantalla.
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClickLabel = if (showHistory) "Contraer" else "Expandir",
                        onLongClickLabel = onEdit?.let { "Editar" },
                        onLongClick = onEdit?.let {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                it()
                            }
                        },
                        onClick = { showHistory = !showHistory },
                    )
                    .padding(16.dp),
            ) {
                InvestmentHeader(investment.name, subtitle) {
                    // Como el Saldo de la cuenta: monto + etiqueta contraída; expandida baja al pie (con el %).
                    FadeSection(!showHistory) {
                        Column(horizontalAlignment = Alignment.End) {
                            MoneyText(
                                amountMinor = if (closed) yieldMinor else totals.initialMinor,
                                currency = investment.currency,
                                style = MaterialTheme.typography.bodyLarge,
                                signed = closed,
                                colorOverride = if (closed) yieldColor else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (closed) "Rendimiento" else "Inicial",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        if (showHistory) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            ExpandableSection(showHistory) {
                // Mismo formato que los movimientos de una cuenta expandida (AccountMovementRow), sin
                // divisor bajo la cabecera. Tocar una fila pide confirmar su borrado; no hay swipe porque la card ya lo usa.
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = spacing.md, top = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    if (movements.isEmpty()) {
                        Text("Sin aportes todavía", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    movements.forEach { tx ->
                        // Papelera visible en vez de "tocar la fila borra": en el resto de la app tocar
                        // una fila abre o edita, nunca elimina.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountMovementRow(
                                tx,
                                title = if (tx.kind == "INCOME") "Retiro" else "Aporte",
                                // Aportes: cuenta de origen bajo el monto (puede no ser la del fondo). Retiros: su
                                // categoría, siempre la de sistema "Inversiones" (withdraw() usa inv.categoryId).
                                amountCaption = if (tx.kind == "EXPENSE") accountNameById[tx.accountId] else "Inversiones",
                                modifier = Modifier.weight(1f),
                            )
                            if (onDeleteMovement != null) {
                                IconButton(onClick = { onDeleteMovement(tx) }) {
                                    Icon(
                                        LucideIcons.Trash,
                                        contentDescription = if (tx.kind == "INCOME") "Eliminar retiro" else "Eliminar aporte",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    // Como el pie de la cuenta: divisor con sangría, un respiro y los totales pegados.
                    Column {
                        val indent = movementTextIndent()
                        val ink = MaterialTheme.colorScheme.onSurface
                        HorizontalDivider(indent)
                        Spacer(Modifier.height(spacing.sm))
                        // Con la columna de papelera, los totales se corren igual para alinear con los montos.
                        val trashColumn = if (onDeleteMovement != null) Modifier.padding(end = 48.dp) else Modifier
                        Column(trashColumn) {
                            MovementSummaryRow("Inicial", totals.initialMinor, investment.currency, colorOverride = ink, signed = false, modifier = indent)
                            MovementSummaryRow("Aportes", totals.contributedMinor - totals.initialMinor, investment.currency, colorOverride = ink, signed = false, modifier = indent)
                            MovementSummaryRow("Retirado", totals.withdrawnMinor, investment.currency, colorOverride = ink, signed = false, modifier = indent)
                            if (closed) {
                                MovementSummaryRow("Rendimiento", yieldMinor, investment.currency, colorOverride = yieldColor, modifier = indent, pct = yieldPct)
                            }
                        }
                        // Editar visible (además de mantener pulsado, que nadie descubre solo).
                        if (onEdit != null) {
                            Row(indent) { CardAction("Editar", trailing = LucideIcons.Pencil, onClick = onEdit) }
                        }
                    }
                }
            }
        }
    }
}

/** Encabezado común de las cards (abierta y cerrada): mismo avatar y columna de nombre en todas. */
@Composable
private fun InvestmentHeader(name: String, subtitle: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
        IconAvatar(Icons.AutoMirrored.Outlined.TrendingUp)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** Acción de la card como texto clicable con target táctil de 48dp, sin padding horizontal extra.
 * También la usa la tarjeta de cuenta (AccountsTabScreen). */
@Composable
internal fun CardAction(label: String, trailing: Painter? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amountMinor: Long,
    currency: String,
    signed: Boolean = false,
    colorOverride: Color? = null,
    pct: Double? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs), verticalAlignment = Alignment.CenterVertically) {
            if (pct != null) {
                val sign = if (pct >= 0) "+" else ""
                Text(
                    "($sign${pct.toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(amountMinor = amountMinor, currency = currency, style = MaterialTheme.typography.bodyMedium, signed = signed, colorOverride = colorOverride)
        }
    }
}
