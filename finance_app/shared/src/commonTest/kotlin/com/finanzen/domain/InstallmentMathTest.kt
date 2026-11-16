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

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun installmentDueDateAcotaFinDeMesCortoNoBisiesto() {
        // Compra el 31 de enero de 2026 (NO bisiesto): la cuota 1 se acota a 28 feb, no 29.
        // (installmentDueDateAcotaFinDeMesCorto ya cubre el caso bisiesto 2024; este cubre el no-bisiesto).
        val start = LocalDate(2026, 1, 31).toEpochDays().toLong()
        val due1 = InstallmentMath.installmentDueDate(start, 1)
        assertEquals(LocalDate(2026, 2, 28), LocalDate.fromEpochDays(due1.toInt()))
    }

    @Test
    fun installmentDueDateConIndiceNegativoRetrocedeMeses() {
        // index negativo no está documentado como caso de uso, pero no revienta: retrocede meses
        // igual que avanza. 31 ene 2024, index=-1 -> diciembre 2023 (31 días, sin recorte).
        val start = LocalDate(2024, 1, 31).toEpochDays().toLong()
        val due = InstallmentMath.installmentDueDate(start, -1)
        assertEquals(LocalDate(2023, 12, 31), LocalDate.fromEpochDays(due.toInt()))
    }

    @Test
    fun remainingCommitmentAMitadDePlazo() {
        // 12 cuotas de 10_000 sin interés; a los 5 meses transcurridos (6 ya facturadas), quedan 6.
        val start = LocalDate(2026, 1, 1).toEpochDays().toLong()
        val today = LocalDate(2026, 6, 1).toEpochDays().toLong()
        val expected = InstallmentMath.monthlyPaymentMinor(120_000, 12, 0.0) * 6
        assertEquals(expected, InstallmentMath.remainingCommitmentMinor(120_000, 12, 0.0, start, today, settled = false))
    }

    @Test
    fun monthlyPaymentNoDivisibleExactoPierdeCentavos_documentaBug() {
        // BUG conocido (marcado // ponytail: en InstallmentMath.kt): la división entera de un total no
        // divisible exacto entre las cuotas pierde el residuo. 100_000 / 3 = 33_333 * 3 = 99_999, un
        // centavo menos que el principal original. No hay ajuste de "última cuota" todavía.
        val monthly = InstallmentMath.monthlyPaymentMinor(100_000, 3, 0.0)
        assertEquals(33_333, monthly)
        assertEquals(99_999, InstallmentMath.totalWithInterestMinor(100_000, 3, 0.0))
    }
}
