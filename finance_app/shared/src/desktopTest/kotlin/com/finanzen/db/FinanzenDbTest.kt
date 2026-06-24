package com.finanzen.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinanzenDbTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun insertaYLeeMoneda() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0)
        val usd = db.currencyQueries.selectByCode("USD").executeAsOne()
        assertEquals("USD", usd.code)
        assertEquals(2L, usd.decimals)
    }

    @Test
    fun seedIdempotente() {
        val db = freshDb()
        seedIfEmpty(db)
        val n1 = db.currencyQueries.selectAll().executeAsList().size
        seedIfEmpty(db)
        val n2 = db.currencyQueries.selectAll().executeAsList().size
        assertEquals(n1, n2)
        assertTrue(n1 >= 4)
    }

    @Test
    fun transaccionSeInsertaYRecupera() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        db.transactionQueries.insert(
            accountId = account.id,
            categoryId = expenseCat.id,
            amountMinor = 1234,
            currency = account.currency,
            date = 20000,
            note = "café",
            kind = "EXPENSE",
            transferAccountId = null,
            installmentPlanId = null,
        )

        val rows = db.transactionQueries.selectAll().executeAsList()
        assertEquals(1, rows.size)
        assertEquals(1234, rows[0].amountMinor)
        assertEquals("café", rows[0].note)
    }
}
