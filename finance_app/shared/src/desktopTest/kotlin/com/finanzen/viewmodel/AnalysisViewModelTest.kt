package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.ui.format.monthPeriod
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
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
    fun computeCashflowCubreTodosLosDiasDelMesConIngresoYGastoPorDia() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)
        val expectedDays = referenceMonth.plus(DatePeriod(months = 1)).toEpochDays() - referenceMonth.toEpochDays()

        // día 1 del mes: ingreso y gasto
        db.transactionQueries.insert(
            account.id, incomeCat.id, 5_000, account.currency, referenceMonth.toEpochDays().toLong(), "ingreso día 1", "INCOME", null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 2_000, account.currency, referenceMonth.toEpochDays().toLong(), "gasto día 1", "EXPENSE", null, null,
        )

        // día 15 del mes: otro ingreso, para distinguir por día (no por mes)
        val day15 = referenceMonth.plus(DatePeriod(days = 14))
        db.transactionQueries.insert(
            account.id, incomeCat.id, 3_000, account.currency, day15.toEpochDays().toLong(), "ingreso día 15", "INCOME", null, null,
        )

        val cashflow = AnalysisViewModel.computeCashflow(
            txs = db.transactionQueries.selectAll().executeAsList(),
            referenceMonth = referenceMonth,
        )

        assertEquals(expectedDays, cashflow.size)
        assertEquals(5_000, cashflow.first().incomeMinor)
        assertEquals(2_000, cashflow.first().expenseMinor)
        assertEquals(3_000, cashflow[14].incomeMinor)
        assertEquals(0, cashflow[14].expenseMinor)
    }

    @Test
    fun computeNetWorthAcumulaPorDiaYExcluyeCredito() {
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
        val day10 = referenceMonth.plus(DatePeriod(days = 9))

        db.transactionQueries.insert(
            cash.id, incomeCat.id, 5_000, "USD", day10.toEpochDays().toLong(), "sueldo", "INCOME", null, null,
        )
        // gasto en tarjeta de crédito: NO debe reducir el patrimonio neto
        db.transactionQueries.insert(
            credit.id, expenseCat.id, 9_000, "USD", day10.toEpochDays().toLong(), "compra a credito", "EXPENSE", null, null,
        )

        val netWorth = AnalysisViewModel.computeNetWorth(
            accounts = accounts,
            txs = db.transactionQueries.selectAll().executeAsList(),
            referenceMonth = referenceMonth,
        )

        // antes del día 10: solo el saldo inicial de CASH
        assertEquals(10_000, netWorth[0].netWorthMinor)
        // desde el día 10 en adelante: + 5_000 (ingreso), sin restar el gasto de crédito
        assertEquals(15_000, netWorth[9].netWorthMinor)
        assertEquals(15_000, netWorth.last().netWorthMinor)
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
