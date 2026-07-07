package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalysisViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun computeYearlyExpensesCubreDoceMesesYExcluyeLoQueQuedaFueraDeLaVentana() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)

        // dentro de la ventana de 12 meses
        val threeMonthsAgo = referenceMonth.minus(DatePeriod(months = 3))
        db.transactionQueries.insert(
            account.id, expenseCat.id, 4_000, account.currency, threeMonthsAgo.toEpochDays().toLong(), "gasto reciente", "EXPENSE", null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 2_000, account.currency, referenceMonth.toEpochDays().toLong(), "gasto del mes", "EXPENSE", null, null,
        )

        // fuera de la ventana (13 meses atrás)
        val thirteenMonthsAgo = referenceMonth.minus(DatePeriod(months = 13))
        db.transactionQueries.insert(
            account.id, expenseCat.id, 9_000, account.currency, thirteenMonthsAgo.toEpochDays().toLong(), "gasto viejo", "EXPENSE", null, null,
        )

        val yearly = AnalysisViewModel.computeYearlyExpenses(
            txs = db.transactionQueries.selectAll().executeAsList(),
            referenceMonth = referenceMonth,
        )

        assertEquals(12, yearly.size)
        // el gasto de 13 meses atrás no debe reflejarse en ninguna barra de la ventana.
        assertEquals(6_000, yearly.sumOf { it.expenseMinor })
        assertEquals(2_000, yearly.last().expenseMinor)
        assertEquals(4_000, yearly[yearly.size - 4].expenseMinor)
    }
}
