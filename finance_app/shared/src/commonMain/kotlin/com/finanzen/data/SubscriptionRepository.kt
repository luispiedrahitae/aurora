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

class SubscriptionRepository(private val db: FinanzenDb) {
    fun observeActive(): Flow<List<Subscription>> = db.subscriptionQueries.selectActive().asFlow().mapToList(Dispatchers.Default)

    fun add(
        name: String,
        amountMinor: Long,
        currency: String,
        categoryId: Long,
        accountId: Long?,
        frequency: String,
        intervalCount: Long,
        nextChargeDateEpochDay: Long,
        remindDaysBefore: Long,
    ): Long = db.transactionWithResult {
        db.subscriptionQueries.insert(
            name, amountMinor, currency, categoryId, accountId,
            frequency, intervalCount, nextChargeDateEpochDay, remindDaysBefore, active = 1,
        )
        db.subscriptionQueries.lastInsertRowId().executeAsOne()
    }

    fun activeNow(): List<Subscription> = db.subscriptionQueries.selectActive().executeAsList()

    fun all(): List<Subscription> = db.subscriptionQueries.selectAll().executeAsList()

    fun byId(id: Long): Subscription? = db.subscriptionQueries.selectById(id).executeAsOneOrNull()

    fun updateNextCharge(id: Long, nextChargeDateEpochDay: Long) = db.subscriptionQueries.updateNextCharge(nextChargeDateEpochDay, id)

    fun update(id: Long, name: String, amountMinor: Long, currency: String, accountId: Long?, frequency: String, intervalCount: Long) = db.subscriptionQueries.update(name, amountMinor, currency, accountId, frequency, intervalCount, id)

    fun delete(id: Long) = db.subscriptionQueries.delete(id)

    fun clearAccount(accountId: Long) = db.subscriptionQueries.clearAccount(accountId)
}
