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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class DashboardData(
    val totalBalanceMinor: Long,
    val monthIncomeMinor: Long,
    val monthExpenseMinor: Long,
    val currency: String,
    val topExpenses: List<CategorySlice>,
)

/**
 * Pantalla de inicio: balance general (saldos iniciales + ingresos − gastos de todo el
 * histórico), ingreso/gasto del mes en curso, y top 5 categorías de gasto de todo el histórico.
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
            DashboardData(0, 0, 0, DEFAULT_CURRENCY, emptyList()),
        )

    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        private const val TOP_CATEGORIES = 5
        private val NET_WORTH_TYPES = setOf("CASH", "DEBIT", "SAVINGS")

        private fun monthKey(year: Int, monthNumber: Int): Int = year * 100 + monthNumber
        private fun monthKeyOf(epochDay: Long): Int = LocalDate.fromEpochDays(epochDay.toInt()).let { monthKey(it.year, it.monthNumber) }

        /** Agregación pura. `firstOfMonth` = primer día del mes "actual" (inyectable para tests). */
        internal fun computeDashboard(
            txs: List<TransactionRow>,
            accounts: List<Account>,
            cats: List<Category>,
            firstOfMonth: LocalDate,
        ): DashboardData {
            val currentKey = monthKey(firstOfMonth.year, firstOfMonth.monthNumber)
            val currency = accounts.firstOrNull()?.currency ?: txs.firstOrNull()?.currency ?: DEFAULT_CURRENCY

            val countedAccountIds = accounts.filter { it.type in NET_WORTH_TYPES }.map { it.id }.toSet()
            val allIncome = txs.filter { it.kind == "INCOME" && it.accountId in countedAccountIds }.sumOf { it.amountMinor }
            val allExpense = txs.filter { it.kind == "EXPENSE" && it.accountId in countedAccountIds }.sumOf { it.amountMinor }
            val totalBalance = accounts.filter { it.type in NET_WORTH_TYPES }.sumOf { it.openingBalanceMinor } + allIncome - allExpense

            val monthTx = txs.filter { monthKeyOf(it.date) == currentKey }
            val monthIncome = monthTx.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val monthExpense = monthTx.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }

            val catNameById = cats.associate { it.id to it.name }
            val totalAllExp = allExpense.coerceAtLeast(1L)
            val topExpenses = txs.filter { it.kind == "EXPENSE" }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                .entries
                .sortedByDescending { it.value }
                .take(TOP_CATEGORIES)
                .map { (catId, amount) ->
                    CategorySlice(
                        name = catNameById[catId] ?: "Sin categoría",
                        amountMinor = amount,
                        pct = amount.toFloat() / totalAllExp.toFloat(),
                    )
                }

            return DashboardData(totalBalance, monthIncome, monthExpense, currency, topExpenses)
        }
    }
}
