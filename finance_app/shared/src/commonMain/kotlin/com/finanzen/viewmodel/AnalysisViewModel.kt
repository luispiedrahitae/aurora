package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategorySlice(val name: String, val amountMinor: Long, val pct: Float, val colorHex: Long)

data class AnalysisData(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val byCategory: List<CategorySlice>,
    val currency: String,
)

class AnalysisViewModel(
    txRepo: TransactionRepository,
    categoryRepo: CategoryRepository,
) : ViewModel() {

    val data: StateFlow<AnalysisData> =
        combine(txRepo.observeAll(), categoryRepo.observeAll()) { txs, cats ->
            val incomes = txs.filter { it.kind == "INCOME" }.sumOf { it.amountMinor }
            val expenses = txs.filter { it.kind == "EXPENSE" }.sumOf { it.amountMinor }
            val currency = txs.firstOrNull()?.currency ?: "USD"

            val byCatId = txs.filter { it.kind == "EXPENSE" }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }

            val catNameById = cats.associate { it.id to it.name }
            val totalExp = expenses.coerceAtLeast(1L)
            val slices = byCatId.entries
                .sortedByDescending { it.value }
                .mapIndexed { idx, (catId, amount) ->
                    CategorySlice(
                        name = catNameById[catId] ?: "Sin categoría",
                        amountMinor = amount,
                        pct = amount.toFloat() / totalExp.toFloat(),
                        colorHex = palette[idx % palette.size],
                    )
                }
            AnalysisData(incomes, expenses, slices, currency)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnalysisData(0, 0, emptyList(), "USD"),
        )

    companion object {
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
    }
}
