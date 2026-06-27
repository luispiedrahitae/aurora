package com.finanzen.domain

import kotlinx.datetime.LocalDate
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
}
