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

class CardRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<Card>> = db.cardQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun all(): List<Card> = db.cardQueries.selectAll().executeAsList()

    fun add(
        accountId: Long,
        last4: String,
        network: String,
        creditLimitMinor: Long?,
        cutoffDay: Long?,
        dueDay: Long?,
        interestRate: Double? = null,
    ) = db.cardQueries.insert(accountId, last4, network, creditLimitMinor, cutoffDay, dueDay, interestRate)

    fun byAccount(accountId: Long): Card? = db.cardQueries.selectByAccount(accountId).executeAsList().firstOrNull()

    fun delete(id: Long) = db.cardQueries.delete(id)

    fun updateCreditTerms(id: Long, creditLimitMinor: Long?, cutoffDay: Long?, dueDay: Long?, interestRate: Double?) = db.cardQueries.updateCreditTerms(creditLimitMinor, cutoffDay, dueDay, interestRate, id)
}
