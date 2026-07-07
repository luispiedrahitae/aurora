package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals

class CardRepositoryTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun updateCreditTermsCambiaCupoYDejaElRestoIntacto() {
        val db = freshDb()
        db.currencyQueries.upsert("USD", "$", 2, 1.0, "US Dollar", ".", ",")
        val accountRepo = AccountRepository(db)
        val cardRepo = CardRepository(db)
        val accountId = accountRepo.add(name = "Tarjeta", type = "CREDIT", currency = "USD")
        cardRepo.add(accountId = accountId, last4 = "", network = "OTRA", creditLimitMinor = 100000, cutoffDay = 5, dueDay = 20, interestRate = 2.5)
        val cardId = cardRepo.byAccount(accountId)!!.id

        cardRepo.updateCreditTerms(cardId, creditLimitMinor = 200000, cutoffDay = 10, dueDay = 25, interestRate = 3.0)

        val card = cardRepo.byAccount(accountId)!!
        assertEquals(200000, card.creditLimitMinor)
        assertEquals(10, card.cutoffDay)
        assertEquals(25, card.dueDay)
        assertEquals(3.0, card.interestRate)
        assertEquals(accountId, card.accountId)
        assertEquals("OTRA", card.network)
    }
}
