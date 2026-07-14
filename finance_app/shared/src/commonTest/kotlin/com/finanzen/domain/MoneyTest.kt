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

    @Test
    fun formatConAgrupacionAnglosajona() {
        // 1,234,567.89 — punto decimal, coma de miles (ej. USD)
        assertEquals("1,234,567.89", Money(123456789, "USD").format(2, ".", ","))
    }

    @Test
    fun formatConAgrupacionLatina() {
        // 1.234.567,89 — coma decimal, punto de miles (ej. EUR/COP)
        assertEquals("1.234.567,89", Money(123456789, "EUR").format(2, ",", "."))
    }

    @Test
    fun formatSinDecimalesConAgrupacion() {
        // JPY-like: sin decimales, con miles
        assertEquals("1,234,567", Money(1234567, "JPY").format(0, ".", ","))
    }

    @Test
    fun formatConLongMinValueNoDesborda() {
        // FIX: kotlin.math.abs(Long.MIN_VALUE) desborda (su magnitud no cabe en Long) y seguía dando
        // negativo, produciendo doble signo. Dividir/tomar el resto primero y recién ahí aplicar abs
        // evita el desborde (ni el cociente ni el resto de Long.MIN_VALUE llegan a ese extremo).
        assertEquals("-92233720368547758.08", Money(Long.MIN_VALUE, "USD").format(2))
    }
}
