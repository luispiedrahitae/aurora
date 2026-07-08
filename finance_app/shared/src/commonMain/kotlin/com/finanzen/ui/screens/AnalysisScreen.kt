package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IncomeExpenseLineChart
import com.finanzen.ui.components.MonthSelector
import com.finanzen.ui.components.NetWorthAreaChart
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.TopFrequentExpensesList
import com.finanzen.ui.format.formatMesAnio
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.AnalysisViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(onBack: () -> Unit, vm: AnalysisViewModel = koinViewModel()) {
    val data by vm.data.collectAsState()
    val month by vm.month.collectAsState()
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // bottom extra para que el FAB central no tape la última tarjeta.
                contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                item { TotalsCard(income = data.totalIncomeMinor, expense = data.totalExpenseMinor, currency = data.currency) }

                item { SectionHeader("Ingresos vs gastos") }
                item { IncomeExpenseLineChart(data.cashflow, data.currency) }

                item { SectionHeader("Patrimonio neto") }
                item { NetWorthAreaChart(data.netWorth, data.currency) }

                item { SectionHeader("Gastos más frecuentes") }
                item { TopFrequentExpensesList(data.frequent, data.currency) }
            }
        }
    }
}

@Composable
private fun TotalsCard(income: Long, expense: Long, currency: String) {
    val finance = LocalFinanceColors.current
    val fmt = LocalMoneyFormat.current
    val net = income - expense
    val netColor = if (net >= 0) finance.income else finance.expense

    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Balance del periodo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                fmt.format(net, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = netColor,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(label = "Ingresos", amount = income, currency = currency, color = finance.income)
                StatColumn(label = "Gastos", amount = expense, currency = currency, color = finance.expense)
                StatColumn(
                    label = "Gastado de lo ingresado",
                    amount = null,
                    currency = currency,
                    color = MaterialTheme.colorScheme.onSurface,
                    overrideText = if (income == 0L) "—" else "${(expense * 100 / income.coerceAtLeast(1))}%",
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    amount: Long?,
    currency: String,
    color: Color,
    overrideText: String? = null,
) {
    val fmt = LocalMoneyFormat.current
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            overrideText ?: (amount?.let { fmt.format(it, currency) } ?: "—"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
