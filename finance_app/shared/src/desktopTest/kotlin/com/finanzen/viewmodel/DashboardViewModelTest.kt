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
            budgets = emptyList(),
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
            budgets = emptyList(),
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
            budgets = emptyList(),
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
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // Top gastos es histórico: suma ambos, aunque solo uno sea del mes en curso.
        assertEquals(1, data.topExpenses.size)
        assertEquals(8_000, data.topExpenses.first().amountMinor)
        // El desglose del mes sigue siendo solo del mes en curso.
        assertEquals(3_000, data.monthExpenseMinor)
    }

    @Test
    fun tasaDeAhorroEsCeroSinIngresos() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // Solo un gasto este mes, sin ingresos.
        db.transactionQueries.insert(
            account.id, expenseCat.id, 4_000, account.currency, todayEpoch, "gasto", "EXPENSE", null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // Sin ingresos, la tasa de ahorro es 0 (no divide por cero).
        assertEquals(0f, data.savingsRate)
    }

    @Test
    fun tasaDeAhorroEntreCeroYUno() {
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
            account.id, expenseCat.id, 4_000, account.currency, todayEpoch, "gasto", "EXPENSE", null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // (10000 - 4000) / 10000 = 0.6
        assertEquals(0.6f, data.savingsRate)
    }

    @Test
    fun cuentaDeCreditoQuedaNegativaEnAccountBar() {
        val db = freshDb()
        seedIfEmpty(db)
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // Tarjeta de crédito con saldo inicial 0 (name, type, currency, opening, color, archived).
        db.accountQueries.insert("Tarjeta", "CREDIT", "USD", 0, 0, 0)
        val tarjeta = db.accountQueries.selectAll().executeAsList().first { it.name == "Tarjeta" }
        db.transactionQueries.insert(
            tarjeta.id, expenseCat.id, 4_000, tarjeta.currency, todayEpoch, "compra", "EXPENSE", null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
        )

        // Opening 0 menos el gasto de 4000: la barra de la cuenta CREDIT queda negativa.
        assertEquals(-4_000, data.accounts.first { it.type == "CREDIT" }.balanceMinor)
    }

    @Test
    fun presupuestoSoloIncluyeCategoriasConLimite() {
        val db = freshDb()
        seedIfEmpty(db)
        val expenseCats = db.categoryQueries.selectByKind("EXPENSE").executeAsList()
        val budgetedCat = expenseCats.first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstOfMonth = LocalDate(today.year, today.month, 1)
        val periodMonth = monthPeriod(firstOfMonth)

        // Solo una categoría de gasto tiene presupuesto (categoryId, periodMonth, limitMinor).
        db.budgetQueries.upsert(budgetedCat.id, periodMonth, 5_000)

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = db.budgetQueries.selectAll().executeAsList(),
            firstOfMonth = firstOfMonth,
        )

        // Solo la categoría con límite > 0 aparece en presupuestos.
        assertEquals(1, data.budgets.size)
        assertEquals(budgetedCat.id, data.budgets.first().categoryId)
    }
}
