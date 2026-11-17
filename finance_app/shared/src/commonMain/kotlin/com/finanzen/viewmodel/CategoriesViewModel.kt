package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.CategoryRepository
import com.finanzen.db.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CategoriesViewModel(private val repo: CategoryRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** kind = INCOME | EXPENSE. parentId no nulo crea una subcategoría. Ignora nombres vacíos. */
    fun add(name: String, kind: String, parentId: Long?, icon: String = "", color: Long = 0) {
        if (name.isBlank()) return
        repo.add(name = name.trim(), kind = kind, parentId = parentId, icon = icon, color = color)
    }

    /** Cambia solo el nombre; tipo, padre, icono y color se conservan. Ignora nombres vacíos. */
    fun rename(category: Category, newName: String) {
        if (newName.isBlank()) return
        repo.update(
            id = category.id,
            name = newName.trim(),
            kind = category.kind,
            parentId = category.parentId,
            icon = category.icon,
            color = category.color,
        )
    }

    /** Borra la categoría (y sus subcategorías si es de nivel superior) junto con todo lo que la
     * referencia — movimientos, presupuestos, suscripciones, inversiones y planes de cuotas. Ver
     * [CategoryRepository.deleteCascade]. */
    fun delete(id: Long) {
        repo.deleteCascade(id)
    }
}
