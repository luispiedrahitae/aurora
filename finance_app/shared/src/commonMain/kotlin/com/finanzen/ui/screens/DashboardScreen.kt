package com.finanzen.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.viewmodel.CategorySlice
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.MonthNet
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

private val ExpenseRed = Color(0xFFB13E53)

@Composable
fun DashboardScreen(vm: DashboardViewModel = koinViewModel()) {
    val data by vm.data.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BalanceCard(data.totalBalanceMinor, data.monthIncomeMinor, data.monthExpenseMinor, data.currency) }

        item {
            SectionTitle("Flujo de los últimos 6 meses")
        }
        item { CashflowCard(data.cashflow, data.currency) }

        item { SectionTitle("Top categorías del mes") }
        if (data.topCategories.isEmpty()) {
            item {
                Text(
                    "Sin gastos este mes. Añade transacciones desde la pestaña Transacciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(data.topCategories, key = { it.name }) { slice ->
                CategoryRow(slice, data.currency)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun BalanceCard(balance: Long, monthIncome: Long, monthExpense: Long, currency: String) {
    val balanceColor = if (balance >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Balance total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${Money(balance, currency).format()} $currency",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = balanceColor,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Ingresos (mes)", monthIncome, currency, MaterialTheme.colorScheme.primary)
                Stat("Gastos (mes)", monthExpense, currency, ExpenseRed)
                Stat("Neto (mes)", monthIncome - monthExpense, currency, MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun Stat(label: String, amount: Long, currency: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Money(amount, currency).format(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun CashflowCard(cashflow: List<MonthNet>, currency: String) {
    val maxAbs = (cashflow.maxOfOrNull { it.netMinor.absoluteValue } ?: 0L).coerceAtLeast(1L)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            cashflow.forEach { month ->
                CashflowBar(month, maxAbs, currency, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CashflowBar(month: MonthNet, maxAbs: Long, currency: String, modifier: Modifier) {
    val fraction = (month.netMinor.absoluteValue.toFloat() / maxAbs.toFloat()).coerceIn(0f, 1f)
    val barColor = if (month.netMinor >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            Money(month.netMinor, currency).format(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(24.dp)
                .fillMaxHeight(fraction)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor),
        )
        Text(month.label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice, currency: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(slice.name, fontWeight = FontWeight.SemiBold)
                Text("${Money(slice.amountMinor, currency).format()} $currency", style = MaterialTheme.typography.bodyMedium)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(slice.pct.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(slice.colorHex)),
                )
            }
        }
    }
}
