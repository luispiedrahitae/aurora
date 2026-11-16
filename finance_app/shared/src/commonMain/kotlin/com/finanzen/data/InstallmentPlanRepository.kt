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

class InstallmentPlanRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<InstallmentPlan>> = db.installmentPlanQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun activeByCard(cardId: Long): List<InstallmentPlan> = db.installmentPlanQueries.selectActiveByCard(cardId).executeAsList()

    fun add(
        cardId: Long,
        categoryId: Long,
        totalAmountMinor: Long,
        installments: Long,
        interestRate: Double,
        startDateEpochDay: Long,
        description: String,
    ): Long = db.transactionWithResult {
        db.installmentPlanQueries.insert(cardId, categoryId, totalAmountMinor, installments, interestRate, startDateEpochDay, description, settled = 0)
        db.installmentPlanQueries.lastInsertRowId().executeAsOne()
    }

    fun delete(id: Long) = db.installmentPlanQueries.delete(id)

    fun deleteByCard(cardId: Long) = db.installmentPlanQueries.deleteByCard(cardId)

    fun settleAllForCard(cardId: Long) = db.installmentPlanQueries.settleAllByCard(cardId)
}
