package com.finanzen.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.AccountBalanceBars
import com.finanzen.ui.components.AutoSizeText
import com.finanzen.ui.components.CategoryDonutChart
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.SavingsRateGauge
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = koinViewModel(),
    settingsVm: SettingsViewModel = koinInject(),
) {
    val data by vm.data.collectAsState()
    val hideAmounts by settingsVm.hideAmounts.collectAsState()
    val savingsGoalPct by settingsVm.savingsGoalPct.collectAsState()
    val spacing = LocalSpacing.current

    Column(Modifier.fillMaxSize()) {
        MainTabHeader(title = "Resumen")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // bottom extra para que el FAB central no tape la última tarjeta.
            contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                BalanceHeroCard(
                    totalBalance = data.totalBalanceMinor,
                    currency = data.currency,
                    hidden = hideAmounts,
                    onToggleHidden = { settingsVm.setHideAmounts(!hideAmounts) },
                )
            }

            item {
                KpiRow(
                    income = data.monthIncomeMinor,
                    expense = data.monthExpenseMinor,
                    currency = data.currency,
                    hidden = hideAmounts,
                )
            }

            item {
                SavingsRateGauge(
                    savingsRate = data.savingsRate,
                    goalPct = savingsGoalPct,
                    onGoalChange = settingsVm::setSavingsGoalPct,
                )
            }

            item { SectionHeader("Saldos por cuenta") }
            item { AccountBalanceBars(data.accounts, data.currency) }

            item { SectionHeader("Gastos por categoría") }
            if (data.donut.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.PieChart,
                        title = "Sin gastos este mes",
                        subtitle = "Añade movimientos desde la pestaña Movimientos.",
                        modifier = Modifier.padding(spacing.xl),
                    )
                }
            } else {
                item { CategoryDonutChart(data.donut, data.currency) }
            }
        }
    }
}

private const val MASK = "******"

@Composable
private fun BalanceHeroCard(
    totalBalance: Long,
    currency: String,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant
    // Tarjeta neutra (no primaryContainer): los colores semánticos verde/rojo contrastan como es debido
    // en cualquier acento/dinámico, sin lavar el texto sobre el contenedor de color.
    val expenseRed = LocalFinanceColors.current.expense

    // El balance general es histórico (saldos iniciales + ingresos − gastos de todo el
    // histórico), con el mismo formato global (símbolo + separadores) que el resto de montos de
    // la app. AutoSizeText se encarga de que la cifra grande siempre quepa en una línea.
    val fmt = LocalMoneyFormat.current
    val haptic = LocalHapticFeedback.current
    val balanceColor = if (totalBalance >= 0) onSurface else expenseRed
    val animatedBalance by animateFloatAsState(targetValue = totalBalance.toFloat(), animationSpec = tween(400))
    val balanceText = if (hidden) MASK else fmt.format(animatedBalance.toLong(), currency)

    FinanceCard(color = MaterialTheme.colorScheme.surfaceContainerHigh, contentPadding = PaddingValues(spacing.xl)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Balance general", style = MaterialTheme.typography.labelLarge, color = onSurfaceVar)
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleHidden()
                }) {
                    Icon(
                        if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Mostrar montos" else "Ocultar montos",
                        tint = onSurfaceVar,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            AutoSizeText(
                text = balanceText,
                modifier = Modifier.fillMaxWidth(),
                maxFontSize = MaterialTheme.typography.displayLarge.fontSize,
                color = balanceColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}

/**
 * Fila de KPIs del mes en curso: ingresos y gastos. Sustituye la barra de proporción que antes
 * vivía en el hero. Cada tarjeta respeta el modo "ocultar montos" con la misma máscara.
 */
@Composable
private fun KpiRow(
    income: Long,
    expense: Long,
    currency: String,
    hidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        KpiCard("Ingresos", income, currency, finance.income, hidden, Modifier.weight(1f))
        KpiCard("Gastos", expense, currency, finance.expense, hidden, Modifier.weight(1f))
    }
}

@Composable
private fun KpiCard(
    label: String,
    amountMinor: Long,
    currency: String,
    accent: Color,
    hidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    FinanceCard(modifier = modifier, contentPadding = PaddingValues(spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (hidden) MASK else fmt.format(amountMinor, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
            )
        }
    }
}
