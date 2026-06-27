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
import com.finanzen.db.RecurringExpense
import com.finanzen.db.Subscription
import com.finanzen.db.TransactionRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<TransactionRow>> = db.transactionQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun add(
        accountId: Long,
        categoryId: Long?,
        amountMinor: Long,
        currency: String,
        epochDay: Long,
        note: String,
        kind: String,
        installmentPlanId: Long? = null,
    ) = db.transactionQueries.insert(
        accountId = accountId,
        categoryId = categoryId,
        amountMinor = amountMinor,
        currency = currency,
        date = epochDay,
        note = note,
        kind = kind,
        transferAccountId = null,
        installmentPlanId = installmentPlanId,
    )

    fun update(
        id: Long,
        accountId: Long,
        categoryId: Long?,
        amountMinor: Long,
        currency: String,
        epochDay: Long,
        note: String,
        kind: String,
    ) = db.transactionQueries.update(
        accountId = accountId,
        categoryId = categoryId,
        amountMinor = amountMinor,
        currency = currency,
        date = epochDay,
        note = note,
        kind = kind,
        transferAccountId = null,
        installmentPlanId = null,
        id = id,
    )

    /** Transferencia: una fila kind=TRANSFER, origen=accountId, destino=transferAccountId. Sin categoría. */
    fun addTransfer(fromAccountId: Long, toAccountId: Long, amountMinor: Long, currency: String, epochDay: Long, note: String) = db.transactionQueries.insert(
        accountId = fromAccountId,
        categoryId = null,
        amountMinor = amountMinor,
        currency = currency,
        date = epochDay,
        note = note,
        kind = "TRANSFER",
        transferAccountId = toAccountId,
        installmentPlanId = null,
    )

    fun updateTransfer(id: Long, fromAccountId: Long, toAccountId: Long, amountMinor: Long, currency: String, epochDay: Long, note: String) = db.transactionQueries.update(
        accountId = fromAccountId,
        categoryId = null,
        amountMinor = amountMinor,
        currency = currency,
        date = epochDay,
        note = note,
        kind = "TRANSFER",
        transferAccountId = toAccountId,
        installmentPlanId = null,
        id = id,
    )

    fun byId(id: Long): TransactionRow? = db.transactionQueries.selectById(id).executeAsOneOrNull()

    fun all(): List<TransactionRow> = db.transactionQueries.selectAll().executeAsList()

    fun delete(id: Long) = db.transactionQueries.delete(id)
}

class AccountRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<Account>> = db.accountQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun all(): List<Account> = db.accountQueries.selectAll().executeAsList()

    fun add(name: String, type: String, currency: String, openingBalanceMinor: Long = 0): Long {
        db.accountQueries.insert(name, type, currency, openingBalanceMinor, color = 0, archived = 0)
        return db.accountQueries.selectAll().executeAsList().last().id
    }

    fun delete(id: Long) = db.accountQueries.delete(id)
}

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
}

class InstallmentPlanRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<InstallmentPlan>> = db.installmentPlanQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun add(
        cardId: Long,
        totalAmountMinor: Long,
        installments: Long,
        interestRate: Double,
        startDateEpochDay: Long,
        description: String,
    ): Long = db.transactionWithResult {
        db.installmentPlanQueries.insert(cardId, totalAmountMinor, installments, interestRate, startDateEpochDay, description)
        db.installmentPlanQueries.lastInsertRowId().executeAsOne()
    }

    fun delete(id: Long) = db.installmentPlanQueries.delete(id)
}

class SubscriptionRepository(private val db: FinanzenDb) {
    fun observeActive(): Flow<List<Subscription>> = db.subscriptionQueries.selectActive().asFlow().mapToList(Dispatchers.Default)

    fun add(
        name: String,
        amountMinor: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long?,
        frequency: String,
        intervalCount: Long,
        nextChargeDateEpochDay: Long,
        remindDaysBefore: Long,
    ): Long {
        db.subscriptionQueries.insert(
            name, amountMinor, currency, categoryId, accountId,
            frequency, intervalCount, nextChargeDateEpochDay, remindDaysBefore, active = 1,
        )
        return db.subscriptionQueries.selectAll().executeAsList().last().id
    }

    fun delete(id: Long) = db.subscriptionQueries.delete(id)
}

class RecurringExpenseRepository(private val db: FinanzenDb) {
    fun observeActive(): Flow<List<RecurringExpense>> = db.recurringExpenseQueries.selectActive().asFlow().mapToList(Dispatchers.Default)

    fun add(
        name: String,
        amountMinor: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long?,
        frequency: String,
        intervalCount: Long,
        nextChargeDateEpochDay: Long,
        remindDaysBefore: Long,
    ): Long {
        db.recurringExpenseQueries.insert(
            name, amountMinor, currency, categoryId, accountId,
            frequency, intervalCount, nextChargeDateEpochDay, remindDaysBefore, active = 1,
        )
        return db.recurringExpenseQueries.selectAll().executeAsList().last().id
    }

    fun delete(id: Long) = db.recurringExpenseQueries.delete(id)
}

class CategoryRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<Category>> = db.categoryQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun byKind(kind: String): List<Category> = db.categoryQueries.selectByKind(kind).executeAsList()

    fun add(name: String, kind: String, parentId: Long?, icon: String = "", color: Long = 0) = db.categoryQueries.insert(parentId = parentId, name = name, icon = icon, color = color, kind = kind)

    fun update(id: Long, name: String, kind: String, parentId: Long?, icon: String = "", color: Long = 0) = db.categoryQueries.update(parentId = parentId, name = name, icon = icon, color = color, kind = kind, id = id)

    fun delete(id: Long) = db.categoryQueries.delete(id)
}

class CurrencyRepository(private val db: FinanzenDb) {
    fun all(): List<Currency> = db.currencyQueries.selectAll().executeAsList()
}

class BudgetRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<Budget>> = db.budgetQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun setLimit(categoryId: Long, periodMonth: Long, limitMinor: Long) = db.budgetQueries.upsert(categoryId = categoryId, periodMonth = periodMonth, limitMinor = limitMinor)

    /** Límite de una categoría en un periodo (0 si no tiene presupuesto). */
    fun limitFor(categoryId: Long, periodMonth: Long): Long = db.budgetQueries.selectByPeriod(periodMonth).executeAsList().firstOrNull { it.categoryId == categoryId }?.limitMinor ?: 0L

    fun delete(id: Long) = db.budgetQueries.delete(id)
}
