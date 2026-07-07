package com.finanzen.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.AutoSizeText
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.TopExpensesList
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.PillShape
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.SettingsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = koinViewModel(),
    settingsVm: SettingsViewModel = koinInject(),
    onOpenAnalysis: () -> Unit = {},
) {
    val data by vm.data.collectAsState()
    val hideAmounts by settingsVm.hideAmounts.collectAsState()
    val spacing = LocalSpacing.current
    val dateLocale = LocalDateLocale.current
    val monthLabel = remember(dateLocale) {
        formatMesAnio(Clock.System.todayIn(TimeZone.currentSystemDefault()), dateLocale)
    }

    Column(Modifier.fillMaxSize()) {
        MainTabHeader(title = "Resumen", subtitle = monthLabel)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // bottom extra para que el FAB central no tape la última tarjeta.
            contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                BalanceHeroCard(
                    totalBalance = data.totalBalanceMinor,
                    monthIncome = data.monthIncomeMinor,
                    monthExpense = data.monthExpenseMinor,
                    currency = data.currency,
                    hidden = hideAmounts,
                    onToggleHidden = { settingsVm.setHideAmounts(!hideAmounts) },
                )
            }

            item { AnalysisShortcutCard(onClick = onOpenAnalysis) }

            item { SectionHeader("Top gastos") }
            if (data.topExpenses.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.PieChart,
                        title = "Sin gastos registrados",
                        subtitle = "Añade movimientos desde la pestaña Movimientos.",
                        modifier = Modifier.padding(spacing.xl),
                    )
                }
            } else {
                item { TopExpensesList(data.topExpenses, data.currency) }
            }
        }
    }
}

private const val MASK = "******"

@Composable
private fun BalanceHeroCard(
    totalBalance: Long,
    monthIncome: Long,
    monthExpense: Long,
    currency: String,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant
    // Tarjeta neutra (no primaryContainer): los colores semánticos verde/rojo contrastan como es debido
    // en cualquier acento/dinámico, sin lavar el texto sobre el contenedor de color.
    val finance = LocalFinanceColors.current
    val incomeGreen = finance.income
    val expenseRed = finance.expense
    val haptic = LocalHapticFeedback.current

    // El balance general es histórico (saldos iniciales + ingresos − gastos de todo el
    // histórico), con el mismo formato global (símbolo + separadores) que el resto de montos de
    // la app. AutoSizeText se encarga de que la cifra grande siempre quepa en una línea.
    val fmt = LocalMoneyFormat.current
    val balanceColor = if (totalBalance >= 0) onSurface else expenseRed
    val animatedBalance by animateFloatAsState(targetValue = totalBalance.toFloat(), animationSpec = tween(400))
    val balanceText = if (hidden) MASK else fmt.format(animatedBalance.toLong(), currency)

    val totalFlow = monthIncome + monthExpense
    val incomeFraction by animateFloatAsState(
        targetValue = if (totalFlow > 0) (monthIncome.toFloat() / totalFlow.toFloat()).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(400),
    )

    FinanceCard(color = MaterialTheme.colorScheme.surfaceContainerHigh, contentPadding = PaddingValues(spacing.xl)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
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

            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                // Barra de proporción ingreso vs gasto del mes.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (totalFlow > 0) {
                        Box(Modifier.fillMaxHeight().weight(incomeFraction.coerceAtLeast(0.001f)).background(incomeGreen))
                        Box(Modifier.fillMaxHeight().weight((1f - incomeFraction).coerceAtLeast(0.001f)).background(expenseRed))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FlowItem(
                        Icons.Outlined.ArrowUpward,
                        "Ingresos",
                        if (hidden) MASK else fmt.format(monthIncome, currency),
                        incomeGreen,
                        onSurfaceVar,
                        Alignment.Start,
                    )
                    FlowItem(
                        Icons.Outlined.ArrowDownward,
                        "Gastos",
                        if (hidden) MASK else fmt.format(monthExpense, currency),
                        expenseRed,
                        onSurfaceVar,
                        Alignment.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowItem(
    icon: ImageVector,
    label: String,
    valueText: String,
    accent: Color,
    labelColor: Color,
    align: Alignment.Horizontal,
) {
    Column(horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
        }
        Text(valueText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

@Composable
private fun AnalysisShortcutCard(onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    FinanceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ver detalle mensual", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(spacing.lg),
            )
        }
    }
}
