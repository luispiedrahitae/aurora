package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.domain.Money
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

        val dashboard = DashboardViewModel.computeDashboard(
            txs,
            accounts,
            cats,
            emptyList(),
            LocalDate(today.year, today.month, 1),
            today.year,
            account.currency,
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
        assertContains(context, Money(dashboard.incomeByMonth[monthIdx].amountMinor, account.currency).format(2))
        assertContains(context, Money(dashboard.expenseByMonth[monthIdx].amountMinor, account.currency).format(2))
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
    }
}
