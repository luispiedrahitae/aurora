package com.finanzen.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NiceTicksTest {
    @Test
    fun cubreElMaximoConPasosRedondos() {
        assertEquals(listOf(0.0, 50.0, 100.0, 150.0), niceTicks(115.8))
        assertEquals(listOf(0.0, 100.0, 200.0, 300.0), niceTicks(290.0))
    }

    @Test
    fun maximoCeroDaUnRangoMinimo() {
        assertEquals(listOf(0.0, 1.0), niceTicks(0.0))
    }

    @Test
    fun siempreEmpiezaEnCeroYSuperaElMaximo() {
        listOf(0.37, 7.0, 12.5, 999.0, 1_234_567.0).forEach { max ->
            val ticks = niceTicks(max)
            assertEquals(0.0, ticks.first())
            assertTrue(ticks.last() >= max, "max=$max ticks=$ticks")
        }
    }
}
