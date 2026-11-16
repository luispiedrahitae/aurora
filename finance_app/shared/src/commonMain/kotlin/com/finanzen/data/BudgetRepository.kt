package com.finanzen.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.finanzen.db.Account
import com.finanzen.db.Budget
import com.finanzen.db.Card
import com.finanzen.db.Category
import com.finanzen.db.Currency
import com.finanzen.db.FinanzenDb
import com.finanzen.db.InstallmentPlan
import com.finanzen.db.Investment
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<Budget>> = db.budgetQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun all(): List<Budget> = db.budgetQueries.selectAll().executeAsList()

    fun setLimit(categoryId: Long, periodMonth: Long, limitMinor: Long) = db.budgetQueries.upsert(categoryId = categoryId, periodMonth = periodMonth, limitMinor = limitMinor)

    /** Límite efectivo de una categoría en un periodo (0 si nunca tuvo presupuesto). Hereda del mes
     * anterior más reciente si el periodo pedido no tiene una fila propia. */
    fun limitFor(categoryId: Long, periodMonth: Long): Long = db.budgetQueries.selectAll().executeAsList()
        .filter { it.categoryId == categoryId && it.periodMonth <= periodMonth }
        .maxByOrNull { it.periodMonth }?.limitMinor ?: 0L

    fun delete(id: Long) = db.budgetQueries.delete(id)

    fun deleteByCategory(categoryId: Long) = db.budgetQueries.deleteByCategory(categoryId)
}
