package com.finanzen.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Cálculo de planes de cuotas. La tasa se interpreta como **anual nominal**; la mensual es tasa/12.
 * - Sin interés (tasa ≤ 0): división simple del total entre el número de cuotas.
 * - Con interés: amortización francesa (cuota fija) con la fórmula estándar de anualidades.
 *
 * `// ponytail:` modelo francés básico. Comisiones/seguros, redondeo banco-específico y el ajuste
 * de la última cuota por centavos se añaden cuando se afine el sistema; el total puede diferir en ±n centavos.
 */
object InstallmentMath {
    /** Valor de cada cuota (minor units). */
    fun monthlyPaymentMinor(totalMinor: Long, installments: Long, annualRatePct: Double): Long {
        if (installments <= 0) return 0
        if (annualRatePct <= 0.0) return totalMinor / installments
        val r = annualRatePct / 100.0 / 12.0
        val n = installments.toDouble()
        return (totalMinor * r / (1 - (1 + r).pow(-n))).roundToLong()
    }

    /** Total a pagar (cuota × número de cuotas); ≥ principal cuando hay interés. */
    fun totalWithInterestMinor(totalMinor: Long, installments: Long, annualRatePct: Double): Long = monthlyPaymentMinor(totalMinor, installments, annualRatePct) * installments

    /** Cuotas ya transcurridas: meses completos desde el inicio, acotado a [0, installments]. */
    fun elapsedInstallments(startEpochDay: Long, todayEpochDay: Long, installments: Long): Long {
        if (todayEpochDay < startEpochDay) return 0
        val start = LocalDate.fromEpochDays(startEpochDay.toInt())
        val today = LocalDate.fromEpochDays(todayEpochDay.toInt())
        val months = (today.year - start.year) * 12 + (today.monthNumber - start.monthNumber)
        return months.toLong().coerceIn(0, installments)
    }

    /** Fecha (epoch day) de la cuota [index] (0-based): mismo día de mes que el inicio, acotado a fin de mes. */
    fun installmentDueDate(startEpochDay: Long, index: Long): Long {
        val start = LocalDate.fromEpochDays(startEpochDay.toInt())
        val firstOfTargetMonth = LocalDate(start.year, start.monthNumber, 1).plus(DatePeriod(months = index.toInt()))
        val daysInTargetMonth = firstOfTargetMonth.plus(DatePeriod(months = 1)).toEpochDays() - firstOfTargetMonth.toEpochDays()
        val day = minOf(start.dayOfMonth, daysInTargetMonth)
        return LocalDate(firstOfTargetMonth.year, firstOfTargetMonth.monthNumber, day).toEpochDays().toLong()
    }

    /** Compromiso restante (aún no facturado) de un plan; 0 si está saldado antes de tiempo o ya completo. */
    fun remainingCommitmentMinor(
        totalMinor: Long,
        installments: Long,
        annualRatePct: Double,
        startEpochDay: Long,
        todayEpochDay: Long,
        settled: Boolean,
    ): Long {
        if (settled) return 0
        val posted = (elapsedInstallments(startEpochDay, todayEpochDay, installments) + 1).coerceAtMost(installments)
        val remaining = installments - posted
        if (remaining <= 0) return 0
        return monthlyPaymentMinor(totalMinor, installments, annualRatePct) * remaining
    }
}
