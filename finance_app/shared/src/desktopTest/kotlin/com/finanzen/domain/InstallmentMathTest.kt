package com.finanzen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallmentMathTest {
    @Test
    fun sinInteresDivideElTotal() {
        // 120000 / 12 = 10000 por cuota; total = principal.
        assertEquals(10_000, InstallmentMath.monthlyPaymentMinor(120_000, 12, 0.0))
        assertEquals(120_000, InstallmentMath.totalWithInterestMinor(120_000, 12, 0.0))
    }

    @Test
    fun conInteresLaCuotaYElTotalSuben() {
        val principal = 120_000L
        val monthly = InstallmentMath.monthlyPaymentMinor(principal, 12, 24.0)
        assertTrue(monthly > principal / 12, "la cuota con interés debe superar la cuota sin interés")
        assertTrue(InstallmentMath.totalWithInterestMinor(principal, 12, 24.0) > principal)
    }

    @Test
    fun cuotasTranscurridasContamMeses() {
        // 0 = 1970-01-01. 90 días ≈ 3 meses (abril); meses completos = 3.
        assertEquals(0, InstallmentMath.elapsedInstallments(0, 0, 12))
        assertEquals(3, InstallmentMath.elapsedInstallments(0, 90, 12))
        // Nunca excede el número de cuotas.
        assertEquals(12, InstallmentMath.elapsedInstallments(0, 100_000, 12))
    }
}
