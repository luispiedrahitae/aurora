package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.InvestmentRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.NotificationScheduler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TransactionsViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    private fun buildVm(db: FinanzenDb) = TransactionsViewModel(
        TransactionRepository(db),
        AccountRepository(db),
        CategoryRepository(db),
        CardRepository(db),
        InstallmentPlanRepository(db),
        BudgetRepository(db),
        SettingsRepository(db),
        NotificationScheduler(),
        InvestmentRepository(db),
        SubscriptionRepository(db),
    )

    @Test
    fun movimientoSueltoNoSeBloquea() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        TransactionRepository(db).add(account.id, null, 1000, account.currency, 20000, "café", "EXPENSE")
        val id = db.transactionQueries.selectAll().executeAsList().first { it.note == "café" }.id

        assertNull(buildVm(db).deleteBlockReason(id))
    }

    @Test
    fun movimientoDeSuscripcionActivaSeBloqueaYSeDesbloqueaAlBorrarla() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val subsRepo = SubscriptionRepository(db)
        val subId = subsRepo.add(
            name = "Netflix",
            amountMinor = 5000,
            currency = account.currency,
            categoryId = null,
            accountId = account.id,
            frequency = "MONTHLY",
            intervalCount = 1,
            nextChargeDateEpochDay = 20000,
            remindDaysBefore = 1,
        )
        TransactionRepository(db).add(account.id, null, 5000, account.currency, 20000, "Netflix", "EXPENSE", subscriptionId = subId)
        val txId = db.transactionQueries.selectAll().executeAsList().first { it.note == "Netflix" }.id

        val vm = buildVm(db)
        val block = vm.deleteBlockReason(txId)
        assertIs<TransactionsViewModel.DeleteBlock.Subscription>(block)
        assertEquals("Netflix", block.name)

        subsRepo.delete(subId)
        assertNull(vm.deleteBlockReason(txId))
    }

    @Test
    fun movimientoDeInversionPeriodicaSeBloquea() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val investId = InvestmentRepository(db).add(
            name = "Fondo Mensual",
            amountMinor = 100_000,
            currency = account.currency,
            accountId = account.id,
            categoryId = null,
            periodic = true,
            frequency = "MONTHLY",
            intervalCount = 1,
            nextContributionDate = 20030,
            startDate = 20000,
        )
        TransactionRepository(db).add(account.id, null, 100_000, account.currency, 20000, "Fondo Mensual", "EXPENSE", investmentId = investId)
        val txId = db.transactionQueries.selectAll().executeAsList().first { it.note == "Fondo Mensual" }.id

        val block = buildVm(db).deleteBlockReason(txId)
        assertIs<TransactionsViewModel.DeleteBlock.Investment>(block)
        assertEquals("Fondo Mensual", block.name)
    }

    @Test
    fun movimientoDeInversionUnicaNoSeBloquea() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val investId = InvestmentRepository(db).add(
            name = "Acciones Dinamico",
            amountMinor = 300_000,
            currency = account.currency,
            accountId = account.id,
            categoryId = null,
            periodic = false,
            frequency = null,
            intervalCount = null,
            nextContributionDate = null,
            startDate = 20000,
        )
        TransactionRepository(db).add(account.id, null, 300_000, account.currency, 20000, "Acciones Dinamico", "EXPENSE", investmentId = investId)
        val txId = db.transactionQueries.selectAll().executeAsList().first { it.note == "Acciones Dinamico" }.id

        assertNull(buildVm(db).deleteBlockReason(txId))
    }
}
