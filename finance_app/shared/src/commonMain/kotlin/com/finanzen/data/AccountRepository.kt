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

class AccountRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<Account>> = db.accountQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun all(): List<Account> = db.accountQueries.selectAll().executeAsList()

    fun add(name: String, type: String, currency: String, openingBalanceMinor: Long = 0): Long = db.transactionWithResult {
        db.accountQueries.insert(name, type, currency, openingBalanceMinor, color = 0, archived = 0)
        db.accountQueries.lastInsertRowId().executeAsOne()
    }

    fun delete(id: Long) = db.accountQueries.delete(id)

    fun ensureAny(): Account = all().firstOrNull()
        ?: add("Efectivo", "CASH", "USD").let { id -> all().first { it.id == id } }

    fun observeArchived(): Flow<List<Account>> = db.accountQueries.selectArchived().asFlow().mapToList(Dispatchers.Default)

    fun observeAllIncludingArchived(): Flow<List<Account>> = db.accountQueries.selectAllAny().asFlow().mapToList(Dispatchers.Default)

    fun allIncludingArchived(): List<Account> = db.accountQueries.selectAllAny().executeAsList()

    fun archive(id: Long) = db.accountQueries.archive(id)
    fun unarchive(id: Long) = db.accountQueries.unarchive(id)
    fun updateBasics(id: Long, name: String, openingBalanceMinor: Long) = db.accountQueries.updateBasics(name, openingBalanceMinor, id)
}
