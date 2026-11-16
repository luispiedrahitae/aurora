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

class InvestmentRepository(private val db: FinanzenDb) {
    fun observeOpen(): Flow<List<Investment>> = db.investmentQueries.selectOpen().asFlow().mapToList(Dispatchers.Default)

    fun observeClosed(): Flow<List<Investment>> = db.investmentQueries.selectClosed().asFlow().mapToList(Dispatchers.Default)

    fun observeArchived(): Flow<List<Investment>> = db.investmentQueries.selectArchived().asFlow().mapToList(Dispatchers.Default)

    fun archive(id: Long) = db.investmentQueries.archive(id)

    fun unarchive(id: Long) = db.investmentQueries.unarchive(id)

    fun add(
        name: String,
        amountMinor: Long,
        currency: String,
        accountId: Long?,
        categoryId: Long,
        periodic: Boolean,
        frequency: String?,
        intervalCount: Long?,
        nextContributionDate: Long?,
        startDate: Long,
    ): Long = db.transactionWithResult {
        db.investmentQueries.insert(
            name, amountMinor, currency, accountId, categoryId,
            if (periodic) 1L else 0L, frequency, intervalCount, nextContributionDate, startDate,
        )
        db.investmentQueries.lastInsertRowId().executeAsOne()
    }

    fun openNow(): List<Investment> = db.investmentQueries.selectOpen().executeAsList()

    /** Abiertas + cerradas — para reportes de periodos pasados que aún deben mostrar el nombre
     * propio de una inversión ya cerrada. */
    fun all(): List<Investment> = db.investmentQueries.selectAll().executeAsList()

    fun byId(id: Long): Investment? = db.investmentQueries.selectById(id).executeAsOneOrNull()

    fun updateNextContribution(id: Long, next: Long) = db.investmentQueries.updateNextContribution(next, id)

    fun update(
        id: Long,
        name: String,
        amountMinor: Long,
        currency: String,
        accountId: Long?,
        periodic: Boolean,
        frequency: String?,
        intervalCount: Long?,
    ) = db.investmentQueries.update(name, amountMinor, currency, accountId, if (periodic) 1L else 0L, frequency, intervalCount, id)

    fun close(id: Long, withdrawnAmountMinor: Long, closedDate: Long, yieldMinor: Long) = db.investmentQueries.close(withdrawnAmountMinor, closedDate, yieldMinor, id)

    fun reopen(id: Long, nextContributionDate: Long?) = db.investmentQueries.reopen(nextContributionDate, id)

    fun delete(id: Long) = db.investmentQueries.delete(id)

    fun clearAccount(accountId: Long) = db.investmentQueries.clearAccount(accountId)
}
