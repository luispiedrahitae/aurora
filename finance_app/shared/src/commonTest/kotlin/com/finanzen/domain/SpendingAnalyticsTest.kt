package com.finanzen.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpendingAnalyticsTest {

    private fun d(month: Int, day: Int, year: Int = 2026) = LocalDate(year, month, day)

    private fun expense(date: LocalDate, amount: Long, category: Long? = 1, recurring: Boolean = false) = Movement(date, amount, MovementKind.EXPENSE, category, recurring)

    private fun income(date: LocalDate, amount: Long) = Movement(date, amount, MovementKind.INCOME, 9, false)

    private val november = listOf(
        expense(d(11, 1), 100),
        expense(d(11, 2), 200, recurring = true),
        expense(d(11, 8), 50, category = 2),
        income(d(11, 15), 6_400),
        expense(d(11, 16), 300, category = 3),
        Movement(d(11, 16), 999, MovementKind.TRANSFER, null, false),
        expense(d(11, 30), 40, category = 2),
    )

    @Test
    fun totalesDiariosCubrenTodoElMesYNoCuentanTransferencias() {
        val days = SpendingAnalytics.dailyTotals(november, d(11, 10))
        assertEquals(30, days.size)
        assertEquals(300, days[15].expenseMinor)
        assertEquals(6_400, days[14].incomeMinor)
    }

    @Test
    fun semanasDeLunesADomingoRecortadasAlMes() {
        // Noviembre de 2026 empieza en domingo y termina en lunes.
        val weeks = SpendingAnalytics.weeklyTotals(november, d(11, 1))
        assertEquals(6, weeks.size)
        assertEquals(d(11, 1) to d(11, 1), weeks.first().start to weeks.first().end)
        assertEquals(d(11, 2) to d(11, 8), weeks[1].start to weeks[1].end)
        assertEquals(d(11, 30) to d(11, 30), weeks.last().start to weeks.last().end)
        assertEquals(listOf(100L, 250L, 0L, 300L, 0L, 40L), weeks.map { it.expenseMinor })
    }

    @Test
    fun totalesMensualesDelMasAntiguoAlMasReciente() {
        val months = SpendingAnalytics.monthlyTotals(november + expense(d(10, 20), 70), d(11, 5), count = 3)
        assertEquals(listOf(d(9, 1), d(10, 1), d(11, 1)), months.map { it.start })
        assertEquals(listOf(0L, 70L, 690L), months.map { it.expenseMinor })
        assertEquals(d(10, 31), months[1].end)
    }

    @Test
    fun recurrenteFrenteAPuntual() {
        val split = SpendingAnalytics.recurringSplit(november, d(11, 1), d(11, 30))
        assertEquals(200, split.recurringMinor)
        assertEquals(1, split.recurringCount)
        assertEquals(490, split.oneOffMinor)
        assertEquals(4, split.oneOffCount)
    }

    @Test
    fun repartoPorCategoriaConOtros() {
        val shares = SpendingAnalytics.categoryBreakdown(november, d(11, 1), d(11, 30), top = 2)
        assertEquals(listOf(1L, 3L, null), shares.map { it.categoryId })
        assertEquals(listOf(300L, 300L, 90L), shares.map { it.amountMinor })
        assertEquals(1f, shares.sumOf { it.fraction.toDouble() }.toFloat(), 0.0001f)
    }

    @Test
    fun promediosDiariosYComparacionConElMesAnterior() {
        val data = november + expense(d(10, 3), 160)
        // Hasta el 16 de noviembre: 100 + 200 + 50 + 300 = 650 en 16 días.
        assertEquals(40, SpendingAnalytics.averageDailyExpense(data, d(11, 1), today = d(11, 16)))
        // Octubre del 1 al 16: 160 en 16 días.
        assertEquals(10, SpendingAnalytics.previousMonthAverageSameDays(data, d(11, 1), today = d(11, 16)))
        // Mes pasado completo: 690 / 30.
        assertEquals(23, SpendingAnalytics.averageDailyExpense(november, d(11, 1), today = d(12, 5)))
        assertEquals(0, SpendingAnalytics.averageDailyExpense(november, d(12, 1), today = d(11, 5)))
    }

    @Test
    fun proyeccionYGastoPorDiaPermitido() {
        assertEquals(1_500, SpendingAnalytics.projectMonthEnd(800, today = d(11, 16), month = d(11, 1)))
        assertNull(SpendingAnalytics.projectMonthEnd(800, today = d(12, 1), month = d(11, 1)))
        // Quedan 1.500 y 15 días (del 16 al 30, hoy incluido).
        assertEquals(100, SpendingAnalytics.dailyAllowance(4_000, 2_500, today = d(11, 16), month = d(11, 1)))
        assertNull(SpendingAnalytics.dailyAllowance(1_000, 1_200, today = d(11, 16), month = d(11, 1)))
    }

    @Test
    fun tipoDesdeLaBd() {
        assertEquals(MovementKind.INCOME, MovementKind.fromDb("INCOME"))
        assertEquals(MovementKind.EXPENSE, MovementKind.fromDb("desconocido"))
    }
}
