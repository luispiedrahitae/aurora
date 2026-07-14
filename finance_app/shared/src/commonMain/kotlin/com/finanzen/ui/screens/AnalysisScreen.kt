package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IncomeExpenseBarChart
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.SavingsRateGauge
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.TopFrequentExpensesList
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.AnalysisViewModel
import com.finanzen.viewmodel.SettingsViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    vm: AnalysisViewModel = koinViewModel(),
    settingsVm: SettingsViewModel = koinInject(),
) {
    val data by vm.data.collectAsState()
    val month by vm.month.collectAsState()
    val savingsGoalPct by settingsVm.savingsGoalPct.collectAsState()
    val spacing = LocalSpacing.current
    val dateLocale = LocalDateLocale.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            // MonthSelector fijo fuera del LazyColumn (mismo patrón que DashboardScreen): no scrollea.
            MonthSelector(
                label = formatMesAnio(month, dateLocale),
                onPrev = { vm.setMonth(month.plus(DatePeriod(months = -1))) },
                onNext = { vm.setMonth(month.plus(DatePeriod(months = 1))) },
                modifier = Modifier.padding(horizontal = spacing.lg),
            )
            val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                // bottom extra para que el FAB central no tape la última tarjeta.
                contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                item(span = fullSpan) {
                    TotalsCard(income = data.totalIncomeMinor, expense = data.totalExpenseMinor, currency = data.currency)
                }
                item {
                    StatTile("Ingresos", data.totalIncomeMinor, data.currency, LocalFinanceColors.current.income)
                }
                item {
                    StatTile("Gastos", data.totalExpenseMinor, data.currency, LocalFinanceColors.current.expense)
                }

                item(span = fullSpan) {
                    StatTile(
                        "Suscripciones activas",
                        data.subscriptionMonthlyCostMinor,
                        data.currency,
                        MaterialTheme.colorScheme.onSurface,
                    )
                }

                item(span = fullSpan) {
                    SavingsRateGauge(
                        savingsRate = data.savingsRate,
                        goalPct = savingsGoalPct,
                        onGoalChange = settingsVm::setSavingsGoalPct,
                    )
                }

                item(span = fullSpan) { SectionHeader("Ingresos vs gastos") }
                item(span = fullSpan) { IncomeExpenseBarChart(data.cashflow, data.currency) }

                item(span = fullSpan) { SectionHeader("Gastos más frecuentes") }
                item(span = fullSpan) { TopFrequentExpensesList(data.frequent, data.currency) }
            }
        }
    }
}

/** Hero de vidrio (DESIGN.md, "The One Glass Tile Rule"): el único número que importa por pantalla. */
@Composable
private fun TotalsCard(income: Long, expense: Long, currency: String) {
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val net = income - expense
    val netColor = if (net >= 0) finance.income else finance.expense
    val spentPct = if (income == 0L) "—" else "${(expense * 100 / income.coerceAtLeast(1))}%"

    FinanceCard(modifier = Modifier.fillMaxWidth(), size = BentoTileSize.Hero, glass = true) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Balance del periodo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                fmt.format(net, currency),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                color = netColor,
            )
            Text(
                "Gastado de lo ingresado: $spentPct",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Tile de estadística del periodo (ingresos o gastos totales). */
@Composable
private fun StatTile(label: String, amountMinor: Long, currency: String, color: Color) {
    val fmt = LocalMoneyFormat.current
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                fmt.format(amountMinor, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
            )
        }
    }
}
