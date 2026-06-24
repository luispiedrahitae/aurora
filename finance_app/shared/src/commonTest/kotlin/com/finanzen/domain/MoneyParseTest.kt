package com.finanzen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyParseTest {
    @Test
    fun parseaEnteros() {
        assertEquals(1500L, Money.parseToMinor("15"))
        assertEquals(0L, Money.parseToMinor("0"))
    }

    @Test
    fun parseaDecimalesYComa() {
        assertEquals(1250L, Money.parseToMinor("12.50"))
        assertEquals(1250L, Money.parseToMinor("12,5"))
        assertEquals(9L, Money.parseToMinor("0.09"))
    }

    @Test
    fun truncaDecimalesExtra() {
        // "12.999" → 12 + 99 centavos (toma 2)
        assertEquals(1299L, Money.parseToMinor("12.999"))
    }

    @Test
    fun respetaDecimalesPersonalizados() {
        assertEquals(15L, Money.parseToMinor("15", decimals = 0))
    }

    @Test
    fun rechazaTextoInvalido() {
        assertNull(Money.parseToMinor(""))
        assertNull(Money.parseToMinor("abc"))
    }
}
