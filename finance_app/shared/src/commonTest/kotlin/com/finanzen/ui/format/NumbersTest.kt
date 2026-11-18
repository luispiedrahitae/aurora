package com.finanzen.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumbersTest {
    @Test
    fun formatPercentSinDecimalesSobrantes() {
        assertEquals("9 %", formatPercent(0.09))
        assertEquals("1,5 %", formatPercent(0.015))
        assertEquals("−2,3 %", formatPercent(-0.023))
        assertEquals("22 %", formatPercent(0.2203, maxDecimals = 0))
    }

    @Test
    fun parsePercentAceptaComaYPunto() {
        assertEquals(0.015, parsePercent("1,5")!!, 1e-12)
        assertEquals(0.0075, parsePercent("0.75")!!, 1e-12)
        assertEquals(0.09, parsePercent(" 9 % ")!!, 1e-12)
        assertNull(parsePercent("abc"))
    }

    @Test
    fun percentFieldTextIdaYVuelta() {
        assertEquals("1,5", percentFieldText(0.015))
        assertEquals("9", percentFieldText(0.09))
        assertEquals(0.0125, parsePercent(percentFieldText(0.0125))!!, 1e-12)
    }

    @Test
    fun formatDecimalUnaCifra() {
        assertEquals("32,8", formatDecimal(32.846))
        assertEquals("65,0", formatDecimal(65.0))
        assertEquals("−9,5", formatDecimal(-9.53))
    }
}
