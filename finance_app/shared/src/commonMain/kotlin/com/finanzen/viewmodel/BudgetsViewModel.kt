package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Budget
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

/** Una categoría de gasto con su límite del mes (0 = sin presupuesto) y lo gastado en el mes. */
data class BudgetRow(
    val categoryId: Long,
    val categoryName: String,
    val limitMinor: Long,
    val spentMinor: Long,
)

data class BudgetsData(val periodMonth: Long, val rows: List<BudgetRow>)

class BudgetsViewModel(
    private val budgetRepo: BudgetRepository,
    categoryRepo: CategoryRepository,
    txRepo: TransactionRepository,
) : ViewModel() {

    val data: StateFlow<BudgetsData> =
        combine(
            budgetRepo.observeAll(),
            categoryRepo.observeAll(),
            txRepo.observeAll(),
        ) { budgets, cats, txs ->
            val period = currentPeriodMonth()
            computeBudgets(budgets, cats, txs, period)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsData(currentPeriodMonth(), emptyList()))

    fun setLimit(categoryId: Long, limitMinor: Long) = budgetRepo.setLimit(categoryId, currentPeriodMonth(), limitMinor)

    companion object {
        /** YYYYMM del mes en curso, p.ej. 202606. */
        fun currentPeriodMonth(): Long {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return (today.year * 100 + today.monthNumber).toLong()
        }

        private fun monthOf(epochDay: Long): Long = LocalDate.fromEpochDays(epochDay.toInt()).let { (it.year * 100 + it.monthNumber).toLong() }

        internal fun computeBudgets(
            budgets: List<Budget>,
            cats: List<Category>,
            txs: List<TransactionRow>,
            period: Long,
        ): BudgetsData {
            val limitByCat = budgets.filter { it.periodMonth == period }.associate { it.categoryId to it.limitMinor }
            val spentByCat = txs.asSequence()
                .filter { it.kind == "EXPENSE" && it.categoryId != null && monthOf(it.date) == period }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            val rows = cats.filter { it.kind == "EXPENSE" }.map { cat ->
                BudgetRow(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    limitMinor = limitByCat[cat.id] ?: 0L,
                    spentMinor = spentByCat[cat.id] ?: 0L,
                )
            }
            return BudgetsData(period, rows)
        }
    }
}
