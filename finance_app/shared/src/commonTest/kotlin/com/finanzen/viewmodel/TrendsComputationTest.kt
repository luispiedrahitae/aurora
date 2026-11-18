package com.finanzen.viewmodel

import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrendsComputationTest {
    private val today = LocalDate(2026, 11, 16)
    private val categories = listOf(
        Category(1, null, "Vivienda", "", 0, "EXPENSE"),
        Category(2, 1, "Alquiler", "", 0, "EXPENSE"),
        Category(3, null, "Alimentación", "", 0, "EXPENSE"),
        Category(4, 3, "Supermercado", "", 0, "EXPENSE"),
    )
    private var nextId = 1L

    private fun tx(date: LocalDate, amount: Long, category: Long, currency: String = "COP", subscription: Long? = null, kind: String = "EXPENSE") = TransactionRow(nextId++, 1, category, amount, currency, date.toEpochDays(), "", kind, null, null, null, subscription)

    private val txs = listOf(
        tx(LocalDate(2026, 11, 1), 1_000, 2, subscription = 9), // recurrente: alquiler
        tx(LocalDate(2026, 11, 2), 300, 4),
        tx(LocalDate(2026, 11, 13), 500, 4),
        tx(LocalDate(2026, 11, 13), 999, 4, currency = "USD"), // otra moneda: fuera
        tx(LocalDate(2026, 11, 14), 2_000, 4, kind = "INCOME"), // ingreso: no es gasto
        tx(LocalDate(2026, 10, 5), 1_600, 4), // octubre
    )

    @Test
    fun barrasDiariasHastaHoySinRecurrentes() {
        val data = computeTrends(txs, categories, "COP", TrendGrouping.DAY, today, today)
        assertEquals(16, data.bars.size)
        assertEquals("Hoy", data.bars.last().axisLabel)
        assertEquals(500, data.bars[12].expenseMinor)
        assertEquals(0, data.bars[0].expenseMinor) // el alquiler del 1 es recurrente
        assertNull(data.bars[14].axisLabel) // el 15 no se rotula: está pegado a "Hoy"
        assertEquals(800L / 16, data.averageDailyMinor)
        assertEquals(1_600L / 16, data.previousAverageMinor)
        assertEquals("octubre", data.previousMonthName)
    }

    @Test
    fun repartoYCategoriasCuentanTodoElGasto() {
        val data = computeTrends(txs, categories, "COP", TrendGrouping.WEEK, today, today)
        assertEquals(1_000, data.split.recurringMinor)
        assertEquals(800, data.split.oneOffMinor)
        assertEquals(listOf("Vivienda", "Alimentación"), data.categories.map { it.name })
    }

    @Test
    fun semanasRotuladasConFechas() {
        val data = computeTrends(txs, categories, "COP", TrendGrouping.WEEK, today, today)
        // Noviembre 2026 empieza en domingo: la primera semana es solo el 1.
        assertEquals("1", data.bars.first().axisLabel)
        assertEquals("2–8", data.bars[1].axisLabel)
        assertEquals("16–22 nov", data.bars.last().label)
    }

    @Test
    fun seisMesesEnModoMes() {
        val data = computeTrends(txs, categories, "COP", TrendGrouping.MONTH, today, today)
        assertEquals(6, data.bars.size)
        assertEquals(1_600, data.bars[4].expenseMinor)
        assertEquals(800, data.bars[5].expenseMinor)
    }
}
