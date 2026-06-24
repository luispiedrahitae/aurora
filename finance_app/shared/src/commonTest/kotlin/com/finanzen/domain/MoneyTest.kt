package com.finanzen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    @Test
    fun sumaMismaMoneda() {
        val a = Money(1234, "USD")
        val b = Money(2766, "USD")
        assertEquals(Money(4000, "USD"), a + b)
    }

    @Test
    fun restaMismaMoneda() {
        val a = Money(5000, "USD")
        val b = Money(1500, "USD")
        assertEquals(Money(3500, "USD"), a - b)
    }

    @Test
    fun monedasDistintasFallan() {
        assertFailsWith<IllegalArgumentException> {
            Money(100, "USD") + Money(100, "EUR")
        }
    }

    @Test
    fun formatDosDecimales() {
        assertEquals("12.34", Money(1234, "USD").format(2))
        assertEquals("-0.05", Money(-5, "USD").format(2))
        assertEquals("0.00", Money(0, "USD").format(2))
    }

    @Test
    fun formatSinDecimales() {
        assertEquals("12345", Money(12345, "JPY").format(0))
    }

    @Test
    fun currencyCodeValida() {
        CurrencyCode("USD")
        assertFailsWith<IllegalArgumentException> { CurrencyCode("US") }
        assertFailsWith<IllegalArgumentException> { CurrencyCode("USDD") }
    }
}
