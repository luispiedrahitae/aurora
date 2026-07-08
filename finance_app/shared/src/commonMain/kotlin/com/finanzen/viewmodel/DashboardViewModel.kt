package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Budget
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.ui.format.monthPeriod
import kotlinx.coroutines.flow.MutableStateFlow
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
    val netMinor: Long,
    val savingsRate: Float,
    val accounts: List<AccountBar>,
    val donut: List<CategorySlice>,
    val budgets: List<BudgetRow>,
)

/** Una cuenta con su saldo actual, para la fila de barras del inicio. */
data class AccountBar(val name: String, val type: String, val balanceMinor: Long)

/**
 * Pantalla de inicio: balance general (saldos iniciales + ingresos − gastos de todo el
 * histórico), ingreso/gasto/neto/tasa de ahorro del mes en curso, saldos por cuenta, rosca de
 * gasto del mes y presupuesto vs gastado. Reusa `CategorySlice` de [AnalysisViewModel]. El cálculo
 * vive en [computeDashboard] (testeable); nada se calcula en la UI.
 */
class DashboardViewModel(
    txRepo: TransactionRepository,
    accountRepo: AccountRepository,
    categoryRepo: CategoryRepository,
    budgetRepo: BudgetRepository,
) : ViewModel() {

    private val _month = MutableStateFlow(
        run {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            LocalDate(today.year, today.month, 1)
        },
    )
    val month: StateFlow<LocalDate> = _month

    fun setMonth(month: LocalDate) {
        _month.value = month
    }

    val data: StateFlow<DashboardData> =
        combine(
            txRepo.observeAll(),
            accountRepo.observeAll(),
            categoryRepo.observeAll(),
            budgetRepo.observeAll(),
            _month,
        ) { txs, accounts, cats, budgets, month ->
            computeDashboard(txs, accounts, cats, budgets, month)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardData(0, 0, 0, DEFAULT_CURRENCY, 0L, 0f, emptyList(), emptyList(), emptyList()),
        )

    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        internal val NET_WORTH_TYPES = setOf("CASH", "DEBIT", "SAVINGS")

        private fun monthKey(year: Int, monthNumber: Int): Int = year * 100 + monthNumber
        private fun monthKeyOf(epochDay: Long): Int = LocalDate.fromEpochDays(epochDay.toInt()).let { monthKey(it.year, it.monthNumber) }

        /** Agregación pura. `firstOfMonth` = primer día del mes "actual" (inyectable para tests). */
        internal fun computeDashboard(
            txs: List<TransactionRow>,
            accounts: List<Account>,
            cats: List<Category>,
            budgets: List<Budget>,
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

            val netMinor = monthIncome - monthExpense
            val savingsRate = if (monthIncome == 0L) {
                0f
            } else {
                ((monthIncome - monthExpense).toFloat() / monthIncome.toFloat()).coerceIn(0f, 1f)
            }

            val balanceByAccountId = AccountsViewModel.computeBalances(accounts, txs)
            val accountBars = accounts.map {
                AccountBar(name = it.name, type = it.type, balanceMinor = balanceByAccountId[it.id] ?: 0L)
            }

            val monthExpenseFloor = monthExpense.coerceAtLeast(1L).toFloat()
            val donut = monthTx.filter { it.kind == "EXPENSE" }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                .entries
                .sortedByDescending { it.value }
                .map { (catId, amount) ->
                    CategorySlice(
                        name = catNameById[catId] ?: "Sin categoría",
                        amountMinor = amount,
                        pct = amount.toFloat() / monthExpenseFloor,
                    )
                }

            val budgetRows = BudgetsViewModel.computeBudgets(budgets, cats, txs, monthPeriod(firstOfMonth))
                .rows.filter { it.limitMinor > 0 }

            return DashboardData(
                totalBalance, monthIncome, monthExpense, currency,
                netMinor, savingsRate, accountBars, donut, budgetRows,
            )
        }
    }
}
