package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.AccountRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.NotificationScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubscriptionsViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    private fun buildVm(db: FinanzenDb) = SubscriptionsViewModel(
        SubscriptionRepository(db),
        AccountRepository(db),
        TransactionRepository(db),
        SettingsRepository(db),
        NotificationScheduler(),
    )

    private fun todayEpochDay(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()

    @Test
    fun addSubscriptionConFechaFuturaNoCobraDeInmediato() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountId = AccountRepository(db).add("Efectivo", "CASH", "USD")
        val future = todayEpochDay() + 10

        buildVm(db).addSubscription("Streaming", 5_000, "DAILY", 30, accountId, future)

        assertTrue(db.transactionQueries.selectAll().executeAsList().isEmpty())
        val sub = db.subscriptionQueries.selectActive().executeAsList().first()
        assertEquals(future, sub.nextChargeDate)
    }

    @Test
    fun addSubscriptionConFechaPasadaPoneAlDiaLosCobrosAtrasados() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountId = AccountRepository(db).add("Efectivo", "CASH", "USD")
        val past = todayEpochDay() - 5

        buildVm(db).addSubscription("Streaming", 5_000, "DAILY", 1, accountId, past)

        val txs = db.transactionQueries.selectAll().executeAsList()
        assertTrue(txs.isNotEmpty())
        assertTrue(txs.all { it.subscriptionId != null && it.kind == "EXPENSE" })
        val sub = db.subscriptionQueries.selectActive().executeAsList().first()
        assertTrue(sub.nextChargeDate > todayEpochDay())
    }

    @Test
    fun addSubscriptionUsaLaCuentaElegidaPorElUsuario() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountRepo = AccountRepository(db)
        accountRepo.add("Efectivo", "CASH", "USD")
        val creditId = accountRepo.add("Tarjeta", "CREDIT", "USD")

        buildVm(db).addSubscription("Streaming", 5_000, "DAILY", 30, creditId, todayEpochDay())

        val sub = db.subscriptionQueries.selectActive().executeAsList().first()
        assertEquals(creditId, sub.accountId)
    }
}
