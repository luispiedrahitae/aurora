package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.ui.format.formatDiaMes
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

/** Un día del gráfico de flujo (mes seleccionado): ingreso y gasto en paralelo. */
data class DayPoint(val label: String, val incomeMinor: Long, val expenseMinor: Long)

/** Patrimonio neto acumulado al cierre de un día del mes seleccionado. */
data class DayNetWorth(val label: String, val netWorthMinor: Long)

/** Categoría de gasto ordenada por frecuencia (gasto hormiga). */
data class FrequentExpense(val name: String, val count: Int, val amountMinor: Long)

data class AnalysisData(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val currency: String,
    val cashflow: List<DayPoint>,
    val netWorth: List<DayNetWorth>,
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
        private const val FREQUENT_TOP = 5

        private fun daysInMonth(firstOfMonth: LocalDate): Int = firstOfMonth.plus(DatePeriod(months = 1)).toEpochDays() - firstOfMonth.toEpochDays()

        /** Ingreso y gasto de cada día del mes de [referenceMonth] (equivalente táctil de "hover": cada
         * día es un punto tocable, no hay hover real en Android/iOS). */
        internal fun computeCashflow(txs: List<TransactionRow>, referenceMonth: LocalDate): List<DayPoint> {
            val periodTxs = txs.filter { periodOfEpochDay(it.date) == monthPeriod(referenceMonth) }
            val byDay = periodTxs.groupBy { it.date }
            val firstEpochDay = referenceMonth.toEpochDays()
            return (0 until daysInMonth(referenceMonth)).map { offset ->
                val epochDay = (firstEpochDay + offset).toLong()
                val dayTxs = byDay[epochDay].orEmpty()
                DayPoint(
                    label = formatDiaMes(epochDay),
                    incomeMinor = dayTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor },
                    expenseMinor = dayTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor },
                )
            }
        }

        /** Patrimonio neto acumulado al cierre de cada día del mes de [referenceMonth]: saldo inicial
         * de las cuentas de patrimonio + (ingresos − gastos) de esas cuentas hasta ese día, inclusive. */
        internal fun computeNetWorth(accounts: List<Account>, txs: List<TransactionRow>, referenceMonth: LocalDate): List<DayNetWorth> {
            val countedAccountIds = accounts.filter { it.type in DashboardViewModel.NET_WORTH_TYPES }.map { it.id }.toSet()
            val opening = accounts.filter { it.type in DashboardViewModel.NET_WORTH_TYPES }.sumOf { it.openingBalanceMinor }
            val countedTxs = txs.filter { it.accountId in countedAccountIds }
            val firstEpochDay = referenceMonth.toEpochDays()
            return (0 until daysInMonth(referenceMonth)).map { offset ->
                val epochDay = (firstEpochDay + offset).toLong()
                val upTo = countedTxs.filter { it.date <= epochDay }
                val income = upTo.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
                val expense = upTo.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
                DayNetWorth(
                    label = formatDiaMes(epochDay),
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
