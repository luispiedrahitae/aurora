package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountRepositoryTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun updateBasicsCambiaNombreYMontoInicialSinTocarElTipo() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val repo = AccountRepository(db)
        val id = repo.add(name = "Efectivo", type = "CASH", currency = "USD", openingBalanceMinor = 500)

        repo.updateBasics(id, "Billetera", 1000)

        val account = repo.all().single { it.id == id }
        assertEquals("Billetera", account.name)
        assertEquals("CASH", account.type)
        assertEquals(1000, account.openingBalanceMinor)
    }
}
