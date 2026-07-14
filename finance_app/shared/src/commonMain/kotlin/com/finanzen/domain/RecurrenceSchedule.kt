package com.finanzen.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.roundToLong

/**
 * Lógica pura de recurrencia (sin BD ni plataforma), compartida por Suscripciones e Inversiones
 * periódicas — ambas cobran/aportan según una [frequency] ("DAILY" | "MONTHLY") + intervalo.
 */
object RecurrenceSchedule {
    private const val AVG_DAYS_PER_MONTH = 30.437 // 365.25/12, para promediar cargos DAILY a un valor mensual estable

    /** Ocurrencias con fecha ≤ [todayEpochDay] ([dates], epoch days) y la nueva próxima fecha ([next]). */
    data class DueDates(val dates: List<Long>, val next: Long)

    /**
     * Costo mensual equivalente de una suscripción/recurrencia. MONTHLY cobra [amountMinor] una vez
     * al mes tal cual (ahí [interval] es el día del mes, no un multiplicador — ver [nextOccurrenceAfter]).
     * Cualquier otra frecuencia (hoy solo "DAILY") se promedia: amountMinor × días-promedio-por-mes ÷
     * intervalo-en-días.
     */
    fun monthlyEquivalent(amountMinor: Long, frequency: String, interval: Long): Long = if (frequency == "MONTHLY") {
        amountMinor
    } else {
        (amountMinor * AVG_DAYS_PER_MONTH / interval.coerceAtLeast(1)).roundToLong()
    }

    /**
     * Calcula, de forma pura, todas las ocurrencias con fecha ≤ [todayEpochDay] partiendo de
     * [firstEpochDay] y avanzando con [nextOccurrenceAfter], más la próxima fecha resultante.
     * Es la lógica del catch-up sin tocar la BD (testeable y sin bucle infinito: [nextOccurrenceAfter]
     * siempre avanza ≥1 día).
     */
    fun occurrencesDueUpTo(firstEpochDay: Long, todayEpochDay: Long, frequency: String, interval: Long): DueDates {
        val dates = mutableListOf<Long>()
        var next = firstEpochDay
        while (next <= todayEpochDay) {
            dates += next
            next = nextOccurrenceAfter(next, frequency, interval)
        }
        return DueDates(dates, next)
    }

    /**
     * Próxima ocurrencia **estrictamente posterior** a [fromEpochDay].
     * - DAILY: suma [interval] días (cada N días).
     * - MONTHLY: [interval] es el día del mes (1–31); devuelve su próxima ocurrencia, recortada en
     *   meses cortos (día 31 → 28/29 feb).
     */
    fun nextOccurrenceAfter(fromEpochDay: Long, frequency: String, interval: Long): Long {
        val from = LocalDate.fromEpochDays(fromEpochDay.toInt())
        if (frequency != "MONTHLY") {
            return from.plus(DatePeriod(days = interval.coerceAtLeast(1).toInt())).toEpochDays().toLong()
        }
        val day = interval.coerceIn(1, 31).toInt()
        var cand = dayInMonth(from.year, from.monthNumber, day)
        if (cand <= from) {
            val nm = LocalDate(from.year, from.monthNumber, 1).plus(DatePeriod(months = 1))
            cand = dayInMonth(nm.year, nm.monthNumber, day)
        }
        return cand.toEpochDays().toLong()
    }

    /** Día [day] del mes [m]/[y], recortado al último día si el mes es más corto. */
    private fun dayInMonth(y: Int, m: Int, day: Int): LocalDate {
        val first = LocalDate(y, m, 1)
        val daysInMonth = first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays()
        return LocalDate(y, m, minOf(day, daysInMonth))
    }
}
