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
            account.id, incomeCat.id, 5_000, account.currency, referenceMonth.toEpochDays().toLong(), "ingreso día 1", "INCOME", null, null, null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 2_000, account.currency, referenceMonth.toEpochDays().toLong(), "gasto día 1", "EXPENSE", null, null, null, null,
        )

        // día 15 del mes: otro ingreso, para distinguir por día (no por mes)
        val day15 = referenceMonth.plus(DatePeriod(days = 14))
        db.transactionQueries.insert(
            account.id, incomeCat.id, 3_000, account.currency, day15.toEpochDays().toLong(), "ingreso día 15", "INCOME", null, null, null, null,
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
                account.id, catA.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "cafe", "EXPENSE", null, null, null, null,
            )
        }
        // categoría B: 1 gasto de 10_000 (conteo bajo, monto alto)
        db.transactionQueries.insert(
            account.id, catB.id, 10_000, "USD", referenceMonth.toEpochDays().toLong(), "tv", "EXPENSE", null, null, null, null,
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

    @Test
    fun costoMensualDeSuscripcionesSumaEquivalenteDeCadaUna() {
        val db = freshDb()
        // MONTHLY: cobra 5_000 tal cual (interval = día 15 del mes, no multiplica).
        db.subscriptionQueries.insert("Streaming", 5_000, "USD", null, null, "MONTHLY", 15, 0, 3, 1)
        // DAILY cada 7 días: 700 x 30.437 / 7 = 3043.7... -> 3_044.
        db.subscriptionQueries.insert("Café diario", 700, "USD", null, null, "DAILY", 7, 0, 0, 1)

        val subscriptions = db.subscriptionQueries.selectAll().executeAsList()

        assertEquals(8_044, AnalysisViewModel.computeSubscriptionMonthlyCost(subscriptions))
    }

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun computeFrequentExpensesTruncaEnTopN() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)
        val period = monthPeriod(referenceMonth)

        // 6 categorías con conteos distintos y decrecientes: 6,5,4,3,2,1.
        val counts = listOf(6, 5, 4, 3, 2, 1)
        counts.forEachIndexed { i, count ->
            db.categoryQueries.insert(null, "Cat$i", "", 0, "EXPENSE")
            val cat = db.categoryQueries.selectAll().executeAsList().last()
            repeat(count) {
                db.transactionQueries.insert(
                    account.id, cat.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "gasto", "EXPENSE", null, null, null, null,
                )
            }
        }

        val frequent = AnalysisViewModel.computeFrequentExpenses(
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            period = period,
        )

        // topN=5 por defecto: la categoría de menor conteo (Cat5, count=1) queda fuera.
        assertEquals(5, frequent.size)
        assertEquals(true, frequent.none { it.name == "Cat5" })
        assertEquals("Cat0", frequent.first().name)
    }

    @Test
    fun computeFrequentExpensesConListaVaciaDevuelveVacio() {
        val frequent = AnalysisViewModel.computeFrequentExpenses(txs = emptyList(), cats = emptyList(), period = 202607)
        assertEquals(emptyList(), frequent)
    }

    @Test
    fun computeFrequentExpensesEnEmpateOrdenaPorInsercionMasReciente() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Primera", "", 0, "EXPENSE")
        db.categoryQueries.insert(null, "Segunda", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val primera = cats.first { it.name == "Primera" }
        val segunda = cats.first { it.name == "Segunda" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)
        // Ambas categorías con el mismo conteo (2): no hay criterio de desempate explícito en
        // computeFrequentExpenses. El orden real lo decide TransactionRow.selectAll(), que ordena
        // `date DESC, id DESC` (Transaction.sq) — con la misma fecha, gana el id más alto, es decir
        // la transacción insertada MÁS RECIENTE. Como "Segunda" se insertó después, aparece primero.
        repeat(2) {
            db.transactionQueries.insert(
                account.id, primera.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "a", "EXPENSE", null, null, null, null,
            )
        }
        repeat(2) {
            db.transactionQueries.insert(
                account.id, segunda.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "b", "EXPENSE", null, null, null, null,
            )
        }

        val frequent = AnalysisViewModel.computeFrequentExpenses(
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
            period = monthPeriod(referenceMonth),
        )

        assertEquals(listOf("Segunda", "Primera"), frequent.map { it.name })
    }

    @Test
    fun computeSubscriptionMonthlyCostConListaVaciaEsCero() {
        assertEquals(0L, AnalysisViewModel.computeSubscriptionMonthlyCost(emptyList()))
    }

    @Test
    fun mergeOthersByPctUbicaOtrosSegunSuPropioPorcentajeNoSiempreAlFinal() {
        // Reproduce el bug reportado: "Otros" agrega más que cualquier categoría individual (39%)
        // y antes quedaba fijo al final en vez de aparecer primero.
        val top = listOf(
            CategorySlice(name = "A", amountMinor = 30, pct = 0.30f),
            CategorySlice(name = "B", amountMinor = 20, pct = 0.20f),
            CategorySlice(name = "C", amountMinor = 11, pct = 0.11f),
        )
        val otros = CategorySlice(name = "Otros", amountMinor = 39, pct = 0.39f)

        val merged = AnalysisViewModel.mergeOthersByPct(top, otros)

        assertEquals(listOf("Otros", "A", "B", "C"), merged.map { it.name })
    }

    @Test
    fun mergeOthersByPctSinOtrosDevuelveElTopSinCambios() {
        val top = listOf(CategorySlice(name = "A", amountMinor = 30, pct = 0.30f))
        assertEquals(top, AnalysisViewModel.mergeOthersByPct(top, null))
    }

    @Test
    fun computeFrequentExpensesConSubcategoriaMuestraCategoriaPadreYSubcategoriaSeparadas() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Comida", "", 0, "EXPENSE")
        val parent = db.categoryQueries.selectAll().executeAsList().first { it.name == "Comida" }
        db.categoryQueries.insert(parent.id, "Restaurantes", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val child = cats.first { it.name == "Restaurantes" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)
        db.transactionQueries.insert(
            account.id, child.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "cena", "EXPENSE", null, null, null, null,
        )

        val frequent = AnalysisViewModel.computeFrequentExpenses(
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
            period = monthPeriod(referenceMonth),
        )

        assertEquals("Comida", frequent.first().name)
        assertEquals("Restaurantes", frequent.first().subcategoryName)
    }

    @Test
    fun computeFrequentExpensesSinSubcategoriaDejaSubcategoryNameNulo() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Transporte", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val cat = cats.first { it.name == "Transporte" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)
        db.transactionQueries.insert(
            account.id, cat.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "bus", "EXPENSE", null, null, null, null,
        )

        val frequent = AnalysisViewModel.computeFrequentExpenses(
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
            period = monthPeriod(referenceMonth),
        )

        assertEquals("Transporte", frequent.first().name)
        assertEquals(null, frequent.first().subcategoryName)
    }
}
