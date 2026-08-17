package com.finanzen.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.ReportLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportBuilderTest {
    private fun freshDb(): FinanzenDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanzenDb.Schema.create(driver)
        return FinanzenDb(driver)
    }

    private fun rowsOfSection(lines: List<ReportLine>, title: String): List<ReportLine.Row> = lines
        .dropWhile { it != ReportLine.Section(title) }
        .drop(1)
        .takeWhile { it !is ReportLine.Section }
        .filterIsInstance<ReportLine.Row>()

    @Test
    fun transactionsCsvIncluyeSoloLasFilasRecibidas() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        TransactionRepository(db).add(account.id, cat.id, 5_000, account.currency, 100, "café", "EXPENSE")
        TransactionRepository(db).add(account.id, cat.id, 8_000, account.currency, 200, "cena", "EXPENSE")

        // El caller pre-filtra: aquí solo pasamos la primera fila.
        val onlyFirst = db.transactionQueries.selectAll().executeAsList().filter { it.note == "café" }
        val csv = ReportBuilder.transactionsCsv(
            onlyFirst,
            db.categoryQueries.selectAll().executeAsList(),
            db.accountQueries.selectAll().executeAsList().associate { it.id to it.name },
        )
        val dataLines = csv.trim().lines().drop(1)
        assertEquals(1, dataLines.size)
        assertTrue(dataLines.first().contains("café"))
    }

    @Test
    fun transactionsCsvSeparaCategoriaYSubcategoriaEnColumnasPropias() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val catRepo = CategoryRepository(db)
        catRepo.add("Ocio", "EXPENSE", parentId = null) // crea "Ocio" + subcategoría "General"
        val ocio = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Ocio" }
        val streaming = catRepo.addAndGetId("Streaming", "EXPENSE", parentId = ocio.id)
        TransactionRepository(db).add(account.id, streaming, 5_000, account.currency, 100, "netflix", "EXPENSE")

        val csv = ReportBuilder.transactionsCsv(
            db.transactionQueries.selectAll().executeAsList(),
            db.categoryQueries.selectAll().executeAsList(),
            db.accountQueries.selectAll().executeAsList().associate { it.id to it.name },
        )
        assertEquals("id,date,account,category,subcategory,kind,amount,currency,note", csv.lines().first())
        val dataRow = csv.trim().lines().drop(1).first { it.contains("netflix") }
        assertTrue(dataRow.contains("Ocio"))
        assertTrue(dataRow.contains("Streaming"))
    }

    @Test
    fun periodSummaryAnidaSubcategoriasBajoSuCategoriaConPorcentajesPropios() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val catRepo = CategoryRepository(db)

        catRepo.add("Comida", "EXPENSE", parentId = null) // crea "Comida" + subcategoría "General"
        val comida = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Comida" }
        val comidaGeneral = catRepo.children(comida.id).first { it.name == "General" }
        val comidaSnacks = catRepo.addAndGetId("Snacks", "EXPENSE", parentId = comida.id)

        catRepo.add("Transporte", "EXPENSE", parentId = null)
        val transporte = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Transporte" }
        val transporteGeneral = catRepo.children(transporte.id).first { it.name == "General" }

        val txRepo = TransactionRepository(db)
        txRepo.add(account.id, comidaGeneral.id, 3_000, account.currency, 100, "super", "EXPENSE")
        txRepo.add(account.id, comidaSnacks, 7_000, account.currency, 100, "snacks", "EXPENSE")
        txRepo.add(account.id, transporteGeneral.id, 5_000, account.currency, 100, "bus", "EXPENSE")
        // Total: 15_000 → Comida 10_000 (66%), Transporte 5_000 (33%).

        val lines = ReportBuilder.periodSummaryPdfLines(
            periodLabel = "Agosto 2026",
            rows = db.transactionQueries.selectAll().executeAsList(),
            categories = db.categoryQueries.selectAll().executeAsList(),
            currency = account.currency,
            budgetRows = emptyList(),
            netWorthMinor = 0L,
            openInvestments = emptyList(),
        )

        val expenseRows = rowsOfSection(lines, "Gastos por categoría")

        val comidaRow = expenseRows.first { it.label == "Comida" }
        assertTrue(comidaRow.emphasis) // la fila de categoría (padre) va en negrita
        assertTrue(comidaRow.value.contains("66%"))

        val transporteRow = expenseRows.first { it.label == "Transporte" }
        assertTrue(transporteRow.value.contains("33%"))

        // Comida (10_000) va antes que Transporte (5_000): orden por gasto total de la categoría.
        assertTrue(expenseRows.indexOf(comidaRow) < expenseRows.indexOf(transporteRow))

        // Las subcategorías van indentadas bajo su categoría, con % relativo al total de ESA
        // categoría (no del total general): Snacks 7000/10000 = 70%, General 3000/10000 = 30%.
        val comidaIdx = expenseRows.indexOf(comidaRow)
        val transporteIdx = expenseRows.indexOf(transporteRow)
        val comidaChildren = expenseRows.subList(comidaIdx + 1, transporteIdx)
        assertTrue(comidaChildren.any { it.label.contains("Snacks") && it.value.contains("70%") && !it.emphasis })
        assertTrue(comidaChildren.any { it.label.contains("General") && it.value.contains("30%") })
    }

    @Test
    fun periodSummarySeparaIngresosDeGastosPorCategoria() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val catRepo = CategoryRepository(db)
        catRepo.add("Comida", "EXPENSE", parentId = null)
        val expenseLeaf = catRepo.children(db.categoryQueries.selectByKind("EXPENSE").executeAsList().first { it.name == "Comida" }.id).first()
        catRepo.add("Sueldo", "INCOME", parentId = null)
        val incomeLeaf = catRepo.children(db.categoryQueries.selectByKind("INCOME").executeAsList().first { it.name == "Sueldo" }.id).first()

        val txRepo = TransactionRepository(db)
        txRepo.add(account.id, expenseLeaf.id, 4_000, account.currency, 100, "gasto", "EXPENSE")
        txRepo.add(account.id, incomeLeaf.id, 9_000, account.currency, 100, "sueldo", "INCOME")

        val lines = ReportBuilder.periodSummaryPdfLines(
            periodLabel = "Agosto 2026",
            rows = db.transactionQueries.selectAll().executeAsList(),
            categories = db.categoryQueries.selectAll().executeAsList(),
            currency = account.currency,
            budgetRows = emptyList(),
            netWorthMinor = 0L,
            openInvestments = emptyList(),
        )

        val incomeRows = rowsOfSection(lines, "Ingresos por categoría")
        val expenseRows = rowsOfSection(lines, "Gastos por categoría")
        assertTrue(incomeRows.any { it.label == "Sueldo" && it.value.contains("100%") })
        assertTrue(expenseRows.any { it.label == "Comida" && it.value.contains("100%") })
        // Cada sección solo lista sus propias categorías, no se mezclan.
        assertTrue(incomeRows.none { it.label == "Comida" })
        assertTrue(expenseRows.none { it.label == "Sueldo" })
    }

    @Test
    fun periodSummaryMarcaPresupuestosExcedidosConEnfasis() {
        val lines = ReportBuilder.periodSummaryPdfLines(
            periodLabel = "2026",
            rows = emptyList(),
            categories = emptyList(),
            currency = "USD",
            budgetRows = listOf(
                BudgetSummaryRow(categoryName = "Comida", parentName = null, limitMinor = 10_000, spentMinor = 15_000),
                BudgetSummaryRow(categoryName = "Transporte", parentName = null, limitMinor = 20_000, spentMinor = 5_000),
            ),
            netWorthMinor = 0L,
            openInvestments = emptyList(),
        )

        val budgetRows = rowsOfSection(lines, "Presupuestos del periodo")

        val comida = budgetRows.first { it.label.startsWith("Comida") }
        assertTrue(comida.label.endsWith("⚠"))
        assertTrue(!comida.value.contains("⚠"))
        assertTrue(!comida.value.contains("EXCEDIDO"))
        assertTrue(comida.emphasis)

        val transporte = budgetRows.first { it.label.startsWith("Transporte") }
        assertTrue(!transporte.label.contains("⚠"))
        assertTrue(!transporte.emphasis)

        // La leyenda del símbolo solo aparece si algún presupuesto se excedió.
        assertTrue(lines.any { it is ReportLine.Row && it.label.contains("⚠") && it.label.contains("Presupuesto excedido") })
    }

    @Test
    fun periodSummaryPresupuestosAgrupaCategoriasAntesQueSubcategorias() {
        val lines = ReportBuilder.periodSummaryPdfLines(
            periodLabel = "2026",
            rows = emptyList(),
            categories = emptyList(),
            currency = "USD",
            budgetRows = listOf(
                // La subcategoría gasta más, pero las categorías (nivel superior) deben listarse
                // primero de todas formas — el formato "Ocio" / "Ocio › Streaming" no cambia.
                BudgetSummaryRow(categoryName = "Streaming", parentName = "Ocio", limitMinor = 10_000, spentMinor = 9_000),
                BudgetSummaryRow(categoryName = "Ocio", parentName = null, limitMinor = 5_000, spentMinor = 1_000),
                BudgetSummaryRow(categoryName = "Comida", parentName = null, limitMinor = 5_000, spentMinor = 500),
            ),
            netWorthMinor = 0L,
            openInvestments = emptyList(),
        )

        val budgetRows = rowsOfSection(lines, "Presupuestos del periodo")
        val ocioIdx = budgetRows.indexOfFirst { it.label == "Ocio" }
        val comidaIdx = budgetRows.indexOfFirst { it.label == "Comida" }
        val streamingIdx = budgetRows.indexOfFirst { it.label == "Ocio › Streaming" }
        assertTrue(ocioIdx >= 0 && comidaIdx >= 0 && streamingIdx >= 0)
        assertTrue(ocioIdx < streamingIdx)
        assertTrue(comidaIdx < streamingIdx)
    }

    @Test
    fun periodSummaryNoIncluyeLeyendaDeExcedidoSiNingunPresupuestoSePaso() {
        val lines = ReportBuilder.periodSummaryPdfLines(
            periodLabel = "2026",
            rows = emptyList(),
            categories = emptyList(),
            currency = "USD",
            budgetRows = listOf(
                BudgetSummaryRow(categoryName = "Transporte", parentName = null, limitMinor = 20_000, spentMinor = 5_000),
            ),
            netWorthMinor = 0L,
            openInvestments = emptyList(),
        )

        assertTrue(lines.none { it is ReportLine.Row && it.label.contains("⚠") })
    }

    @Test
    fun periodSummaryIncluyePatrimonioNetoEInversionesAbiertas() {
        val db = freshDb()
        seedIfEmpty(db)
        val account = db.accountQueries.selectAll().executeAsList().first()
        val cat = db.categoryQueries.selectByKind("EXPENSE").executeAsList().first()
        InvestmentRepository(db).add(
            name = "Fondo indexado",
            amountMinor = 100_000,
            currency = account.currency,
            accountId = account.id,
            categoryId = cat.id,
            periodic = false,
            frequency = null,
            intervalCount = null,
            nextContributionDate = null,
            startDate = 0,
        )

        val lines = ReportBuilder.periodSummaryPdfLines(
            periodLabel = "2026",
            rows = emptyList(),
            categories = db.categoryQueries.selectAll().executeAsList(),
            currency = account.currency,
            budgetRows = emptyList(),
            netWorthMinor = 42_000,
            openInvestments = InvestmentRepository(db).openNow(),
        )

        assertTrue(lines.any { it is ReportLine.Row && it.label.contains("Patrimonio neto") && it.value.contains("42") })
        assertTrue(lines.any { it is ReportLine.Row && it.label == "Fondo indexado" })
        assertTrue(lines.any { it is ReportLine.Row && it.label == "Total invertido" })
    }
}
