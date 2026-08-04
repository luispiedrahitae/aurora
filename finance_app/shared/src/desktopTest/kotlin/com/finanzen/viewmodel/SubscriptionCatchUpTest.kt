package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.AccountRepository
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

class SubscriptionCatchUpTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    private fun todayEpochDay(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()

    private fun anyCategoryId(db: FinanzenDb): Long {
        db.categoryQueries.insert(parentId = null, name = "Suscripciones", icon = "", color = 0, kind = "EXPENSE")
        val parentId = db.categoryQueries.lastInsertRowId().executeAsOne()
        db.categoryQueries.insert(parentId = parentId, name = "Suscripción", icon = "", color = 0, kind = "EXPENSE")
        return db.categoryQueries.lastInsertRowId().executeAsOne()
    }

    @Test
    fun runConVariasSuscripcionesVencidasCobraCadaUnaEnLaSuscripcionCorrecta() {
        // FIX (mismo hallazgo que AccountRepositoryTest): SubscriptionRepository.add() resolvía el
        // id nuevo con selectAll().last(), pero selectAll ordena por nextChargeDate -- al crear una
        // suscripción vencida mientras ya existe otra con fecha MÁS futura, .last() devolvía el id
        // de esa OTRA suscripción. SubscriptionsViewModelTest no lo detectaba porque sus casos solo
        // crean una suscripción a la vez.
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountId = AccountRepository(db).add("Efectivo", "CASH", "USD")
        val subsRepo = SubscriptionRepository(db)
        val txRepo = TransactionRepository(db)
        val catchUp = SubscriptionCatchUp(subsRepo, AccountRepository(db), txRepo, NotificationScheduler())
        val categoryId = anyCategoryId(db)

        subsRepo.add(
            name = "Futura", amountMinor = 1_000, currency = "USD", categoryId = categoryId, accountId = accountId,
            frequency = "DAILY", intervalCount = 30, nextChargeDateEpochDay = todayEpochDay() + 100, remindDaysBefore = 3,
        )
        val vencidaId = subsRepo.add(
            name = "Vencida", amountMinor = 2_000, currency = "USD", categoryId = categoryId, accountId = accountId,
            frequency = "DAILY", intervalCount = 1, nextChargeDateEpochDay = todayEpochDay() - 5, remindDaysBefore = 3,
        )
        assertEquals("Vencida", subsRepo.byId(vencidaId)?.name) // el id ya apunta a la fila correcta

        catchUp.run()

        val txs = txRepo.all()
        assertTrue(txs.isNotEmpty())
        assertTrue(txs.all { it.subscriptionId == vencidaId }) // "Futura" no se tocó
        val futura = subsRepo.activeNow().single { it.name == "Futura" }
        assertEquals(todayEpochDay() + 100, futura.nextChargeDate)
    }

    @Test
    fun runDosVecesSeguidasNoDuplicaLosCobros() {
        // El catch-up corre tanto al arranque de la app (AppModule.kt) como al abrir Suscripciones
        // (SubscriptionsViewModel.init) -- debe ser idempotente.
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountId = AccountRepository(db).add("Efectivo", "CASH", "USD")
        val subsRepo = SubscriptionRepository(db)
        val txRepo = TransactionRepository(db)
        val catchUp = SubscriptionCatchUp(subsRepo, AccountRepository(db), txRepo, NotificationScheduler())
        subsRepo.add(
            name = "Streaming", amountMinor = 1_500, currency = "USD", categoryId = anyCategoryId(db), accountId = accountId,
            frequency = "DAILY", intervalCount = 1, nextChargeDateEpochDay = todayEpochDay() - 3, remindDaysBefore = 3,
        )

        catchUp.run()
        val txsTrasPrimeraCorrida = txRepo.all().size
        catchUp.run()

        assertEquals(txsTrasPrimeraCorrida, txRepo.all().size)
    }
}
