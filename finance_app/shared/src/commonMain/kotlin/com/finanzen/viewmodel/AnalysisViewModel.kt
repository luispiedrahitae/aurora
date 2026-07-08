package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.ui.format.monthPeriod
import com.finanzen.ui.format.periodOfEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
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

data class CategorySlice(val name: String, val amountMinor: Long, val pct: Float)

/** Un mes del gráfico de flujo: ingreso y gasto en paralelo. */
data class MonthPoint(val label: String, val incomeMinor: Long, val expenseMinor: Long)

/** Patrimonio neto acumulado al cierre de un mes. */
data class MonthNetWorth(val label: String, val netWorthMinor: Long)

/** Categoría de gasto ordenada por frecuencia (gasto hormiga). */
data class FrequentExpense(val name: String, val count: Int, val amountMinor: Long)

data class AnalysisData(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val currency: String,
    val cashflow: List<MonthPoint>,
    val netWorth: List<MonthNetWorth>,
    val frequent: List<FrequentExpense>,
)

class AnalysisViewModel(
    txRepo: TransactionRepository,
    categoryRepo: CategoryRepository,
    accountRepo: AccountRepository,
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

    val data: StateFlow<AnalysisData> =
        combine(txRepo.observeAll(), categoryRepo.observeAll(), accountRepo.observeAll(), _month) { txs, cats, accounts, month ->
            val period = monthPeriod(month)
            val periodTxs = txs.filter { periodOfEpochDay(it.date) == period }
            val incomes = periodTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val expenses = periodTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
            val currency = periodTxs.firstOrNull()?.currency ?: txs.firstOrNull()?.currency ?: "USD"

            val cashflow = computeCashflow(txs, month)
            val netWorth = computeNetWorth(accounts, txs, month)
            val frequent = computeFrequentExpenses(txs, cats, period)

            AnalysisData(incomes, expenses, currency, cashflow, netWorth, frequent)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnalysisData(0, 0, "USD", emptyList(), emptyList(), emptyList()),
        )

    companion object {
        private const val CASHFLOW_MONTHS = 6
        private const val FREQUENT_TOP = 5

        /** Ingreso y gasto de cada uno de los últimos [months] meses relativos a [referenceMonth]. */
        internal fun computeCashflow(txs: List<TransactionRow>, referenceMonth: LocalDate, months: Int = CASHFLOW_MONTHS): List<MonthPoint> {
            val byPeriod = txs.groupBy { periodOfEpochDay(it.date) }
            return (months - 1 downTo 0).map { back ->
                val m = referenceMonth.plus(DatePeriod(months = -back))
                val monthTxs = byPeriod[monthPeriod(m)].orEmpty()
                MonthPoint(
                    label = "${m.monthNumber}/${m.year % 100}",
                    incomeMinor = monthTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor },
                    expenseMinor = monthTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor },
                )
            }
        }

        /** Patrimonio neto acumulado al cierre de cada uno de los últimos [months] meses: saldo inicial
         * de las cuentas de patrimonio + (ingresos − gastos) de esas cuentas hasta el fin de cada mes. */
        internal fun computeNetWorth(accounts: List<Account>, txs: List<TransactionRow>, referenceMonth: LocalDate, months: Int = CASHFLOW_MONTHS): List<MonthNetWorth> {
            val countedAccountIds = accounts.filter { it.type in DashboardViewModel.NET_WORTH_TYPES }.map { it.id }.toSet()
            val opening = accounts.filter { it.type in DashboardViewModel.NET_WORTH_TYPES }.sumOf { it.openingBalanceMinor }
            val countedTxs = txs.filter { it.accountId in countedAccountIds }
            return (months - 1 downTo 0).map { back ->
                val m = referenceMonth.plus(DatePeriod(months = -back))
                val cutoff = monthPeriod(m)
                val upTo = countedTxs.filter { periodOfEpochDay(it.date) <= cutoff }
                val income = upTo.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
                val expense = upTo.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
                MonthNetWorth(
                    label = "${m.monthNumber}/${m.year % 100}",
                    netWorthMinor = opening + income - expense,
                )
            }
        }

        /** Gasto hormiga: categorías de gasto del [period] ordenadas por número de movimientos. */
        internal fun computeFrequentExpenses(txs: List<TransactionRow>, cats: List<Category>, period: Long, topN: Int = FREQUENT_TOP): List<FrequentExpense> {
            val catNameById = cats.associate { it.id to it.name }
            return txs.filter { it.kind == "EXPENSE" && periodOfEpochDay(it.date) == period }
                .groupBy { it.categoryId }
                .map { (catId, list) ->
                    FrequentExpense(
                        name = catNameById[catId] ?: "Sin categoría",
                        count = list.size,
                        amountMinor = list.sumOf { it.amountMinor },
                    )
                }
                .sortedByDescending { it.count }
                .take(topN)
        }
    }
}
