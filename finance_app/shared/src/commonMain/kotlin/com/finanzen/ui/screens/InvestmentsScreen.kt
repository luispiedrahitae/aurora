package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.finanzen.db.Investment
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IconAvatar
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SheetHeader
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwipeToDeleteRow
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
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
    val accounts by vm.accounts.collectAsState()
    val contributionsByInvestment by vm.contributionsByInvestment.collectAsState()
    var withdrawingInvestment by remember { mutableStateOf<Investment?>(null) }
    var pendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val accountNameById = remember(accounts) { accounts.associate { it.id to it.name } }

    fun requestDelete(inv: Investment) {
        pendingDeleteIds = pendingDeleteIds + inv.id
        // vm.viewModelScope, no rememberCoroutineScope(): el deshacer/borrado debe sobrevivir a la
        // navegación fuera de esta pantalla, no cancelarse con la composición.
        vm.viewModelScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Inversión eliminada",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                pendingDeleteIds = pendingDeleteIds - inv.id
            } else {
                vm.deleteInvestment(inv.id)
                pendingDeleteIds = pendingDeleteIds - inv.id
            }
        }
    }

    withdrawingInvestment?.let { inv ->
        val contributed = contributionsByInvestment[inv.id].orEmpty().sumOf { it.amountMinor }
        WithdrawDialog(
            investmentName = inv.name,
            contributedMinor = contributed,
            currencyCode = inv.currency,
            onDismiss = { withdrawingInvestment = null },
            onConfirm = { withdrawnAmountMinor ->
                vm.closeInvestment(inv.id, withdrawnAmountMinor)
                withdrawingInvestment = null
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SubScreenHeader(title = "Inversiones", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    items(open.filter { it.id !in pendingDeleteIds }, key = { it.id }) { inv ->
                        SwipeToDeleteRow(onSwipeToDelete = {
                            requestDelete(inv)
                            true
                        }) {
                            InvestmentItem(
                                investment = inv,
                                accountName = accountNameById[inv.accountId] ?: "",
                                onWithdraw = { withdrawingInvestment = inv },
                            )
                        }
                    }
                }
                if (closed.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader("Cerradas")
                            val first = closed.first()
                            ClosedInvestmentItem(investment = first, accountName = accountNameById[first.accountId] ?: "")
                        }
                    }
                    items(closed.drop(1), key = { it.id }) { inv ->
                        ClosedInvestmentItem(investment = inv, accountName = accountNameById[inv.accountId] ?: "")
                    }
                }
            }
        }
    }
}

/** Retiro/cierre: el usuario ingresa el monto retirado y ve en vivo el rendimiento resultante. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawDialog(
    investmentName: String,
    contributedMinor: Long,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (withdrawnAmountMinor: Long) -> Unit,
) {
    var withdrawnAmountMinor by remember { mutableStateOf(0L) }
    val finance = LocalFinanceColors.current
    val yieldMinor = withdrawnAmountMinor - contributedMinor
    val yieldPct = if (contributedMinor != 0L) yieldMinor * 100.0 / contributedMinor else null
    val yieldColor = if (yieldMinor >= 0) finance.income else finance.expense

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(title = "Retirar \"$investmentName\"", onDismiss = onDismiss)
            MoneyField(
                amountMinor = withdrawnAmountMinor,
                onAmountChange = { withdrawnAmountMinor = it },
                currencyCode = currencyCode,
                label = "Monto retirado",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rendimiento", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs), verticalAlignment = Alignment.CenterVertically) {
                    if (yieldPct != null) {
                        val sign = if (yieldPct >= 0) "+" else ""
                        Text(
                            "($sign${yieldPct.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = yieldColor,
                        )
                    }
                    MoneyText(
                        amountMinor = yieldMinor,
                        currency = currencyCode,
                        style = MaterialTheme.typography.bodyMedium,
                        signed = true,
                        colorOverride = yieldColor,
                    )
                }
            }
            Button(
                onClick = { onConfirm(withdrawnAmountMinor) },
                enabled = withdrawnAmountMinor > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Confirmar retiro", fontFamily = displayFontFamily()) }
        }
    }
}

/** Tarjeta de inversión abierta: icono · nombre + subtítulo (cuenta, periodicidad) · monto por aporte
 * · retirar. Se elimina deslizando. */
@Composable
private fun InvestmentItem(
    investment: Investment,
    accountName: String,
    onWithdraw: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val dateLocale = LocalDateLocale.current
    val subtitle = if (investment.periodic == 1L) {
        val next = investment.nextContributionDate?.let { " · próximo ${formatDiaMes(it, dateLocale)}" } ?: ""
        "$accountName$next"
    } else {
        "$accountName · puntual"
    }

    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconAvatar(Icons.Outlined.TrendingUp)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(investment.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MoneyText(amountMinor = investment.amountMinor, currency = investment.currency, style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onWithdraw) { Text("Retirar / Cerrar") }
        }
    }
}

/** Tarjeta de inversión cerrada: aportado / retirado / rendimiento (verde si ganancia, rosa si pérdida). */
@Composable
private fun ClosedInvestmentItem(investment: Investment, accountName: String) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    val withdrawn = investment.withdrawnAmountMinor ?: 0L
    val yieldMinor = investment.yieldMinor ?: 0L
    val contributed = withdrawn - yieldMinor
    val yieldColor = if (yieldMinor >= 0) finance.income else finance.expense
    val yieldPct = if (contributed != 0L) yieldMinor * 100.0 / contributed else null

    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Column {
                Text(investment.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(accountName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ClosedInvestmentRow("Aportado", contributed, investment.currency)
            ClosedInvestmentRow("Retirado", withdrawn, investment.currency)
            ClosedInvestmentRow(
                "Rendimiento",
                yieldMinor,
                investment.currency,
                signed = true,
                colorOverride = yieldColor,
                pct = yieldPct,
            )
        }
    }
}

@Composable
private fun ClosedInvestmentRow(
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
