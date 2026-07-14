package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.db.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CategoriesViewModel(private val repo: CategoryRepository, private val budgetRepo: BudgetRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** kind = INCOME | EXPENSE. parentId no nulo crea una subcategoría. Ignora nombres vacíos. */
    fun add(name: String, kind: String, parentId: Long?, icon: String = "", color: Long = 0) {
        if (name.isBlank()) return
        repo.add(name = name.trim(), kind = kind, parentId = parentId, icon = icon, color = color)
    }

    /** Borra la categoría y cualquier presupuesto que la referencie — el enforcement de FKs de
     * SQLite no está garantizado, así que la cascada se hace a mano (mismo patrón que
     * AccountsViewModel.delete()). */
    fun delete(id: Long) {
        budgetRepo.deleteByCategory(id)
        repo.delete(id)
    }
}
