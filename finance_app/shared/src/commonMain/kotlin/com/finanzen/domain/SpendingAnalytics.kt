package com.finanzen.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Tipo de movimiento. En la BD se guarda como texto; la conversión vive solo en [fromDb]. */
enum class MovementKind {
    INCOME,
    EXPENSE,
    TRANSFER,
    ADJUSTMENT,
    ;

    companion object {
        fun fromDb(value: String): MovementKind = entries.firstOrNull { it.name == value } ?: EXPENSE
    }
}

/**
 * Movimiento visto por los cálculos de gasto, sin depender de la BD. [recurring] marca lo que llega
 * solo cada período (suscripción, aporte periódico a un fondo o cuota de un plan); lo demás es puntual.
 */
data class Movement(
    val date: LocalDate,
    val amountMinor: Long,
    val kind: MovementKind,
    val categoryId: Long?,
    val recurring: Boolean,
)

/** Totales de un tramo de fechas (día, semana o mes), ambos extremos incluidos. */
data class PeriodTotal(val start: LocalDate, val end: LocalDate, val incomeMinor: Long, val expenseMinor: Long)

/** Gasto recurrente frente a puntual en un período. */
data class RecurringSplit(val recurringMinor: Long, val recurringCount: Int, val oneOffMinor: Long, val oneOffCount: Int) {
    val totalMinor: Long get() = recurringMinor + oneOffMinor

    /** Fracción recurrente (0 si no hubo gasto). */
    val recurringFraction: Float get() = if (totalMinor == 0L) 0f else recurringMinor.toFloat() / totalMinor
}

/** Parte de una categoría en el gasto. [categoryId] nulo = "Otros" (todo lo que no entró en el top). */
data class CategoryShare(val categoryId: Long?, val amountMinor: Long, val fraction: Float)

/**
 * Cálculos de gasto diario para Tendencias, Resumen y Presupuesto. Todo es puro: recibe movimientos
 * y fechas y devuelve totales; no lee la BD ni el reloj.
 *
 * Reglas comunes: los gastos son solo [MovementKind.EXPENSE] y los ingresos solo
 * [MovementKind.INCOME]; transferencias y ajustes no cuentan como gasto ni ingreso.
 */
object SpendingAnalytics {

    /** Un total por cada día del mes de [month] (cualquier día del mes sirve de referencia). */
    fun dailyTotals(movements: List<Movement>, month: LocalDate): List<PeriodTotal> {
        val first = month.firstOfMonth()
        val byDay = movements.groupBy { it.date }
        return (0 until first.daysInMonth()).map { offset ->
            val day = first.plus(DatePeriod(days = offset))
            totalOf(day, day, byDay[day].orEmpty())
        }
    }

    /**
     * Semanas de calendario (lunes a domingo) que tocan el mes de [month], recortadas al mes: la primera
     * y la última pueden tener menos de 7 días. Cada total trae sus fechas reales para rotularlo
     * ("2–8 nov").
     */
    fun weeklyTotals(movements: List<Movement>, month: LocalDate): List<PeriodTotal> {
        val first = month.firstOfMonth()
        val last = first.plus(DatePeriod(days = first.daysInMonth() - 1))
        val weeks = mutableListOf<PeriodTotal>()
        var weekStart = first.minus(DatePeriod(days = first.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber))
        while (weekStart <= last) {
            val start = maxOf(weekStart, first)
            val end = minOf(weekStart.plus(DatePeriod(days = DAYS_PER_WEEK - 1)), last)
            weeks += totalOf(start, end, movements.filter { it.date in start..end })
            weekStart = weekStart.plus(DatePeriod(days = DAYS_PER_WEEK))
        }
        return weeks
    }

    /** Los [count] meses que terminan en el de [lastMonth], del más antiguo al más reciente. */
    fun monthlyTotals(movements: List<Movement>, lastMonth: LocalDate, count: Int): List<PeriodTotal> {
        val end = lastMonth.firstOfMonth()
        return (count - 1 downTo 0).map { back ->
            val start = end.minus(DatePeriod(months = back))
            val stop = start.plus(DatePeriod(days = start.daysInMonth() - 1))
            totalOf(start, stop, movements.filter { it.date in start..stop })
        }
    }

    /** Gasto recurrente frente a puntual entre [start] y [end] (incluidos). */
    fun recurringSplit(movements: List<Movement>, start: LocalDate, end: LocalDate): RecurringSplit {
        val expenses = movements.filter { it.kind == MovementKind.EXPENSE && it.date in start..end }
        val (recurring, oneOff) = expenses.partition { it.recurring }
        return RecurringSplit(recurring.sumOf { it.amountMinor }, recurring.size, oneOff.sumOf { it.amountMinor }, oneOff.size)
    }

