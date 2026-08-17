package com.finanzen.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.data.seedIfEmpty
import com.finanzen.db.FinanzenDb
import com.finanzen.ui.format.PeriodMode
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardViewModelTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    // ---- Modo AÑO ----

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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        // opening 0 + 10000 ingreso - 3000 gasto
        assertEquals(7_000, data.totalBalanceMinor)
        assertEquals(10_000, data.periodIncomeMinor)
        assertEquals(3_000, data.periodExpenseMinor)
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
            year = today.year,
            baseCurrency = efectivo.currency,
        )

        // Balance total: 0 (opening Efectivo) - 2000 (gasto Efectivo); ni el opening 1000 ni el
        // gasto 4000 de la tarjeta CREDIT cuentan aquí.
        assertEquals(-2_000, data.totalBalanceMinor)
        // Los pills del año SÍ incluyen todas las cuentas, también CREDIT.
        assertEquals(6_000, data.periodExpenseMinor)
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
            year = today.year,
            baseCurrency = tarjeta.currency,
        )

        // Opening 0 menos el gasto de 4000: la barra de la cuenta CREDIT queda negativa.
        assertEquals(-4_000, data.accounts.first { it.type == "CREDIT" }.balanceMinor)
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
    fun incomeExpenseTrendEnModoAnioTieneDoceMesesYExcluyeOtrosAnios() {
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
            mode = PeriodMode.YEAR,
            month = LocalDate(2026, 1, 1),
            year = 2026,
            baseCurrency = account.currency,
        )

        assertEquals(12, data.incomeExpenseTrend.size)
        assertEquals(7_000, data.incomeExpenseTrend[2].incomeMinor) // marzo = índice 2
        assertEquals(1_000, data.incomeExpenseTrend[2].expenseMinor)
        assertEquals(0, data.incomeExpenseTrend[0].incomeMinor) // enero, sin movimientos
    }

    @Test
    fun costoDeSuscripcionesEnModoAnioAnualizaElCostoMensualRecurrente() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Suscripciones", "", 0, "EXPENSE")
        val catId = db.categoryQueries.lastInsertRowId().executeAsOne()
        db.subscriptionQueries.insert("Streaming", 1_500, "USD", catId, account.id, "MONTHLY", 1, 0, 3, 1)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = emptyList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            subscriptions = db.subscriptionQueries.selectAll().executeAsList(),
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        // 1_500 mensual recurrente x 12 = 18_000 anualizado (en vez de sumar el gasto histórico por
        // transacción, que era el cómputo anterior).
        assertEquals(18_000, data.subscriptionCostMinor)
    }

    @Test
    fun costoDeSuscripcionesEnModoMesEsElCostoMensualSinAnualizar() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Suscripciones", "", 0, "EXPENSE")
        val catId = db.categoryQueries.lastInsertRowId().executeAsOne()
        db.subscriptionQueries.insert("Streaming", 1_500, "USD", catId, account.id, "MONTHLY", 1, 0, 3, 1)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstOfMonth = LocalDate(today.year, today.month, 1)
        val data = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = emptyList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            subscriptions = db.subscriptionQueries.selectAll().executeAsList(),
            mode = PeriodMode.MONTH,
            month = firstOfMonth,
            year = today.year,
            baseCurrency = account.currency,
        )

        assertEquals(1_500, data.subscriptionCostMinor)
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
    fun computeDashboardIncluyeTotalesDelPeriodoAnteriorParaDeltas() {
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
            mode = PeriodMode.YEAR,
            month = LocalDate(2026, 1, 1),
            year = 2026,
            baseCurrency = account.currency,
        )

        assertEquals(10_000, data.periodIncomeMinor)
        assertEquals(8_000, data.prevPeriodIncomeMinor)
        assertEquals(2_000, data.prevPeriodExpenseMinor)
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
            year = today.year,
            baseCurrency = account.currency,
        )

        assertEquals(0xFF00FF00L, data.categoryBreakdown.single { it.name == "Mascotas" }.color)
    }

    @Test
    fun categoryBreakdownAnnualSubeSubcategoriaAPadreEnVezDeMostrarlaComoPorcionSeparada() {
        // FIX: computeDashboard agrupaba por categoryId crudo, así que una transacción etiquetada
        // con una subcategoría aparecía como su propia porción del donut en vez de sumarse a la
        // categoría padre (inconsistente con computeFrequentExpenses, que sí resuelve parentId).
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
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
            mode = PeriodMode.YEAR,
            month = LocalDate(today.year, 1, 1),
            year = today.year,
            baseCurrency = "USD",
        )

        // Solo la cuenta USD (moneda base) cuenta; la cuenta COP queda fuera del total y de "Saldos
        // por cuenta".
        assertEquals(10_000, data.totalBalanceMinor)
        assertEquals("USD", data.currency)
        assertEquals(1, data.accounts.size)
    }

    // ---- Modo MES ----

    @Test
    fun filterByPeriodFiltraPorMesYPorAnio() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()

        db.transactionQueries.insert(account.id, cat.id, 1_000, account.currency, LocalDate(2026, 1, 15).toEpochDays().toLong(), "ene26", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, cat.id, 2_000, account.currency, LocalDate(2026, 2, 10).toEpochDays().toLong(), "feb26", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, cat.id, 3_000, account.currency, LocalDate(2025, 1, 5).toEpochDays().toLong(), "ene25", "EXPENSE", null, null, null, null)

        val allTxs = db.transactionQueries.selectAll().executeAsList()

        val byMonth = DashboardViewModel.filterByPeriod(allTxs, PeriodMode.MONTH, LocalDate(2026, 1, 1), 2026)
        assertEquals(listOf("ene26"), byMonth.map { it.note })

        val byYear = DashboardViewModel.filterByPeriod(allTxs, PeriodMode.YEAR, LocalDate(2026, 1, 1), 2026)
        assertEquals(setOf("ene26", "feb26"), byYear.map { it.note }.toSet())
    }

    @Test
    fun computeDailyTrendCubreTodosLosDiasDelMesConIngresoYGastoPorDia() {
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

        val trend = DashboardViewModel.computeDailyTrend(db.transactionQueries.selectAll().executeAsList(), referenceMonth)

        assertEquals(expectedDays, trend.size)
        assertEquals("1", trend.first().axisLabel)
        assertEquals(5_000, trend.first().incomeMinor)
        assertEquals(2_000, trend.first().expenseMinor)
        assertEquals("15", trend[14].axisLabel)
        assertEquals(3_000, trend[14].incomeMinor)
        assertEquals(0, trend[14].expenseMinor)
    }

    @Test
    fun computeDailyTrendTieneVeintinueveDiasEnFebreroBisiesto() {
        // 2028 es bisiesto: febrero tiene 29 días — el bucketing diario debe cubrirlos todos, no
        // asumir 28 fijo.
        val trend = DashboardViewModel.computeDailyTrend(emptyList(), LocalDate(2028, 2, 1))
        assertEquals(29, trend.size)
        assertEquals("29", trend.last().axisLabel)
    }

    @Test
    fun computeDailyNetWorthAcumulaHastaCadaDiaDelMes() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 10_000, 0, 0)
        val accounts = db.accountQueries.selectAll().executeAsList()
        val cash = accounts.first()
        db.categoryQueries.insert(null, "Sueldo", "", 0, "INCOME")
        val incomeCat = db.categoryQueries.selectByKind("INCOME").executeAsList().first()

        val month = LocalDate(2026, 6, 1)
        // ingreso el día 10 de junio
        val day10 = month.plus(DatePeriod(days = 9)).toEpochDays().toLong()
        db.transactionQueries.insert(cash.id, incomeCat.id, 2_000, "USD", day10, "sueldo", "INCOME", null, null, null, null)

        val trend = DashboardViewModel.computeDailyNetWorth(accounts, db.transactionQueries.selectAll().executeAsList(), month)

        assertEquals(30, trend.size) // junio tiene 30 días
        assertEquals(10_000, trend[0].netWorthMinor) // día 1: antes del ingreso
        assertEquals(10_000, trend[8].netWorthMinor) // día 9: aún antes del ingreso
        assertEquals(12_000, trend[9].netWorthMinor) // día 10: incluye el ingreso
        assertEquals(12_000, trend.last().netWorthMinor) // día 30: se mantiene
    }

    @Test
    fun projectMonthEndExpenseExtrapolaSoloElMesEnCurso() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstOfMonth = LocalDate(today.year, today.month, 1)
        val daysInMonth = firstOfMonth.plus(DatePeriod(months = 1)).toEpochDays() - firstOfMonth.toEpochDays()

        // 100 por día transcurrido -> proyección = 100 por día del mes completo.
        val projected = DashboardViewModel.projectMonthEndExpense(
            expenseSoFarMinor = today.dayOfMonth * 100L,
            today = today,
            month = firstOfMonth,
        )
        assertEquals(daysInMonth * 100L, projected)
    }

    @Test
    fun projectMonthEndExpenseDevuelveNullParaMesesPasadosOFuturos() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val prevMonth = LocalDate(today.year, today.month, 1).plus(DatePeriod(months = -1))
        val nextMonth = LocalDate(today.year, today.month, 1).plus(DatePeriod(months = 1))
        assertEquals(null, DashboardViewModel.projectMonthEndExpense(5_000, today, prevMonth))
        assertEquals(null, DashboardViewModel.projectMonthEndExpense(5_000, today, nextMonth))
    }

    @Test
    fun computeDashboardEnModoMesProyectaSoloElMesEnCursoNoOtrosMeses() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val expenseCat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstOfMonth = LocalDate(today.year, today.month, 1)

        db.transactionQueries.insert(account.id, expenseCat.id, 1_000, account.currency, today.toEpochDays().toLong(), "gasto", "EXPENSE", null, null, null, null)

        val currentMonth = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            mode = PeriodMode.MONTH,
            month = firstOfMonth,
            year = today.year,
            baseCurrency = account.currency,
        )
        assertEquals(true, currentMonth.projectedExpenseMinor != null)

        val prevMonth = firstOfMonth.plus(DatePeriod(months = -1))
        val pastMonth = DashboardViewModel.computeDashboard(
            accounts = db.accountQueries.selectAll().executeAsList(),
            txs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            mode = PeriodMode.MONTH,
            month = prevMonth,
            year = today.year,
            baseCurrency = account.currency,
        )
        assertEquals(null, pastMonth.projectedExpenseMinor)
    }

    // ---- Compartidos (antes en AnalysisViewModelTest) ----

    @Test
    fun computeFrequentExpensesOrdenaPorConteoNoPorMonto() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Café", "", 0, "EXPENSE")
        val cafePadre = db.categoryQueries.selectAll().executeAsList().first { it.name == "Café" }
        db.categoryQueries.insert(cafePadre.id, "Espresso", "", 0, "EXPENSE")
        db.categoryQueries.insert(null, "Electrónica", "", 0, "EXPENSE")
        val electronicaPadre = db.categoryQueries.selectAll().executeAsList().first { it.name == "Electrónica" }
        db.categoryQueries.insert(electronicaPadre.id, "TV", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val catA = cats.first { it.name == "Espresso" }
        val catB = cats.first { it.name == "TV" }

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

        val frequent = DashboardViewModel.computeFrequentExpenses(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
        )

        // ordena por conteo, no por monto: A (3 veces) va primero pese a que B es más caro
        assertEquals("Café", frequent.first().name)
        assertEquals(3, frequent.first().count)
        // pero cada fila acumula su monto total, que es lo que la UI muestra junto al conteo
        assertEquals(300, frequent.first().amountMinor)
        assertEquals(10_000, frequent[1].amountMinor)
    }

    @Test
    fun costoMensualDeSuscripcionesSumaEquivalenteDeCadaUna() {
        val db = freshDb()
        db.categoryQueries.insert(null, "Suscripciones", "", 0, "EXPENSE")
        val catId = db.categoryQueries.lastInsertRowId().executeAsOne()
        // MONTHLY: cobra 5_000 tal cual (interval = día 15 del mes, no multiplica).
        db.subscriptionQueries.insert("Streaming", 5_000, "USD", catId, null, "MONTHLY", 15, 0, 3, 1)
        // DAILY cada 7 días: 700 x 30.437 / 7 = 3043.7... -> 3_044.
        db.subscriptionQueries.insert("Café diario", 700, "USD", catId, null, "DAILY", 7, 0, 0, 1)

        val subscriptions = db.subscriptionQueries.selectAll().executeAsList()

        assertEquals(8_044, DashboardViewModel.computeSubscriptionMonthlyCost(subscriptions))
    }

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun computeFrequentExpensesTruncaEnTopN() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val referenceMonth = LocalDate(today.year, today.month, 1)

        // 6 categorías con conteos distintos y decrecientes: 6,5,4,3,2,1.
        val counts = listOf(6, 5, 4, 3, 2, 1)
        counts.forEachIndexed { i, count ->
            db.categoryQueries.insert(null, "Cat$i", "", 0, "EXPENSE")
            val parent = db.categoryQueries.selectAll().executeAsList().last()
            db.categoryQueries.insert(parent.id, "Cat${i}Sub", "", 0, "EXPENSE")
            val cat = db.categoryQueries.selectAll().executeAsList().last()
            repeat(count) {
                db.transactionQueries.insert(
                    account.id, cat.id, 100, "USD", referenceMonth.toEpochDays().toLong(), "gasto", "EXPENSE", null, null, null, null,
                )
            }
        }

        val frequent = DashboardViewModel.computeFrequentExpenses(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
        )

        // topN=5 por defecto: la categoría de menor conteo (Cat5, count=1) queda fuera.
        assertEquals(5, frequent.size)
        assertEquals(true, frequent.none { it.name == "Cat5" })
        assertEquals("Cat0", frequent.first().name)
    }

    @Test
    fun computeFrequentExpensesConListaVaciaDevuelveVacio() {
        val frequent = DashboardViewModel.computeFrequentExpenses(expenseTxs = emptyList(), cats = emptyList())
        assertEquals(emptyList(), frequent)
    }

    @Test
    fun computeFrequentExpensesEnEmpateOrdenaPorInsercionMasReciente() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Primera", "", 0, "EXPENSE")
        val primeraPadre = db.categoryQueries.selectAll().executeAsList().first { it.name == "Primera" }
        db.categoryQueries.insert(primeraPadre.id, "PrimeraSub", "", 0, "EXPENSE")
        db.categoryQueries.insert(null, "Segunda", "", 0, "EXPENSE")
        val segundaPadre = db.categoryQueries.selectAll().executeAsList().first { it.name == "Segunda" }
        db.categoryQueries.insert(segundaPadre.id, "SegundaSub", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val primera = cats.first { it.name == "PrimeraSub" }
        val segunda = cats.first { it.name == "SegundaSub" }

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

        val frequent = DashboardViewModel.computeFrequentExpenses(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
        )

        assertEquals(listOf("Segunda", "Primera"), frequent.map { it.name })
    }

    @Test
    fun computeSubscriptionMonthlyCostConListaVaciaEsCero() {
        assertEquals(0L, DashboardViewModel.computeSubscriptionMonthlyCost(emptyList()))
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

        val merged = DashboardViewModel.mergeOthersByPct(top, otros)

        assertEquals(listOf("Otros", "A", "B", "C"), merged.map { it.name })
    }

    @Test
    fun mergeOthersByPctSinOtrosDevuelveElTopSinCambios() {
        val top = listOf(CategorySlice(name = "A", amountMinor = 30, pct = 0.30f))
        assertEquals(top, DashboardViewModel.mergeOthersByPct(top, null))
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

        val frequent = DashboardViewModel.computeFrequentExpenses(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
        )

        assertEquals("Comida", frequent.first().name)
        assertEquals("Restaurantes", frequent.first().subcategoryName)
    }

    @Test
    fun computeCategoryBreakdownSubeSubcategoriaAPadreYSumaMonto() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Comida", "", 0, "EXPENSE")
        val parent = db.categoryQueries.selectAll().executeAsList().first { it.name == "Comida" }
        db.categoryQueries.insert(parent.id, "Restaurantes", "", 0xFF00FF00L, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val child = cats.first { it.name == "Restaurantes" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        db.transactionQueries.insert(
            account.id, child.id, 100, "USD", today.toEpochDays().toLong(), "cena", "EXPENSE", null, null, null, null,
        )

        val breakdown = DashboardViewModel.computeCategoryBreakdown(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
        )

        assertEquals(1, breakdown.size)
        val comida = breakdown.first()
        assertEquals("Comida", comida.name)
        assertEquals(100, comida.amountMinor)
        assertEquals(1, comida.subcategories.size)
        assertEquals("Restaurantes", comida.subcategories.first().name)
        assertEquals(100, comida.subcategories.first().amountMinor)
        assertEquals(1f, comida.subcategories.first().pct)
    }

    @Test
    fun computeCategoryBreakdownPctDeSubcategoriaEsRelativoAlTotalDelPadre() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Comida", "", 0, "EXPENSE")
        val parent = db.categoryQueries.selectAll().executeAsList().first { it.name == "Comida" }
        db.categoryQueries.insert(parent.id, "Restaurantes", "", 0, "EXPENSE")
        db.categoryQueries.insert(parent.id, "Cafe", "", 0, "EXPENSE")
        db.categoryQueries.insert(null, "Transporte", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val restaurantes = cats.first { it.name == "Restaurantes" }
        val cafe = cats.first { it.name == "Cafe" }
        val transporte = cats.first { it.name == "Transporte" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val epoch = today.toEpochDays().toLong()
        db.transactionQueries.insert(account.id, restaurantes.id, 300, "USD", epoch, "cena", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, cafe.id, 100, "USD", epoch, "cafe", "EXPENSE", null, null, null, null)
        db.transactionQueries.insert(account.id, transporte.id, 400, "USD", epoch, "bus", "EXPENSE", null, null, null, null)

        val breakdown = DashboardViewModel.computeCategoryBreakdown(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
        )

        val comida = breakdown.first { it.name == "Comida" }
        // 400 de 800 total general = 0.5
        assertEquals(0.5f, comida.pct)
        // dentro de Comida (400): Restaurantes 300/400=0.75, Cafe 100/400=0.25
        assertEquals(0.75f, comida.subcategories.first { it.name == "Restaurantes" }.pct)
        assertEquals(0.25f, comida.subcategories.first { it.name == "Cafe" }.pct)
    }

    @Test
    fun computeCategoryBreakdownCategoriaSinSubcategoriasDevuelveListaVacia() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Transporte", "", 0, "EXPENSE")
        val cats = db.categoryQueries.selectAll().executeAsList()
        val cat = cats.first { it.name == "Transporte" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        db.transactionQueries.insert(
            account.id, cat.id, 100, "USD", today.toEpochDays().toLong(), "bus", "EXPENSE", null, null, null, null,
        )

        val breakdown = DashboardViewModel.computeCategoryBreakdown(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
        )

        assertEquals(emptyList(), breakdown.first().subcategories)
    }

    @Test
    fun computeCategoryBreakdownConListaVaciaDevuelveVacio() {
        assertEquals(emptyList(), DashboardViewModel.computeCategoryBreakdown(expenseTxs = emptyList(), cats = emptyList()))
    }

    @Test
    fun computeCategoryBreakdownSuscripcionesSeDesglosanPorSuscripcionIndividual() {
        val db = freshDb()
        db.accountQueries.insert("Efectivo", "CASH", "USD", 0, 0, 0)
        val account = db.accountQueries.selectAll().executeAsList().first()
        db.categoryQueries.insert(null, "Suscripciones", "", 0, "EXPENSE")
        val parent = db.categoryQueries.selectAll().executeAsList().first { it.name == "Suscripciones" }
        db.categoryQueries.insert(parent.id, "Suscripción", "", 0, "EXPENSE")
        val leaf = db.categoryQueries.selectAll().executeAsList().first { it.name == "Suscripción" }

        db.subscriptionQueries.insert("Netflix", 500, "USD", leaf.id, account.id, "MONTHLY", 1, 0, 1, 1)
        val netflix = db.subscriptionQueries.selectAll().executeAsList().first { it.name == "Netflix" }
        db.subscriptionQueries.insert("Spotify", 300, "USD", leaf.id, account.id, "MONTHLY", 1, 0, 1, 1)
        val spotify = db.subscriptionQueries.selectAll().executeAsList().first { it.name == "Spotify" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val epoch = today.toEpochDays().toLong()
        db.transactionQueries.insert(account.id, leaf.id, 500, "USD", epoch, "Netflix", "EXPENSE", null, null, null, netflix.id)
        db.transactionQueries.insert(account.id, leaf.id, 300, "USD", epoch, "Spotify", "EXPENSE", null, null, null, spotify.id)

        val breakdown = DashboardViewModel.computeCategoryBreakdown(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = db.categoryQueries.selectAll().executeAsList(),
            subscriptions = db.subscriptionQueries.selectAll().executeAsList(),
        )

        val suscripciones = breakdown.first { it.name == "Suscripciones" }
        assertEquals(800, suscripciones.amountMinor)
        assertEquals(2, suscripciones.subcategories.size)
        assertEquals(500, suscripciones.subcategories.first { it.name == "Netflix" }.amountMinor)
        assertEquals(300, suscripciones.subcategories.first { it.name == "Spotify" }.amountMinor)
    }

    @Test
    fun computeFrequentExpensesSinSubcategoriaSeDescarta() {
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

        val frequent = DashboardViewModel.computeFrequentExpenses(
            expenseTxs = db.transactionQueries.selectAll().executeAsList(),
            cats = cats,
        )

        // Sin subcategoría (dato legado, previo a que fuera obligatoria) no cuenta como "gasto
        // hormiga" de subcategoría — se descarta en vez de aparecer como si fuera una.
        assertEquals(emptyList(), frequent)
    }
}
