package com.finanzen.viewmodel

import com.finanzen.db.TransactionRow
import com.finanzen.viewmodel.InvestmentsViewModel.Companion.fundTotals
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

    @Test
    fun retirosParcialesSeAcumulanYCuentanEnElRendimientoAlCerrar() {
        fun tx(kind: String, amount: Long, date: Long) = TransactionRow(0, 1, 1, amount, "USD", date, "", kind, null, null, 7, null)
        // Desordenados a propósito: el inicial es el aporte de fecha más antigua, no el primero de la lista.
        val txs = listOf(tx("EXPENSE", 50_000, 20), tx("EXPENSE", 100_000, 10), tx("INCOME", 30_000, 30), tx("INCOME", 140_000, 40))
        val totals = fundTotals(txs)
        assertEquals(InvestmentsViewModel.FundTotals(initialMinor = 100_000, contributedMinor = 150_000, withdrawnMinor = 170_000), totals)
        assertEquals(20_000L, yieldMinor(totals.withdrawnMinor, totals.contributedMinor))
    }
}
