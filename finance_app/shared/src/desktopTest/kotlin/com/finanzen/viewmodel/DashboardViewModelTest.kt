package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.ui.format.monthPeriod
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
    fun agregaBalanceGastosYTopCategoriasDelAnio() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        db.transactionQueries.insert(
            account.id, incomeCat.id, 10_000, account.currency, todayEpoch, "salario", "INCOME", null, null, null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 3_000, account.currency, todayEpoch, "café", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            txs = db.transactionQueries.selectAll().executeAsList(),
            accounts = db.accountQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        // opening 0 + 10000 ingreso - 3000 gasto
        assertEquals(7_000, data.totalBalanceMinor)
        assertEquals(10_000, data.yearIncomeMinor)
        assertEquals(3_000, data.yearExpenseMinor)
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
            efectivo.id, incomeCat.id, 10_000, efectivo.currency, todayEpoch, "salario", "INCOME", null, null, null, null,
        )

        // cuenta archivada con saldo inicial y un gasto propio: nada de esto debe colarse
        db.accountQueries.insert("Ahorro cerrado", "DEBIT", "USD", 50_000, 0, 1)
        val archivada = db.accountQueries.selectAllAny().executeAsList().first { it.name == "Ahorro cerrado" }
        db.transactionQueries.insert(
            archivada.id, expenseCat.id, 5_000, archivada.currency, todayEpoch, "gasto archivado", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            // selectAll() ya excluye archived = 1: solo Efectivo llega aquí
            accounts = db.accountQueries.selectAll().executeAsList(),
            // selectAll() de transacciones SÍ trae la de la cuenta archivada
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = efectivo.currency,
        )

        // 0 (opening Efectivo) + 10000 ingreso; ni los 50000 de apertura ni los -5000 del gasto
        // de la cuenta archivada deben aparecer, aunque su transacción sí venga en `txs`.
        assertEquals(10_000, data.totalBalanceMinor)
    }

    @Test
    fun cuentaCreditoNoSumaEnBalanceTotalPeroSiEnGastosDelAnio() {
        val db = freshDb()
        seedIfEmpty(db)
        val efectivo = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // gasto normal en la cuenta activa (Efectivo), este año
        db.transactionQueries.insert(
            efectivo.id, expenseCat.id, 2_000, efectivo.currency, todayEpoch, "super", "EXPENSE", null, null, null, null,
        )

        // tarjeta de crédito activa (no archivada) con saldo inicial y un gasto este año
        db.accountQueries.insert("Tarjeta", "CREDIT", "USD", 1_000, 0, 0)
        val tarjeta = db.accountQueries.selectAll().executeAsList().first { it.name == "Tarjeta" }
        db.transactionQueries.insert(
            tarjeta.id, expenseCat.id, 4_000, tarjeta.currency, todayEpoch, "compra tarjeta", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = efectivo.currency,
        )

        // Balance total: 0 (opening Efectivo) - 2000 (gasto Efectivo); ni el opening 1000 ni el
        // gasto 4000 de la tarjeta CREDIT cuentan aquí.
        assertEquals(-2_000, data.totalBalanceMinor)
        // Los pills del año SÍ incluyen todas las cuentas, también CREDIT.
        assertEquals(6_000, data.yearExpenseMinor)
    }

    @Test
    fun tasaDeAhorroEsCeroSinIngresos() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // Solo un gasto este año, sin ingresos.
        db.transactionQueries.insert(
            account.id, expenseCat.id, 4_000, account.currency, todayEpoch, "gasto", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = account.currency,
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
            account.id, incomeCat.id, 10_000, account.currency, todayEpoch, "salario", "INCOME", null, null, null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 4_000, account.currency, todayEpoch, "gasto", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = account.currency,
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
            tarjeta.id, expenseCat.id, 4_000, tarjeta.currency, todayEpoch, "compra", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = tarjeta.currency,
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

        val accounts = db.accountQueries.selectAll().executeAsList()
        val data = DashboardViewModel.computeDashboard(
            accounts = accounts,
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = db.budgetQueries.selectAll().executeAsList(),
            firstOfMonth = firstOfMonth,
            year = today.year,
            baseCurrency = accounts.first().currency,
        )

        // Solo la categoría con límite > 0 aparece en presupuestos.
        assertEquals(1, data.budgets.size)
        assertEquals(budgetedCat.id, data.budgets.first().categoryId)
    }

    @Test
    fun computeMonthlyNetWorthAcumulaPorMesYExcluyeCredito() {
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

        val year = 2026
        // pago en junio (mes 6)
        val juneFirst = LocalDate(year, 6, 1).toEpochDays().toLong()

        db.transactionQueries.insert(
            cash.id, incomeCat.id, 5_000, "USD", juneFirst, "sueldo", "INCOME", null, null, null, null,
        )
        // gasto en tarjeta de crédito: NO debe reducir el patrimonio neto
        db.transactionQueries.insert(
            credit.id, expenseCat.id, 9_000, "USD", juneFirst, "compra a credito", "EXPENSE", null, null, null, null,
        )

        val netWorth = DashboardViewModel.computeMonthlyNetWorth(accounts, db.transactionQueries.selectAll().executeAsList(), year)

        assertEquals(12, netWorth.size)
        // antes de junio: solo el saldo inicial de CASH
        assertEquals(10_000, netWorth[0].netWorthMinor) // enero
        assertEquals(10_000, netWorth[4].netWorthMinor) // mayo
        // desde junio en adelante: + 5_000 (ingreso), sin restar el gasto de crédito
        assertEquals(15_000, netWorth[5].netWorthMinor) // junio
        assertEquals(15_000, netWorth.last().netWorthMinor) // diciembre
    }

    @Test
    fun incomeByMonthYExpenseByMonthTienenDoceMesesYExcluyenOtrosAnios() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        // marzo de 2026: cuenta
        val march2026 = LocalDate(2026, 3, 15).toEpochDays().toLong()
        db.transactionQueries.insert(
            account.id, incomeCat.id, 7_000, account.currency, march2026, "ingreso 2026", "INCOME", null, null, null, null,
        )
        db.transactionQueries.insert(
            account.id, expenseCat.id, 1_000, account.currency, march2026, "gasto 2026", "EXPENSE", null, null, null, null,
        )
        // marzo de 2025: NO debe contar en el año 2026
        val march2025 = LocalDate(2025, 3, 15).toEpochDays().toLong()
        db.transactionQueries.insert(
            account.id, incomeCat.id, 99_000, account.currency, march2025, "ingreso 2025", "INCOME", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(2026, 3, 1),
            year = 2026,
            baseCurrency = account.currency,
        )

        assertEquals(12, data.incomeByMonth.size)
        assertEquals(12, data.expenseByMonth.size)
        assertEquals(7_000, data.incomeByMonth[2].amountMinor) // marzo = índice 2
        assertEquals(1_000, data.expenseByMonth[2].amountMinor)
        assertEquals(0, data.incomeByMonth[0].amountMinor) // enero, sin movimientos
    }

    @Test
    fun gastoEnSuscripcionesSoloSumaTransaccionesConSubscriptionId() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        db.subscriptionQueries.insert("Streaming", 1_500, account.currency, expenseCat.id, account.id, "MONTHLY", 1, 0, 3, 1)
        val subscription = db.subscriptionQueries.selectAll().executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // gasto ligado a una suscripción: cuenta
        db.transactionQueries.insert(
            account.id, expenseCat.id, 1_500, account.currency, todayEpoch, "cargo streaming", "EXPENSE", null, null, null, subscription.id,
        )
        // gasto normal, sin suscripción: NO cuenta
        db.transactionQueries.insert(
            account.id, expenseCat.id, 3_000, account.currency, todayEpoch, "super", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        assertEquals(1_500, data.subscriptionSpendMinor)
    }

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun computeSavingsRateConGastoMayorAIngresoEsNegativa() {
        // Gastar más de lo ingresado es un déficit y la tasa debe reflejarlo (-400% aquí); la UI
        // decide cómo pintarlo (el gauge recorta el arco a 0 pero muestra el número real).
        assertEquals(-4f, DashboardViewModel.computeSavingsRate(income = 1_000L, expense = 5_000L))
    }

    @Test
    fun computeSavingsRateConAhorroTotalTopaEnCienPorCiento() {
        assertEquals(1f, DashboardViewModel.computeSavingsRate(income = 1_000L, expense = 0L))
    }

    @Test
    fun computeDashboardIncluyeTotalesDelAnioAnteriorParaDeltas() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        // 2025: ingreso 8_000, gasto 2_000. 2026: ingreso 10_000.
        val in2025 = LocalDate(2025, 6, 15).toEpochDays().toLong()
        val in2026 = LocalDate(2026, 6, 15).toEpochDays().toLong()
        db.transactionQueries.insert(account.id, incomeCat.id, 8_000, account.currency, in2025, "salario 25", "INCOME", null, null, null, null)
        db.transactionQueries.insert(account.id, expenseCat.id, 2_000, account.currency, in2025, "gasto 25", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, incomeCat.id, 10_000, account.currency, in2026, "salario 26", "INCOME", null, null, null, null)

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(2026, 6, 1),
            year = 2026,
            baseCurrency = account.currency,
        )

        assertEquals(10_000, data.yearIncomeMinor)
        assertEquals(8_000, data.prevYearIncomeMinor)
        assertEquals(2_000, data.prevYearExpenseMinor)
    }

    @Test
    fun computeDashboardConAnioSinGastosNoRompeDivisionPorCero() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()

        // Solo ingreso este año, ningún gasto: yearExpenseFloor evita la división por cero del donut.
        db.transactionQueries.insert(
            account.id, incomeCat.id, 8_000, account.currency, todayEpoch, "salario", "INCOME", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        assertEquals(emptyList(), data.categoryBreakdown)
    }

    @Test
    fun donutIncluyeElColorGuardadoDeLaCategoria() {
        // FIX (hallazgo alto #7): el color que el usuario elige para una categoría ahora se propaga
        // a CategorySlice.color en vez de perderse (antes CategoryDonutChart lo hardcodeaba a 0L).
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(parentId = null, name = "Mascotas", icon = "", color = 0xFF00FF00L, kind = "EXPENSE")
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Mascotas" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEpoch = today.toEpochDays().toLong()
        db.transactionQueries.insert(account.id, cat.id, 2_000, account.currency, todayEpoch, "vet", "EXPENSE", null, null, null, null)

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        assertEquals(0xFF00FF00L, data.categoryBreakdown.single { it.name == "Mascotas" }.color)
    }

    @Test
    fun categoryBreakdownAnnualSubeSubcategoriaAPadreEnVezDeMostrarlaComoPorcionSeparada() {
        // FIX: computeDashboard agrupaba por categoryId crudo, así que una transacción etiquetada
        // con una subcategoría aparecía como su propia porción del donut en vez de sumarse a la
        // categoría padre (inconsistente con AnalysisViewModel.computeFrequentExpenses, que sí
        // resuelve parentId).
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Comida", "", 0, "EXPENSE")
        val parent = db.categoryQueries.selectAll().executeAsList().first { it.name == "Comida" }
        db.categoryQueries.insert(parent.id, "Restaurantes", "", 0, "EXPENSE")
        val child = db.categoryQueries.selectAll().executeAsList().first { it.name == "Restaurantes" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        db.transactionQueries.insert(
            account.id, child.id, 3_000, account.currency, today.toEpochDays().toLong(), "cena", "EXPENSE", null, null, null, null,
        )

        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        assertEquals(1, data.categoryBreakdown.size)
        val comida = data.categoryBreakdown.first()
        assertEquals("Comida", comida.name)
        assertEquals(3_000, comida.amountMinor)
        assertEquals("Restaurantes", comida.subcategories.single().name)
    }

    @Test
    fun computeDashboardExcluyeCuentasQueNoEstanEnLaMonedaBase() {
        // FIX (hallazgo crítico #1): la app no maneja FX (ver SettingsRepository.setBaseCurrency), así
        // que computeDashboard ya no suma cuentas de monedas distintas como si fueran una sola. Solo
        // las cuentas/transacciones en la moneda base entran en los totales; el resto se excluye.
        val db = freshDb()
        db.accountQueries.insert("1 Efectivo USD", "CASH", "USD", 10_000, 0, 0)
        db.accountQueries.insert("2 Ahorro COP", "SAVINGS", "COP", 5_000, 0, 0)
        val accounts = db.accountQueries.selectAll().executeAsList()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val data = DashboardViewModel.computeDashboard(
            accounts = accounts,
            txs = emptyList(),
            cats = emptyList(),
            budgets = emptyList(),
            firstOfMonth = LocalDate(today.year, today.month, 1),
            year = today.year,
            baseCurrency = "USD",
        )

        // Solo la cuenta USD (moneda base) cuenta; la cuenta COP queda fuera del total y de "Saldos
        // por cuenta".
        assertEquals(10_000, data.totalBalanceMinor)
        assertEquals("USD", data.currency)
        assertEquals(1, data.accounts.size)
    }
}
