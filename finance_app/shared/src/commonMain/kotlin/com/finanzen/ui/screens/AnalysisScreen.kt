package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
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

        item {
            Text(
                "Gasto por categoría",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        CategoryPieChart(data.byCategory, currency = data.currency)
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(income: Long, expense: Long, currency: String) {
    val net = income - expense
    val netColor = if (net >= 0) MaterialTheme.colorScheme.primary else Color(0xFFB13E53)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Balance del periodo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                Money(net, currency).format() + " " + currency,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = netColor,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(label = "Ingresos", amount = income, currency = currency, color = MaterialTheme.colorScheme.primary)
                StatColumn(label = "Gastos", amount = expense, currency = currency, color = Color(0xFFB13E53))
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
