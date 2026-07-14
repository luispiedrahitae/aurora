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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.CategoryDonutChart
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MonthlyBarChart
import com.finanzen.ui.components.NetWorthAreaChart
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.YearSelector
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = koinViewModel(),
    settingsVm: SettingsViewModel = koinInject(),
) {
    val data by vm.data.collectAsState()
    val year by vm.year.collectAsState()
    val hideAmounts by settingsVm.hideAmounts.collectAsState()
    val spacing = LocalSpacing.current
    val fmt = LocalMoneyFormat.current
    val finance = LocalFinanceColors.current

    // Bento: el hero de saldo (vidrio) ocupa las 2 columnas; los pares de KPI son medium tiles lado
    // a lado; gráficas/secciones vuelven a ocupar el ancho completo — capas de tamaño, no una lista
    // plana (DESIGN.md, "Bento Tiles"). Todo salvo el hero y "Saldos por cuenta" está acotado al año
    // seleccionado (selector arriba de la grilla).
    val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

    Column(Modifier.fillMaxSize()) {
        MainTabHeader(title = "Resumen")
        YearSelector(
            label = year.toString(),
            onPrev = { vm.setYear(year - 1) },
            onNext = { vm.setYear(year + 1) },
            modifier = Modifier.padding(horizontal = spacing.lg),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            // bottom extra para que el FAB central no tape la última tarjeta.
            contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(span = fullSpan) {
                BalanceHeroCard(
                    totalBalance = data.totalBalanceMinor,
                    currency = data.currency,
                    hidden = hideAmounts,
                    onToggleHidden = { settingsVm.setHideAmounts(!hideAmounts) },
                )
            }

            item {
                KpiCard(
                    "Ingresos",
                    if (hideAmounts) MASK else fmt.format(data.yearIncomeMinor, data.currency),
                    finance.income,
                )
            }
            item {
                KpiCard(
                    "Gastos",
                    if (hideAmounts) MASK else fmt.format(data.yearExpenseMinor, data.currency),
                    finance.expense,
                )
            }

            item {
                KpiCard(
                    "Tasa de ahorro anual",
                    "${(data.savingsRate * 100).roundToInt()}%",
                    MaterialTheme.colorScheme.onSurface,
                )
            }
            item {
                KpiCard(
                    "Suscripciones",
                    if (hideAmounts) MASK else fmt.format(data.subscriptionSpendMinor, data.currency),
                    MaterialTheme.colorScheme.onSurface,
                )
            }

            item(span = fullSpan) { SectionHeader("Saldos por cuenta") }
            item(span = fullSpan) { AccountBalanceBars(data.accounts, data.currency) }

            item(span = fullSpan) { SectionHeader("Patrimonio neto") }
            item(span = fullSpan) { NetWorthAreaChart(data.netWorthByMonth, data.currency) }

            item(span = fullSpan) { SectionHeader("Ingresos por mes") }
            item(span = fullSpan) { MonthlyBarChart(data.incomeByMonth, data.currency, finance.income, "Ingresos") }

            item(span = fullSpan) { SectionHeader("Gastos por mes") }
            item(span = fullSpan) { MonthlyBarChart(data.expenseByMonth, data.currency, finance.expense, "Gastos") }

            item(span = fullSpan) { SectionHeader("Gastos por categoría") }
            if (data.donut.isEmpty()) {
                item(span = fullSpan) {
                    EmptyState(
                        icon = Icons.Outlined.PieChart,
                        title = "Sin gastos este año",
                        subtitle = "Añade movimientos desde la pestaña Movimientos.",
                        modifier = Modifier.padding(spacing.xl),
                    )
                }
            } else {
                item(span = fullSpan) { CategoryDonutChart(data.donut, data.currency) }
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
    // la app. No cambia con el año seleccionado. AutoSizeText se encarga de que la cifra grande
    // siempre quepa en una línea.
    val fmt = LocalMoneyFormat.current
    val haptic = LocalHapticFeedback.current
    val balanceColor = if (totalBalance >= 0) onSurface else expenseRed
    val animatedBalance by animateFloatAsState(targetValue = totalBalance.toFloat(), animationSpec = tween(400))
    val balanceText = if (hidden) MASK else fmt.format(animatedBalance.toLong(), currency)

    FinanceCard(size = BentoTileSize.Hero, glass = true, contentPadding = PaddingValues(spacing.xl)) {
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

/** Tile de KPI del año seleccionado: recibe el valor ya formateado (dinero u otra unidad, p. ej.
 * un porcentaje) para servir tanto montos como tasas sin duplicar el componente. */
@Composable
private fun KpiCard(
    label: String,
    valueText: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    FinanceCard(modifier = modifier, contentPadding = PaddingValues(spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
            )
        }
    }
}