    /**
     * Reparto del gasto por categoría entre [start] y [end], de mayor a menor. Se quedan las [top]
     * más grandes y el resto se agrupa en una fila "Otros" ([CategoryShare.categoryId] nulo).
     * [categoryOf] permite subir subcategorías a su categoría principal.
     */
    fun categoryBreakdown(
        movements: List<Movement>,
        start: LocalDate,
        end: LocalDate,
        top: Int,
        categoryOf: (Long?) -> Long? = { it },
    ): List<CategoryShare> {
        val expenses = movements.filter { it.kind == MovementKind.EXPENSE && it.date in start..end }
        val total = expenses.sumOf { it.amountMinor }
        if (total == 0L) return emptyList()
        val sorted = expenses.groupBy { categoryOf(it.categoryId) }
            .map { (id, list) -> id to list.sumOf { it.amountMinor } }
            .sortedByDescending { it.second }
        val shares = sorted.take(top).map { (id, amount) -> CategoryShare(id, amount, amount.toFloat() / total) }
        val rest = sorted.drop(top).sumOf { it.second }
        return if (rest > 0) shares + CategoryShare(null, rest, rest.toFloat() / total) else shares
    }

    /**
     * Gasto diario promedio del mes de [month] hasta [today] (si es el mes en curso) o del mes completo.
     * Devuelve 0 para meses futuros.
     */
    fun averageDailyExpense(movements: List<Movement>, month: LocalDate, today: LocalDate): Long {
        val first = month.firstOfMonth()
        val days = elapsedDays(first, today)
        if (days == 0) return 0
        val last = first.plus(DatePeriod(days = days - 1))
        return expenseBetween(movements, first, last) / days
    }

    /**
     * Promedio diario del mes anterior contando solo los mismos días que van del mes de [month]
     * (si hoy es 16, compara con los días 1–16 del mes anterior). 0 si el mes anterior no tuvo gasto.
     */
    fun previousMonthAverageSameDays(movements: List<Movement>, month: LocalDate, today: LocalDate): Long {
        val days = elapsedDays(month.firstOfMonth(), today)
        if (days == 0) return 0
        val prevFirst = month.firstOfMonth().minus(DatePeriod(months = 1))
        val comparable = minOf(days, prevFirst.daysInMonth())
        val prevLast = prevFirst.plus(DatePeriod(days = comparable - 1))
        return expenseBetween(movements, prevFirst, prevLast) / comparable
    }

    /** Proyección de gasto a fin de mes al ritmo actual; nula si el mes no es el de [today]. */
    fun projectMonthEnd(expenseSoFarMinor: Long, today: LocalDate, month: LocalDate): Long? {
        val first = month.firstOfMonth()
        if (today.year != first.year || today.month != first.month) return null
        return expenseSoFarMinor * first.daysInMonth() / today.day
    }

    /**
     * Cuánto se puede gastar por día para no pasarse: lo que queda del límite repartido entre los días
     * que faltan del mes, hoy incluido. Nulo si ya no queda nada o el mes no es el de [today].
     */
    fun dailyAllowance(limitMinor: Long, spentMinor: Long, today: LocalDate, month: LocalDate): Long? {
        val first = month.firstOfMonth()
        if (today.year != first.year || today.month != first.month) return null
        val left = limitMinor - spentMinor
        if (left <= 0) return null
        return left / (first.daysInMonth() - today.day + 1)
    }
}

private fun totalOf(start: LocalDate, end: LocalDate, list: List<Movement>) = PeriodTotal(
    start = start,
    end = end,
    incomeMinor = list.filter { it.kind == MovementKind.INCOME }.sumOf { it.amountMinor },
    expenseMinor = list.filter { it.kind == MovementKind.EXPENSE }.sumOf { it.amountMinor },
)

private fun expenseBetween(movements: List<Movement>, start: LocalDate, end: LocalDate): Long = movements.filter { it.kind == MovementKind.EXPENSE && it.date in start..end }.sumOf { it.amountMinor }

/** Días transcurridos del mes que empieza en [first] a [today]: todos si es pasado, 0 si es futuro. */
private fun elapsedDays(first: LocalDate, today: LocalDate): Int = when {
    today < first -> 0
    today.year == first.year && today.month == first.month -> today.day
    else -> first.daysInMonth()
}

/** Primer día del mes de esta fecha. */
fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

private const val DAYS_PER_WEEK = 7
