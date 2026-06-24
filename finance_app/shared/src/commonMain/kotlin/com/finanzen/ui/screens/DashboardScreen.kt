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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finanzen.domain.Money
import com.finanzen.ui.components.CategoryProgressRow
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.StatPill
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.viewmodel.CategorySlice
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.MonthNet
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

@Composable
fun DashboardScreen(vm: DashboardViewModel = koinViewModel()) {
    val data by vm.data.collectAsState()
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // bottom extra para que el FAB central no tape la última tarjeta.
        contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item { BalanceHeroCard(data.totalBalanceMinor, data.monthIncomeMinor, data.monthExpenseMinor, data.currency) }

        item { SectionHeader("Flujo de los últimos 6 meses") }
        item { CashflowCard(data.cashflow, data.currency) }

        item { SectionHeader("Top categorías del mes") }
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
                FinanceCard { CategoryRow(slice, data.currency) }
            }
        }
    }
}

@Composable
private fun BalanceHeroCard(balance: Long, monthIncome: Long, monthExpense: Long, currency: String) {
    val finance = LocalFinanceColors.current
    val spacing = LocalSpacing.current
    // ponytail: anima como Float; para saldos > ~9 dígitos pierde precisión visual durante el barrido,
    // el valor final mostrado siempre es el Long exacto. Suficiente para finanzas personales.
    val animated by animateFloatAsState(targetValue = balance.toFloat(), animationSpec = tween(700))
    val balanceColor = if (balance >= 0) MaterialTheme.colorScheme.onPrimaryContainer else finance.expense

    FinanceCard(color = MaterialTheme.colorScheme.primaryContainer, contentPadding = PaddingValues(spacing.xl)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    "Balance total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                MoneyText(
                    amountMinor = animated.toLong(),
                    currency = currency,
                    style = MaterialTheme.typography.displayLarge,
                    colorOverride = balanceColor,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                StatPill("Ingresos", monthIncome, currency, finance.income, finance.incomeContainer, Modifier.weight(1f))
                StatPill("Gastos", monthExpense, currency, finance.expense, finance.expenseContainer, Modifier.weight(1f))
                StatPill(
                    "Neto",
                    monthIncome - monthExpense,
                    currency,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CashflowCard(cashflow: List<MonthNet>, currency: String) {
    val maxAbs = (cashflow.maxOfOrNull { it.netMinor.absoluteValue } ?: 0L).coerceAtLeast(1L)
    FinanceCard {
        Row(
            modifier = Modifier.fillMaxWidth().height(150.dp),
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
    val finance = LocalFinanceColors.current
    val targetFraction = (month.netMinor.absoluteValue.toFloat() / maxAbs.toFloat()).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(targetValue = targetFraction, animationSpec = tween(600))
    val barColor = if (month.netMinor >= 0) finance.income else finance.expense
    val gradient = Brush.verticalGradient(listOf(barColor.copy(alpha = 0.85f), barColor))
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
                .width(28.dp)
                .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(gradient),
        )
        Text(month.label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice, currency: String) {
    CategoryProgressRow(
        name = slice.name,
        amountMinor = slice.amountMinor,
        currency = currency,
        pct = slice.pct,
        color = Color(slice.colorHex),
    )
}
