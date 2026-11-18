package com.finanzen.domain

/** Movimiento real de un fondo: aporte ([MovementKind.EXPENSE]) o retiro ([MovementKind.INCOME]). */
data class FundMovement(val epochDay: Long, val id: Long, val amountMinor: Long, val kind: MovementKind)

/** [initialMinor] = aporte más antiguo; [contributedMinor] = total aportado (incluye el inicial). */
data class FundTotals(val initialMinor: Long, val contributedMinor: Long, val withdrawnMinor: Long)

/** Cuentas de un fondo real (no simulado): cuánto se aportó, cuánto se retiró y lo que rindió. */
object FundLedger {

    /** Aportado (gastos) y retirado acumulado (ingresos) de los movimientos de un fondo. */
    fun totals(movements: List<FundMovement>): FundTotals {
        val contributions = movements.filter { it.kind == MovementKind.EXPENSE }
        return FundTotals(
            initialMinor = contributions.minWithOrNull(compareBy({ it.epochDay }, { it.id }))?.amountMinor ?: 0L,
            contributedMinor = contributions.sumOf { it.amountMinor },
            withdrawnMinor = movements.filter { it.kind == MovementKind.INCOME }.sumOf { it.amountMinor },
        )
    }

    /** Rendimiento total: retirado − aportado. Puede ser negativo (pérdida). */
    fun yieldMinor(withdrawnMinor: Long, contributedMinor: Long): Long = withdrawnMinor - contributedMinor
}
