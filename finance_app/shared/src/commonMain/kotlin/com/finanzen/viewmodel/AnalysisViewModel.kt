package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
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

/** Un mes del gráfico anual de gastos. */
data class MonthExpense(val label: String, val expenseMinor: Long)

data class AnalysisData(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val topExpenses: List<CategorySlice>,
    val yearlyExpenses: List<MonthExpense>,
    val currency: String,
)

class AnalysisViewModel(
    txRepo: TransactionRepository,
    categoryRepo: CategoryRepository,
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
        combine(txRepo.observeAll(), categoryRepo.observeAll(), _month) { txs, cats, month ->
            val period = monthPeriod(month)
            val periodTxs = txs.filter { periodOfEpochDay(it.date) == period }
            val incomes = periodTxs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val expenses = periodTxs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
            val currency = periodTxs.firstOrNull()?.currency ?: txs.firstOrNull()?.currency ?: "USD"

            val byCatId = periodTxs.filter { it.kind == "EXPENSE" }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }

            val catNameById = cats.associate { it.id to it.name }
            val totalExp = expenses.coerceAtLeast(1L)
            val topExpenses = byCatId.entries
                .sortedByDescending { it.value }
                .map { (catId, amount) ->
                    CategorySlice(
                        name = catNameById[catId] ?: "Sin categoría",
                        amountMinor = amount,
                        pct = amount.toFloat() / totalExp.toFloat(),
                    )
                }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val yearlyExpenses = computeYearlyExpenses(txs, LocalDate(today.year, today.month, 1))

            AnalysisData(incomes, expenses, topExpenses, yearlyExpenses, currency)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnalysisData(0, 0, emptyList(), emptyList(), "USD"),
        )

    companion object {
        private const val YEARLY_MONTHS = 12

        /** Gasto de cada uno de los últimos 12 meses relativos a [referenceMonth] (ventana fija,
         * no depende del mes seleccionado en pantalla). */
        internal fun computeYearlyExpenses(txs: List<TransactionRow>, referenceMonth: LocalDate): List<MonthExpense> {
            val expenseByPeriod = txs.filter { it.kind == "EXPENSE" }
                .groupBy { periodOfEpochDay(it.date) }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            return (YEARLY_MONTHS - 1 downTo 0).map { back ->
                val m = referenceMonth.plus(DatePeriod(months = -back))
                MonthExpense(
                    label = "${m.monthNumber}/${m.year % 100}",
                    expenseMinor = expenseByPeriod[monthPeriod(m)] ?: 0L,
                )
            }
        }
    }
}
