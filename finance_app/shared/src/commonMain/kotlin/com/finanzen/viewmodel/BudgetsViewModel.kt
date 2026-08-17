package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
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

/** Una categoría de gasto con su límite del mes (0 = sin presupuesto) y lo gastado en el mes.
 * [parentName] es null si [categoryId] es una categoría de nivel superior, o el nombre de su
 * categoría padre si es una subcategoría — así la UI puede distinguir un nivel del otro. */
data class BudgetRow(
    val categoryId: Long,
    val categoryName: String,
    val limitMinor: Long,
    val spentMinor: Long,
    val icon: String = "",
    val color: Long = 0L,
    val parentName: String? = null,
)

data class BudgetsData(val periodMonth: Long, val rows: List<BudgetRow>, val currency: String = "")

class BudgetsViewModel(
    private val budgetRepo: BudgetRepository,
    categoryRepo: CategoryRepository,
    txRepo: TransactionRepository,
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

    val data: StateFlow<BudgetsData> =
        combine(
            budgetRepo.observeAll(),
            categoryRepo.observeAll(),
            txRepo.observeAll(),
            _month,
        ) { budgets, cats, txs, month ->
            computeBudgets(budgets, cats, txs, monthPeriod(month))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsData(currentPeriodMonth(), emptyList()))

    fun setLimit(categoryId: Long, limitMinor: Long) = budgetRepo.setLimit(categoryId, monthPeriod(_month.value), limitMinor)

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
            // Herencia: si el mes no tiene una fila propia, se usa la más reciente anterior a él.
            val limitByCat = budgets.groupBy { it.categoryId }
                .mapValues { (_, rows) -> rows.filter { it.periodMonth <= period }.maxByOrNull { it.periodMonth }?.limitMinor ?: 0L }
            val spentByCat = txs.asSequence()
                .filter { it.kind == "EXPENSE" && it.categoryId != null && monthOf(it.date) == period }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            val catById = cats.associateBy { it.id }
            val rows = cats.filter { it.kind == "EXPENSE" }.map { cat ->
                BudgetRow(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    limitMinor = limitByCat[cat.id] ?: 0L,
                    spentMinor = spentByCat[cat.id] ?: 0L,
                    icon = cat.icon,
                    color = cat.color,
                    parentName = cat.parentId?.let { catById[it]?.name },
                )
            }.sortedWith(compareBy({ it.parentName != null }, { it.parentName ?: it.categoryName }, { it.categoryName }))
            val currency = txs.firstOrNull()?.currency ?: ""
            return BudgetsData(period, rows, currency)
        }
    }
}
