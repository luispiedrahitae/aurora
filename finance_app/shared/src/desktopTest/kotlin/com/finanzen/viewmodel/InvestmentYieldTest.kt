package com.finanzen.viewmodel

import com.finanzen.viewmodel.InvestmentsViewModel.Companion.yieldMinor
import kotlin.test.Test
import kotlin.test.assertEquals

class InvestmentYieldTest {
    @Test
    fun gananciaEsPositiva() {
        assertEquals(20_000L, yieldMinor(withdrawnAmountMinor = 120_000L, contributedAmountMinor = 100_000L))
    }

    @Test
    fun perdidaEsNegativa() {
        assertEquals(-20_000L, yieldMinor(withdrawnAmountMinor = 80_000L, contributedAmountMinor = 100_000L))
    }

    @Test
    fun puntoDeEquilibrioEsCero() {
        assertEquals(0L, yieldMinor(withdrawnAmountMinor = 100_000L, contributedAmountMinor = 100_000L))
    }
}
