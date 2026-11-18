package com.finanzen.ui.screens

import com.finanzen.domain.FundPlan
import com.finanzen.domain.RateMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FundFormStateTest {
    private val base = FundPlan(id = 7, name = "", currency = "COP", initialMinor = 0, monthlyMinor = 0, months = 120)

    private fun form() = FundFormState.from(base).copy(name = "Fondo A", initialMinor = 500_000_000, monthlyMinor = 50_000_000, adminFee = "1,5")

    @Test
    fun convierteAniosYPorcentajes() {
        val (plan, errors) = form().toPlan(base)
        assertTrue(errors.isEmpty())
        assertNotNull(plan)
        assertEquals(7L, plan.id)
        assertEquals(120, plan.months)
        assertEquals(0.015, plan.adminFee, 1e-12)
        assertEquals(0.09, plan.moderateRate, 1e-12)
        assertEquals(RateMode.SCENARIOS, plan.rateMode)
    }

    @Test
    fun porcentajeInvalidoDaError() {
        val (plan, errors) = form().copy(aggressive = "1,2,3").toPlan(base)
        assertNull(plan)
        assertEquals(1, errors.size)
    }

    @Test
    fun plazoVacioDaError() {
        val (plan, errors) = form().copy(years = "").toPlan(base)
        assertNull(plan)
        assertTrue(errors.single().contains("plazo"))
    }

    @Test
    fun sinNombreNoSeSimula() {
        val (plan, errors) = form().copy(name = "  ").toPlan(base)
        assertNull(plan)
        assertTrue(errors.isNotEmpty())
    }
}
