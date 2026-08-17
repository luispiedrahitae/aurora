package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.BudgetRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.ui.format.PeriodMode
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportsViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun modoPorDefectoEsMensualConMesYAnioActual() {
        val db = freshDb()
        seedIfEmpty(db)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // Solo se ejercitan las funciones puras (companion): construir el ViewModel completo
        // requiere ReportExporter/InvestmentRepository, que son detalle de plataforma/IO.
        val filteredThisMonth = ReportsViewModel.filterByPeriod(
            db.transactionQueries.selectAll().executeAsList(),
            PeriodMode.MONTH,
            LocalDate(today.year, today.month, 1),
            today.year,
        )
        assertEquals(emptyList(), filteredThisMonth) // sin movimientos sembrados aún
    }

    @Test
    fun filterByPeriodFiltraPorMesYPorAnio() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val txRepo = TransactionRepository(db)

        val enero2026 = LocalDate(2026, 1, 15).toEpochDays().toLong()
        val febrero2026 = LocalDate(2026, 2, 10).toEpochDays().toLong()
        val enero2025 = LocalDate(2025, 1, 5).toEpochDays().toLong()
        txRepo.add(account.id, cat.id, 1_000, account.currency, enero2026, "ene26", "EXPENSE")
        txRepo.add(account.id, cat.id, 2_000, account.currency, febrero2026, "feb26", "EXPENSE")
        txRepo.add(account.id, cat.id, 3_000, account.currency, enero2025, "ene25", "EXPENSE")

        val allTxs = db.transactionQueries.selectAll().executeAsList()

        val byMonth = ReportsViewModel.filterByPeriod(allTxs, PeriodMode.MONTH, LocalDate(2026, 1, 1), 2026)
        assertEquals(listOf("ene26"), byMonth.map { it.note })

        val byYear = ReportsViewModel.filterByPeriod(allTxs, PeriodMode.YEAR, LocalDate(2026, 1, 1), 2026)
        assertEquals(setOf("ene26", "feb26"), byYear.map { it.note }.toSet())
    }

    @Test
    fun aggregateBudgetsEnModoAnualSumaLosDoceMeses() {
        val db = freshDb()
        seedIfEmpty(db)
        val budgetRepo = BudgetRepository(db)
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        // Límite de 10_000 vigente desde enero: se hereda en los 12 meses del año.
        budgetRepo.setLimit(cat.id, 202601L, 10_000)

        val txRepo = TransactionRepository(db)
        txRepo.add(
            db.accountQueries.selectAll().executeAsList().first().id,
            cat.id,
            4_000,
            "USD",
            LocalDate(2026, 3, 10).toEpochDays().toLong(),
            "gasto marzo",
            "EXPENSE",
        )

        val rows = ReportsViewModel.aggregateBudgets(
            budgets = db.budgetQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            allTxs = db.transactionQueries.selectAll().executeAsList(),
            mode = PeriodMode.YEAR,
            month = LocalDate(2026, 1, 1),
            year = 2026,
        )

        val row = rows.first { it.categoryName == cat.name }
        assertEquals(10_000L * 12, row.limitMinor) // el límite se hereda en los 12 meses
        assertEquals(4_000L, row.spentMinor)
    }

    @Test
    fun aggregateBudgetsEnModoMensualUsaSoloElMesElegido() {
        val db = freshDb()
        seedIfEmpty(db)
        val budgetRepo = BudgetRepository(db)
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        budgetRepo.setLimit(cat.id, 202603L, 5_000)

        val rows = ReportsViewModel.aggregateBudgets(
            budgets = db.budgetQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            allTxs = emptyList(),
            mode = PeriodMode.MONTH,
            month = LocalDate(2026, 3, 1),
            year = 2026,
        )

        assertEquals(5_000L, rows.first { it.categoryName == cat.name }.limitMinor)
    }
}
