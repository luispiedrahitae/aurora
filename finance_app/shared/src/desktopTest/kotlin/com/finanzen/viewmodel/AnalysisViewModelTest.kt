package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.ui.format.monthPeriod
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun computeCashflowCubreSeisMesesConIngresoYGasto() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)

        // ingreso y gasto en el mes de referencia
        db.transactionQueries.insert(
            account.id, incomeCat.id, 5_000, account.currency, referenceMonth.toEpochDays().toLong(), "ingreso del mes", "INCOME", null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 2_000, account.currency, referenceMonth.toEpochDays().toLong(), "gasto del mes", "EXPENSE", null, null,
        )

        // ingreso 2 meses atrás (dentro de la ventana de 6 meses)
        val twoMonthsAgo = referenceMonth.minus(DatePeriod(months = 2))
        db.transactionQueries.insert(
            account.id, incomeCat.id, 3_000, account.currency, twoMonthsAgo.toEpochDays().toLong(), "ingreso viejo", "INCOME", null, null,
        )

        val cashflow = AnalysisViewModel.computeCashflow(
            txs = db.transactionQueries.selectAll().executeAsList(),
            referenceMonth = referenceMonth,
        )

        assertEquals(6, cashflow.size)
        assertEquals(5_000, cashflow.last().incomeMinor)
        assertEquals(2_000, cashflow.last().expenseMinor)
        // 2 meses atrás == penúltima-2 = índice size-3
        assertEquals(3_000, cashflow[cashflow.size - 3].incomeMinor)
    }

    @Test
    fun computeNetWorthAcumulaYExcluyeCredito() {
        val db = freshDb()
        // cuentas controladas (sin seedIfEmpty para no arrastrar saldos iniciales ajenos)
        db.accountQueries.insert("Efectivo", "CASH", "USD", 10_000, 0, 0)
        db.accountQueries.insert("Tarjeta", "CREDIT", "USD", 0, 0, 0)
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cash = accounts.first { it.type == "CASH" }
        val credit = accounts.first { it.type == "CREDIT" }
        db.categoryQueries.insert(null, "Sueldo", "", 0, "INCOME")
        db.categoryQueries.insert(null, "Compras", "", 0, "EXPENSE")
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)

        db.transactionQueries.insert(
            cash.id, incomeCat.id, 5_000, "USD", referenceMonth.toEpochDays().toLong(), "sueldo", "INCOME", null, null,
        )
        // gasto en tarjeta de crédito: NO debe reducir el patrimonio neto
        db.transactionQueries.insert(
            credit.id, expenseCat.id, 9_000, "USD", referenceMonth.toEpochDays().toLong(), "compra a credito", "EXPENSE", null, null,
        )

        val netWorth = AnalysisViewModel.computeNetWorth(
            accounts = accounts,
            txs = db.transactionQueries.selectAll().executeAsList(),
            referenceMonth = referenceMonth,
        )

        assertEquals(6, netWorth.size)
        // 10_000 (saldo inicial CASH) + 5_000 (ingreso), sin restar el gasto de crédito
        assertEquals(15_000, netWorth.last().netWorthMinor)
        // un mes anterior (antes del ingreso) tiene patrimonio menor que el mes de referencia
        assertTrue(netWorth.first().netWorthMinor < netWorth.last().netWorthMinor)
    }

    @Test
    fun computeFrequentExpensesOrdenaPorConteoNoPorMonto() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Café", "", 0, "EXPENSE")
        db.categoryQueries.insert(null, "Electrónica", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val catA = cats.first { it.name == "Café" }
        val catB = cats.first { it.name == "Electrónica" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)

        // categoría A: 3 gastos de 100 (conteo alto, monto bajo)
        repeat(3) {
            db.transactionQueries.insert(
                account.id, catA.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "cafe", "EXPENSE", null, null,
            )
        }
        // categoría B: 1 gasto de 10_000 (conteo bajo, monto alto)
        db.transactionQueries.insert(
            account.id, catB.id, 10_000, "USD", referenceMonth.toEpochDays().toLong(), "tv", "EXPENSE", null, null,
        )

        val frequent = AnalysisViewModel.computeFrequentExpenses(
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
            period = monthPeriod(referenceMonth),
        )

        // ordena por conteo, no por monto: A (3 veces) va primero pese a que B es más caro
        assertEquals("Café", frequent.first().name)
        assertEquals(3, frequent.first().count)
        assertEquals(300, frequent.first().amountMinor)
    }
}
