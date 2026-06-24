package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun agregaBalanceGastosYTopCategoriasDelMes() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        db.transactionQueries.insert(
            account.id, incomeCat.id, 10_000, account.currency, todayEpoch, "salario", "INCOME", null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 3_000, account.currency, todayEpoch, "café", "EXPENSE", null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            txs = db.transactionQueries.selectAll().executeAsList(),
            accounts = db.accountQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // opening 0 + 10000 ingreso - 3000 gasto
        assertEquals(7_000, data.totalBalanceMinor)
        assertEquals(10_000, data.monthIncomeMinor)
        assertEquals(3_000, data.monthExpenseMinor)
        assertEquals(1, data.topCategories.size)
        assertEquals(3_000, data.topCategories.first().amountMinor)
        assertEquals(6, data.cashflow.size)
        // el mes en curso (último del cashflow) tiene neto +7000
        assertEquals(7_000, data.cashflow.last().netMinor)
    }
}
