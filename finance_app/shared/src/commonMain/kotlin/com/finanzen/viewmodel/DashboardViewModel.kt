package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/** Un mes del mini-gráfico de cashflow. netMinor puede ser negativo. */
data class MonthNet(val label: String, val netMinor: Long)

data class DashboardData(
    val totalBalanceMinor: Long,
    val monthIncomeMinor: Long,
    val monthExpenseMinor: Long,
    val currency: String,
    val topCategories: List<CategorySlice>,
    val cashflow: List<MonthNet>,
)

/**
 * Pantalla de inicio: balance total (saldos iniciales + ingresos − gastos de todo el histórico),
 * ingreso/gasto del mes en curso, top 5 categorías del mes y cashflow neto de los últimos 6 meses.
 * Reusa `CategorySlice` de [AnalysisViewModel]. El cálculo vive en [computeDashboard] (testeable);
 * nada se calcula en la UI.
 */
class DashboardViewModel(
    txRepo: TransactionRepository,
    accountRepo: AccountRepository,
    categoryRepo: CategoryRepository,
) : ViewModel() {

    val data: StateFlow<DashboardData> =
        combine(
            txRepo.observeAll(),
            accountRepo.observeAll(),
            categoryRepo.observeAll(),
        ) { txs, accounts, cats ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            computeDashboard(txs, accounts, cats, LocalDate(today.year, today.month, 1))
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardData(0, 0, 0, DEFAULT_CURRENCY, emptyList(), emptyList()),
        )

    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        private const val TOP_CATEGORIES = 5
        private const val CASHFLOW_MONTHS = 6

        private fun monthKey(year: Int, monthNumber: Int): Int = year * 100 + monthNumber
        private fun monthKeyOf(epochDay: Long): Int = LocalDate.fromEpochDays(epochDay.toInt()).let { monthKey(it.year, it.monthNumber) }

        private fun signedAmount(kind: String, amountMinor: Long): Long = when (kind) {
            "INCOME" -> amountMinor
            "EXPENSE" -> -amountMinor
            else -> 0L
        }

        private val palette: List<Long> = listOf(
            0xFF1E6F5C,
            0xFF4A635D,
            0xFF8E6B33,
            0xFFB13E53,
            0xFF7A4988,
            0xFF2E6E9D,
            0xFFCB763E,
            0xFF566246,
        )

        /** Agregación pura. `firstOfMonth` = primer día del mes "actual" (inyectable para tests). */
        internal fun computeDashboard(
            txs: List<TransactionRow>,
            accounts: List<Account>,
            cats: List<Category>,
            firstOfMonth: LocalDate,
        ): DashboardData {
            val currentKey = monthKey(firstOfMonth.year, firstOfMonth.monthNumber)
            val currency = accounts.firstOrNull()?.currency ?: txs.firstOrNull()?.currency ?: DEFAULT_CURRENCY

            val allIncome = txs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val allExpense = txs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
            val totalBalance = accounts.sumOf { it.openingBalanceMinor } + allIncome - allExpense

            val monthTx = txs.filter { monthKeyOf(it.date) == currentKey }
            val monthIncome = monthTx.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val monthExpense = monthTx.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

            val catNameById = cats.associate { it.id to it.name }
            val totalMonthExp = monthExpense.coerceAtLeast(1L)
            val topCategories = monthTx.filter { it.kind == "EXPENSE" }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                .entries
                .sortedByDescending { it.value }
                .take(TOP_CATEGORIES)
                .mapIndexed { idx, (catId, amount) ->
                    CategorySlice(
                        name = catNameById[catId] ?: "Sin categoría",
                        amountMinor = amount,
                        pct = amount.toFloat() / totalMonthExp.toFloat(),
                        colorHex = palette[idx % palette.size],
                    )
                }

            val netByKey = txs.groupBy { monthKeyOf(it.date) }
                .mapValues { (_, list) -> list.sumOf { signedAmount(it.kind, it.amountMinor) } }
            val cashflow = (CASHFLOW_MONTHS - 1 downTo 0).map { back ->
                val m = firstOfMonth.plus(DatePeriod(months = -back))
                MonthNet(
                    label = "${m.monthNumber}/${m.year % 100}",
                    netMinor = netByKey[monthKey(m.year, m.monthNumber)] ?: 0L,
                )
            }

            return DashboardData(totalBalance, monthIncome, monthExpense, currency, topCategories, cashflow)
        }
    }
}
