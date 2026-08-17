package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.domain.Money
import com.finanzen.ui.format.PeriodMode
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    @Test
    fun contextoIncluyeSeccionesEsperadasYMontosConsistentesConDashboard() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        db.transactionQueries.insert(account.id, incomeCat.id, 10_000, account.currency, todayEpoch, "salario", "INCOME", null, null, null, null)
        db.transactionQueries.insert(account.id, expenseCat.id, 3_000, account.currency, todayEpoch, "café", "EXPENSE", null, null, null, null)

        val txs = db.transactionQueries.selectAll().executeAsList()
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val firstOfMonth = LocalDate(today.year, today.month, 1)
        val dashboard = DashboardViewModel.computeDashboard(
            txs = txs,
            accounts = accounts,
            cats = cats,
            mode = PeriodMode.YEAR,
            month = firstOfMonth,
            year = today.year,
            baseCurrency = account.currency,
        )

        val context = AssistantViewModel.buildFinancialContext(
            txs = txs,
            accounts = accounts,
            cats = cats,
            budgets = emptyList(),
            subscriptions = emptyList(),
            today = today,
            baseCurrency = account.currency,
        )

        assertContains(context, "RESUMEN_MES")
        assertContains(context, "CUENTAS")
        assertContains(context, account.name)
        // el balance de la cuenta reportado por el contexto debe coincidir con el que ya usa Dashboard
        val expectedBalance = Money(dashboard.accounts.first().balanceMinor, account.currency).format(2)
        assertContains(context, expectedBalance)
        // ingresos/gastos del mes también deben coincidir con lo que muestra Dashboard (mismo mes/año)
        val monthIdx = today.monthNumber - 1
        assertContains(context, Money(dashboard.incomeExpenseTrend[monthIdx].incomeMinor, account.currency).format(2))
        assertContains(context, Money(dashboard.incomeExpenseTrend[monthIdx].expenseMinor, account.currency).format(2))
    }

    @Test
    fun contextoSeTruncaAlSuperarElLimiteDeCaracteres() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // Muchas cuentas para forzar que el contexto exceda MAX_CONTEXT_CHARS.
        repeat(200) { i ->
            db.accountQueries.insert("Cuenta extra $i", "CASH", account.currency, 1_000, 0, 0)
        }

        val txs = db.transactionQueries.selectAll().executeAsList()
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val context = AssistantViewModel.buildFinancialContext(
            txs = txs,
            accounts = accounts,
            cats = cats,
            budgets = emptyList(),
            subscriptions = emptyList(),
            today = today,
            baseCurrency = account.currency,
        )

        val marker = "\n[...contexto truncado...]"
        assertTrue(context.length <= AssistantViewModel.MAX_CONTEXT_CHARS + marker.length)
        assertContains(context, "truncado")
    }

    @Test
    fun contextoIncluyeGastoPorCategoriaDelMesActual() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        db.transactionQueries.insert(account.id, expenseCat.id, 4_000, account.currency, todayEpoch, "compras del mes", "EXPENSE", null, null, null, null)

        val txs = db.transactionQueries.selectAll().executeAsList()
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val context = AssistantViewModel.buildFinancialContext(
            txs = txs,
            accounts = accounts,
            cats = cats,
            budgets = emptyList(),
            subscriptions = emptyList(),
            today = today,
            baseCurrency = account.currency,
        )

        assertContains(context, "GASTO_CATEGORIA_MES_ACTUAL")
        assertContains(context, expenseCat.name)
        assertContains(context, Money(4_000, account.currency).format(2))
    }

    @Test
    fun contextoMuestraCategoriaConMayorAumentoRespectoAlMesAnterior() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()
        val firstOfMonthEpoch = LocalDate(today.year, today.month, 1).toEpochDays().toLong()
        val prevMonthEpoch = firstOfMonthEpoch - 5 // cae dentro del mes anterior con seguridad

        db.transactionQueries.insert(account.id, expenseCat.id, 1_000, account.currency, prevMonthEpoch, "mes anterior", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, expenseCat.id, 9_000, account.currency, todayEpoch, "mes actual", "EXPENSE", null, null, null, null)

        val txs = db.transactionQueries.selectAll().executeAsList()
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val context = AssistantViewModel.buildFinancialContext(
            txs = txs,
            accounts = accounts,
            cats = cats,
            budgets = emptyList(),
            subscriptions = emptyList(),
            today = today,
            baseCurrency = account.currency,
        )

        assertContains(context, "GASTO_CATEGORIA_MES_ANTERIOR_VS_ACTUAL")
        assertContains(context, expenseCat.name)
        assertContains(context, Money(1_000, account.currency).format(2))
        assertContains(context, Money(9_000, account.currency).format(2))
    }

    @Test
    fun contextoListaLasComprasMasGrandesDelAnioOrdenadasDeMayorAMenor() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        db.transactionQueries.insert(account.id, expenseCat.id, 5_000, account.currency, todayEpoch, "compra chica", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, expenseCat.id, 50_000, account.currency, todayEpoch, "compra grande", "EXPENSE", null, null, null, null)

        val txs = db.transactionQueries.selectAll().executeAsList()
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val context = AssistantViewModel.buildFinancialContext(
            txs = txs,
            accounts = accounts,
            cats = cats,
            budgets = emptyList(),
            subscriptions = emptyList(),
            today = today,
            baseCurrency = account.currency,
        )

        assertContains(context, "TOP_10_COMPRAS_ANIO")
        val topSection = context.substringAfter("TOP_10_COMPRAS_ANIO")
        val bigIndex = topSection.indexOf("compra grande")
        val smallIndex = topSection.indexOf("compra chica")
        assertTrue(bigIndex in 0 until smallIndex, "la compra más grande debe listarse primero")
    }

    @Test
    fun contextoIncluyeGastoPorDiaDeLaSemana() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        db.transactionQueries.insert(account.id, expenseCat.id, 2_000, account.currency, todayEpoch, "gasto de hoy", "EXPENSE", null, null, null, null)

        val txs = db.transactionQueries.selectAll().executeAsList()
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val context = AssistantViewModel.buildFinancialContext(
            txs = txs,
            accounts = accounts,
            cats = cats,
            budgets = emptyList(),
            subscriptions = emptyList(),
            today = today,
            baseCurrency = account.currency,
        )

        assertContains(context, "GASTO_POR_DIA_SEMANA")
        assertContains(context, Money(2_000, account.currency).format(2))
    }

    @Test
    fun contextoCalculaElPromedioMensualDeGastoCorrectamente() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        db.transactionQueries.insert(account.id, expenseCat.id, 6_000, account.currency, todayEpoch, "gasto único", "EXPENSE", null, null, null, null)

        val txs = db.transactionQueries.selectAll().executeAsList()
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val context = AssistantViewModel.buildFinancialContext(
            txs = txs,
            accounts = accounts,
            cats = cats,
            budgets = emptyList(),
            subscriptions = emptyList(),
            today = today,
            baseCurrency = account.currency,
        )

        // un solo mes con datos (el actual) => promedio = el gasto del mes actual
        val expectedAvg = Money(6_000L / today.monthNumber, account.currency).format(2)
        assertContains(context, "GASTO_PROMEDIO_MENSUAL")
        assertContains(context, expectedAvg)
    }

    @Test
    fun inferTemperatureClasificaSegunTipoDePregunta() {
        assertEquals(0.0, AssistantViewModel.inferTemperature("¿Cuánto gasté en comida?"))
        assertEquals(0.1, AssistantViewModel.inferTemperature("Compárame este mes con el anterior."))
        assertEquals(0.2, AssistantViewModel.inferTemperature("¿Qué hábitos financieros detectas?"))
        assertEquals(0.4, AssistantViewModel.inferTemperature("¿Cómo podría ahorrar más?"))
        assertEquals(0.6, AssistantViewModel.inferTemperature("Dame ideas para reducir gastos."))
    }

    @Test
    fun findMatchingTransactionsFiltraPorNotaOCategoria() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()

        db.transactionQueries.insert(account.id, expenseCat.id, 1_500, account.currency, today, "Starbucks café", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, expenseCat.id, 2_000, account.currency, today, "supermercado", "EXPENSE", null, null, null, null)

        val txs = db.transactionQueries.selectAll().executeAsList()
        val cats = db.categoryQueries.selectAll().executeAsList()

        val matches = AssistantViewModel.findMatchingTransactions(txs, cats, "starbucks")

        assertEquals(1, matches.size)
        assertContains(matches.first().note, "Starbucks")
    }

    @Test
    fun systemPromptContieneLasReglasNoNegociables() {
        // Guardia de regresión: si alguien edita el prompt sin querer, esto debe fallar.
        assertContains(AssistantViewModel.SYSTEM_PROMPT, "ÚNICAMENTE los datos")
        assertContains(AssistantViewModel.SYSTEM_PROMPT, "no tienes")
        assertContains(AssistantViewModel.SYSTEM_PROMPT, "No juzgues")
        assertContains(AssistantViewModel.SYSTEM_PROMPT, "Nunca hagas conversión de")
        // guardia de regresión: la excepción para evaluaciones pedidas explícitamente no debe desaparecer
        assertContains(AssistantViewModel.SYSTEM_PROMPT, "sí puedes responder con una evaluación")
        // guardia de regresión: pedir ideas/consejos no debe hacer que el asistente se niegue
        assertContains(AssistantViewModel.SYSTEM_PROMPT, "no te niegues por no tener una sección")
    }
}
