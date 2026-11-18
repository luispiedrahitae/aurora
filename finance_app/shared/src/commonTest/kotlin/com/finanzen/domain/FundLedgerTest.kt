package com.finanzen.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class FundLedgerTest {
    @Test
    fun ignoraTransferenciasYTomaElAporteMasAntiguoComoInicial() {
        val movements = listOf(
            FundMovement(20, 1, 50_000, MovementKind.EXPENSE),
            FundMovement(10, 5, 100_000, MovementKind.EXPENSE),
            FundMovement(10, 6, 999, MovementKind.TRANSFER),
            FundMovement(30, 2, 30_000, MovementKind.INCOME),
        )
        assertEquals(FundTotals(initialMinor = 100_000, contributedMinor = 150_000, withdrawnMinor = 30_000), FundLedger.totals(movements))
    }

    @Test
    fun sinMovimientosTodoEsCero() {
        assertEquals(FundTotals(0, 0, 0), FundLedger.totals(emptyList()))
    }
}
