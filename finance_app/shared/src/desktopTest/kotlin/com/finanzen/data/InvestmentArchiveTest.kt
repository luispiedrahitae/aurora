package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvestmentArchiveTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    private fun addFund(db: FinanzenDb, name: String): Long {
        val account = db.accountQueries.selectAll().executeAsList().first()
        val invCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Inversión" }
        val id = InvestmentRepository(db).add(name, 100_000, account.currency, account.id, invCat.id, false, null, null, null, 0)
        TransactionRepository(db).add(account.id, invCat.id, 100_000, account.currency, 0, name, "EXPENSE", investmentId = id)
        return id
    }

    @Test
    fun archivarYRestaurarDevuelveCadaFondoASuEstadoPrevio() {
        val db = freshDb()
        seedIfEmpty(db)
        val repo = InvestmentRepository(db)
        val open = addFund(db, "Abierto")
        val closed = addFund(db, "Cerrado")
        repo.close(closed, withdrawnAmountMinor = 110_000, closedDate = 10, yieldMinor = 10_000)

        repo.archive(open)
        repo.archive(closed)
        assertEquals(listOf("ARCHIVED", "ARCHIVED"), listOf(repo.byId(open)!!.status, repo.byId(closed)!!.status))
        // Archivadas siguen en all(): backup y reportes no deben perderlas.
        assertEquals(2, repo.all().size)

        repo.unarchive(open)
        repo.unarchive(closed)
        assertEquals("OPEN", repo.byId(open)!!.status)
        assertEquals("CLOSED", repo.byId(closed)!!.status)
    }

    @Test
    fun borrarDefinitivoEliminaElFondoYSusMovimientos() {
        val db = freshDb()
        seedIfEmpty(db)
        val repo = InvestmentRepository(db)
        val txRepo = TransactionRepository(db)
        val keep = addFund(db, "Otro")
        val gone = addFund(db, "Borrar")

        // Mismo orden que InvestmentsViewModel.deletePermanently.
        txRepo.deleteByInvestment(gone)
        repo.delete(gone)

        assertNull(repo.byId(gone))
        assertTrue(txRepo.contributionsForInvestment(gone).isEmpty())
        assertEquals(1, txRepo.contributionsForInvestment(keep).size)
    }
}
