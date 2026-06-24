package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import com.finanzen.viewmodel.BudgetsViewModel
import com.finanzen.viewmodel.CardsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Phase4ManagementTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun categoriaSeAgregaYElimina() {
        val db = freshDb()
        val repo = CategoryRepository(db)
        repo.add(name = "Mascotas", kind = "EXPENSE", parentId = null)
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Mascotas" }
        assertEquals("EXPENSE", cat.kind)

        repo.delete(cat.id)
        assertNull(db.categoryQueries.selectById(cat.id).executeAsOneOrNull())
    }

    @Test
    fun transaccionSeActualiza() {
        val db = freshDb()
        seedIfEmpty(db)
        val tx = TransactionRepository(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        tx.add(account.id, null, 1000, account.currency, 20000, "café", "EXPENSE")
        val id = db.transactionQueries.selectAll().executeAsList().first().id

        tx.update(id, account.id, null, 2500, account.currency, 20000, "café grande", "EXPENSE")
        val updated = db.transactionQueries.selectById(id).executeAsOne()
        assertEquals(2500, updated.amountMinor)
        assertEquals("café grande", updated.note)
    }

    @Test
    fun borrarTarjetaTambienBorraSuCuentaHuerfana() {
        val db = freshDb()
        seedIfEmpty(db)
        val vm = CardsViewModel(CardRepository(db), InstallmentPlanRepository(db), AccountRepository(db))
        val accountsBefore = db.accountQueries.selectAll().executeAsList().size

        vm.addSample() // crea una cuenta CREDIT + su tarjeta
        assertEquals(accountsBefore + 1, db.accountQueries.selectAll().executeAsList().size)
        val card = db.cardQueries.selectAll().executeAsList().first()

        vm.deleteCard(card.id)
        assertNull(db.cardQueries.selectById(card.id).executeAsOneOrNull())
        // la cuenta de respaldo ya no debe quedar (antes aparecía fantasma en el selector)
        assertEquals(accountsBefore, db.accountQueries.selectAll().executeAsList().size)
    }

    @Test
    fun computeBudgetsCalculaGastoYLimiteDelMes() {
        val db = freshDb()
        seedIfEmpty(db)
        val budgetRepo = BudgetRepository(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val period = BudgetsViewModel.currentPeriodMonth()
        budgetRepo.setLimit(cat.id, period, 50_000)

        // gasto en el mes actual (epochDay derivado del periodo no es trivial; usamos hoy)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()
        TransactionRepository(db).add(account.id, cat.id, 12_000, account.currency, today, "compra", "EXPENSE")

        val data = BudgetsViewModel.computeBudgets(
            budgets = db.budgetQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            period = period,
        )
        val row = data.rows.first { it.categoryId == cat.id }
        assertEquals(50_000, row.limitMinor)
        assertEquals(12_000, row.spentMinor)
        assertTrue(data.rows.all { it.spentMinor >= 0 })
    }
}
