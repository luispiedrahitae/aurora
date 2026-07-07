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
        assertEquals(1, data.topExpenses.size)
        assertEquals(3_000, data.topExpenses.first().amountMinor)
    }

    @Test
    fun cuentaArchivadaNoSumaEnBalanceTotal() {
        val db = freshDb()
        seedIfEmpty(db)
        val efectivo = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // ingreso normal en la cuenta activa (Efectivo)
        db.transactionQueries.insert(
            efectivo.id, incomeCat.id, 10_000, efectivo.currency, todayEpoch, "salario", "INCOME", null, null,
        )

        // cuenta archivada con saldo inicial y un gasto propio: nada de esto debe colarse
        db.accountQueries.insert("Ahorro cerrado", "DEBIT", "USD", 50_000, 0, 1)
        val archivada = db.accountQueries.selectAllAny().executeAsList().first { it.name == "Ahorro cerrado" }
        db.transactionQueries.insert(
            archivada.id, expenseCat.id, 5_000, archivada.currency, todayEpoch, "gasto archivado", "EXPENSE", null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            // selectAll() ya excluye archived = 1: solo Efectivo llega aquí
            accounts = db.accountQueries.selectAll().executeAsList(),
            // selectAll() de transacciones SÍ trae la de la cuenta archivada
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // 0 (opening Efectivo) + 10000 ingreso; ni los 50000 de apertura ni los -5000 del gasto
        // de la cuenta archivada deben aparecer, aunque su transacción sí venga en `txs`.
        assertEquals(10_000, data.totalBalanceMinor)
    }

    @Test
    fun cuentaCreditoNoSumaEnBalanceTotalPeroSiEnGastosDelMes() {
        val db = freshDb()
        seedIfEmpty(db)
        val efectivo = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // gasto normal en la cuenta activa (Efectivo), este mes
        db.transactionQueries.insert(
            efectivo.id, expenseCat.id, 2_000, efectivo.currency, todayEpoch, "super", "EXPENSE", null, null,
        )

        // tarjeta de crédito activa (no archivada) con saldo inicial y un gasto este mes
        db.accountQueries.insert("Tarjeta", "CREDIT", "USD", 1_000, 0, 0)
        val tarjeta = db.accountQueries.selectAll().executeAsList().first { it.name == "Tarjeta" }
        db.transactionQueries.insert(
            tarjeta.id, expenseCat.id, 4_000, tarjeta.currency, todayEpoch, "compra tarjeta", "EXPENSE", null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // Balance total: 0 (opening Efectivo) - 2000 (gasto Efectivo); ni el opening 1000 ni el
        // gasto 4000 de la tarjeta CREDIT cuentan aquí.
        assertEquals(-2_000, data.totalBalanceMinor)
        // Los pills de mes SÍ incluyen todas las cuentas, también CREDIT.
        assertEquals(6_000, data.monthExpenseMinor)
    }

    @Test
    fun topGastosEsHistoricoNoSoloDelMes() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val pastMonth = today.minus(DatePeriod(months = 2))

        // gasto de un mes anterior y otro del mes actual, misma categoría
        db.transactionQueries.insert(
            account.id, expenseCat.id, 5_000, account.currency, pastMonth.toEpochDays().toLong(), "gasto viejo", "EXPENSE", null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 3_000, account.currency, today.toEpochDays().toLong(), "gasto reciente", "EXPENSE", null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // Top gastos es histórico: suma ambos, aunque solo uno sea del mes en curso.
        assertEquals(1, data.topExpenses.size)
        assertEquals(8_000, data.topExpenses.first().amountMinor)
        // El desglose del mes sigue siendo solo del mes en curso.
        assertEquals(3_000, data.monthExpenseMinor)
    }
}
