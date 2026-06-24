package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.ui.components.CategoryPieChart
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.AnalysisViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalysisScreen(vm: AnalysisViewModel = koinViewModel()) {
    val data by vm.data.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TotalsCard(income = data.totalIncomeMinor, expense = data.totalExpenseMinor, currency = data.currency) }

        item { SectionHeader("Gasto por categoría") }

        if (data.byCategory.isEmpty()) {
            item {
                Text(
                    "Sin gastos categorizados todavía. Añade transacciones desde la pestaña Transacciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Box {
                        CategoryPieChart(data.byCategory, currency = data.currency)
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(income: Long, expense: Long, currency: String) {
    val finance = LocalFinanceColors.current
    val net = income - expense
    val netColor = if (net >= 0) finance.income else finance.expense

    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Balance del periodo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                Money(net, currency).format() + " " + currency,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = netColor,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(label = "Ingresos", amount = income, currency = currency, color = finance.income)
                StatColumn(label = "Gastos", amount = expense, currency = currency, color = finance.expense)
                StatColumn(
                    label = "Ratio",
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
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            overrideText ?: (amount?.let { Money(it, currency).format() } ?: "—"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
