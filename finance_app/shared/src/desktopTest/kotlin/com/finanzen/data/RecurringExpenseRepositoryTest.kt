package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals

class RecurringExpenseRepositoryTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun addDevuelveElIdDeLaFilaNuevaAunqueNoOrdeneUltimaPorFecha() {
        // FIX (mismo hallazgo que AccountRepositoryTest): add() resolvía el id nuevo con
        // selectAll().last(), pero selectAll ordena por nextChargeDate -- si la fila nueva no
        // ordena última por fecha, .last() devolvía el id de OTRA fila ya existente.
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val repo = RecurringExpenseRepository(db)
        repo.add(
            name = "Gimnasio",
            amountMinor = 5_000,
            currency = "USD",
            categoryId = null,
            accountId = null,
            frequency = "MONTHLY",
            intervalCount = 1,
            nextChargeDateEpochDay = 200,
            remindDaysBefore = 1,
        )
        val newId = repo.add(
            name = "Seguro",
            amountMinor = 8_000,
            currency = "USD",
            categoryId = null,
            accountId = null,
            frequency = "MONTHLY",
            intervalCount = 1,
            nextChargeDateEpochDay = 50,
            remindDaysBefore = 1,
        )

        val recurring = db.recurringExpenseQueries.selectById(newId).executeAsOne()
        assertEquals("Seguro", recurring.name)
        assertEquals(8_000, recurring.amountMinor)
    }
}
