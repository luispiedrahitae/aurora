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

class TransactionRepository(private val db: FinanzenDb) {
    fun observeAll(): Flow<List<TransactionRow>> = db.transactionQueries.selectAll().asFlow().mapToList(Dispatchers.Default)

    fun add(
        accountId: Long,
        categoryId: Long,
        amountMinor: Long,
        currency: String,
        epochDay: Long,
        note: String,
        kind: String,
        installmentPlanId: Long? = null,
        investmentId: Long? = null,
        subscriptionId: Long? = null,
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
        investmentId = investmentId,
        subscriptionId = subscriptionId,
    )

    fun update(
        id: Long,
        accountId: Long,
        categoryId: Long,
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
        investmentId = null,
        subscriptionId = null,
        id = id,
    )

    /** Transferencia: una fila kind=TRANSFER, origen=accountId, destino=transferAccountId. Categoría
     * de sistema "Transferencias/Transferencia entre cuentas" (ver DefaultSeed.kt) — no es un
     * gasto/ingreso real, pero categoryId sigue siendo obligatorio en el esquema. */
    fun addTransfer(fromAccountId: Long, toAccountId: Long, amountMinor: Long, currency: String, epochDay: Long, note: String) = db.transactionQueries.insert(
        accountId = fromAccountId,
        categoryId = systemCategoryLeaf(db, "TRANSFER", "Transferencias", "Transferencia entre cuentas"),
        amountMinor = amountMinor,
        currency = currency,
        date = epochDay,
        note = note,
        kind = "TRANSFER",
        transferAccountId = toAccountId,
        installmentPlanId = null,
        investmentId = null,
        subscriptionId = null,
    )

    fun updateTransfer(id: Long, fromAccountId: Long, toAccountId: Long, amountMinor: Long, currency: String, epochDay: Long, note: String) = db.transactionQueries.update(
        accountId = fromAccountId,
        categoryId = systemCategoryLeaf(db, "TRANSFER", "Transferencias", "Transferencia entre cuentas"),
        amountMinor = amountMinor,
        currency = currency,
        date = epochDay,
        note = note,
        kind = "TRANSFER",
        transferAccountId = toAccountId,
        installmentPlanId = null,
        investmentId = null,
        subscriptionId = null,
        id = id,
    )

    /** Ajuste de saldo (creación de cuenta o edición de monto). Categoría de sistema
     * "Ajustes/Ajuste de saldo" (ver DefaultSeed.kt) — no es un gasto/ingreso real. */
    fun addAdjustment(accountId: Long, amountMinor: Long, currency: String, epochDay: Long, note: String) = db.transactionQueries.insert(
        accountId = accountId,
        categoryId = systemCategoryLeaf(db, "ADJUSTMENT", "Ajustes", "Ajuste de saldo"),
        amountMinor = amountMinor,
        currency = currency,
        date = epochDay,
        note = note,
        kind = "ADJUSTMENT",
        transferAccountId = null,
        installmentPlanId = null,
        investmentId = null,
        subscriptionId = null,
    )

    fun byId(id: Long): TransactionRow? = db.transactionQueries.selectById(id).executeAsOneOrNull()

    fun all(): List<TransactionRow> = db.transactionQueries.selectAll().executeAsList()

    fun delete(id: Long) = db.transactionQueries.delete(id)

    fun deleteByAccount(accountId: Long) = db.transactionQueries.deleteByAccount(accountId)

    fun deleteByInvestment(investmentId: Long) = db.transactionQueries.deleteByInvestment(investmentId)

    fun countForPlan(planId: Long): Long = db.transactionQueries.countByInstallmentPlan(planId).executeAsOne()

    fun contributionsForInvestment(investmentId: Long): List<TransactionRow> = db.transactionQueries.selectByInvestment(investmentId).executeAsList()

    fun contributionsForSubscription(subscriptionId: Long): List<TransactionRow> = db.transactionQueries.selectBySubscription(subscriptionId).executeAsList()
}
