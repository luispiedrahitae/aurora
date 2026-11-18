package com.finanzen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FundProjectionTest {

    // Ejemplo del diseño: 5.000.000 iniciales, 500.000 al mes, 10 años, comisión 1,5 % anual.
    private fun example(rate: Double, fee: Double = 0.015) = ProjectionInput(initialMinor = 5_000_000, monthlyMinor = 500_000, months = 120, annualRate = rate, adminFee = fee)

    @Test
    fun escenariosDelDisenoCoincidenAlPeso() {
        assertEquals(82_861_782, FundProjector.project(example(0.06)).finalBalanceMinor)
        assertEquals(97_846_374, FundProjector.project(example(0.09)).finalBalanceMinor)
        assertEquals(115_801_851, FundProjector.project(example(0.12)).finalBalanceMinor)
    }

    @Test
    fun aportadoYRendimiento() {
        val p = FundProjector.project(example(0.09))
        assertEquals(65_000_000, p.contributedMinor)
        assertEquals(32_846_374, p.yieldMinor)
    }

    @Test
    fun tablaAnoPorAno() {
        val yearly = FundProjector.project(example(0.09)).yearly()
        assertEquals(10, yearly.size)
        assertEquals(11_605_014, yearly[0].balanceMinor)
        assertEquals(26_310_247, yearly[2].balanceMinor)
        assertEquals(43_261_328, yearly[4].balanceMinor)
        assertEquals(23_000_000, yearly[2].contributedMinor)
    }

    @Test
    fun costoDeLasComisiones() {
        assertEquals(107_379_827, FundProjector.project(example(0.09, fee = 0.0)).finalBalanceMinor)
        assertEquals(9_533_453, FundProjector.feeCostMinor(example(0.09)))
        // Fondo B del diseño: más rentabilidad, más comisión.
        assertEquals(97_211_033, FundProjector.project(example(0.10, fee = 0.025)).finalBalanceMinor)
        assertEquals(16_486_479, FundProjector.feeCostMinor(example(0.10, fee = 0.025)))
    }

    @Test
    fun comparacionOrdenaPorSaldoFinal() {
        val result = FundProjector.compare(listOf("B" to example(0.10, fee = 0.025), "A" to example(0.09)))
        assertEquals(listOf("A", "B"), result.map { it.name })
        assertEquals(635_341, result[0].projection.finalBalanceMinor - result[1].projection.finalBalanceMinor)
    }

    @Test
    fun sinRentabilidadNiComisionesElSaldoEsLoAportado() {
        val p = FundProjector.project(ProjectionInput(1_000, 100, 24, annualRate = 0.0))
        assertEquals(3_400, p.finalBalanceMinor)
        assertEquals(0, p.yieldMinor)
        assertEquals(0, p.feesPaidMinor)
    }

    @Test
    fun comisionPorAporteSeDescuentaDeCadaAporte() {
        val p = FundProjector.project(ProjectionInput(1_000, 1_000, 1, annualRate = 0.0, contributionFee = 0.01))
        assertEquals(1_980, p.finalBalanceMinor)
        assertEquals(20, p.feesPaidMinor)
    }

    @Test
    fun comisionDeAdministracionEquivaleALaAnual() {
        // 1.000.000 sin aportes ni rendimiento, 1 año con 1 % de administración → 990.000.
        val p = FundProjector.project(ProjectionInput(1_000_000, 0, 12, annualRate = 0.0, adminFee = 0.01))
        assertEquals(990_000, p.finalBalanceMinor)
    }

    @Test
    fun plazoQueNoEsAnoCompletoIncluyeElUltimoMes() {
        val yearly = FundProjector.project(ProjectionInput(0, 100, 18, annualRate = 0.05)).yearly()
        assertEquals(listOf(12, 18), yearly.map { it.month })
    }

    @Test
    fun entradasInvalidas() {
        assertTrue(FundProjector.validate(ProjectionInput(0, 0, 12, 0.05)).isNotEmpty())
        assertTrue(FundProjector.validate(ProjectionInput(100, 0, 0, 0.05)).isNotEmpty())
        assertTrue(FundProjector.validate(ProjectionInput(100, 0, 12, 2.0)).isNotEmpty())
        assertTrue(FundProjector.validate(ProjectionInput(100, 0, 12, 0.05, adminFee = 0.5)).isNotEmpty())
        assertFailsWith<IllegalArgumentException> { FundProjector.project(ProjectionInput(-1, 0, 12, 0.05)) }
    }
}
