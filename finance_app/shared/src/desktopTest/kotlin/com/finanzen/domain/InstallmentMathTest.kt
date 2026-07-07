package com.finanzen.domain

import kotlinx.datetime.LocalDate
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

    @Test
    fun installmentDueDateMismoDiaMesesSiguientes() {
        // epoch day 0 = 1970-01-01; cuota 1 cae el mismo día de 1970-02-01 (epoch day 31).
        assertEquals(0L, InstallmentMath.installmentDueDate(0, 0))
        assertEquals(31L, InstallmentMath.installmentDueDate(0, 1))
    }

    @Test
    fun installmentDueDateAcotaFinDeMesCorto() {
        // Compra el 31 de enero (2024, bisiesto): la cuota 1 no puede caer en 31 de febrero, se acota a 29.
        val start = LocalDate(2024, 1, 31).toEpochDays().toLong()
        val due1 = InstallmentMath.installmentDueDate(start, 1)
        assertEquals(LocalDate(2024, 2, 29), LocalDate.fromEpochDays(due1.toInt()))
    }

    @Test
    fun remainingCommitmentEsCeroSiSaldadaOCompleta() {
        assertEquals(0L, InstallmentMath.remainingCommitmentMinor(120_000, 12, 0.0, 0, 90, settled = true))
        // Ya transcurrieron las 12 cuotas.
        assertEquals(0L, InstallmentMath.remainingCommitmentMinor(120_000, 12, 0.0, 0, 100_000, settled = false))
    }

    @Test
    fun remainingCommitmentDescuentaLoYaFacturado() {
        // Día de la compra: cuota 1 ya facturada, quedan 11 por facturar.
        val expected = InstallmentMath.monthlyPaymentMinor(120_000, 12, 0.0) * 11
        assertEquals(expected, InstallmentMath.remainingCommitmentMinor(120_000, 12, 0.0, 0, 0, settled = false))
    }
}
