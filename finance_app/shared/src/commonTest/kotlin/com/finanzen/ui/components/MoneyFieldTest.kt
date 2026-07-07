package com.finanzen.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFieldTest {
    @Test
    fun digitosSimplesSonElAmountMinorDirecto() {
        // Estilo calculadora: los decimales de la moneda solo afectan cómo se *muestra*, no cómo se
        // interpreta lo tecleado — "300000" siempre es amountMinor=300000, sea COP (0 decimales) o
        // USD (2 decimales); la diferencia la hace el formateo, no el parseo.
        assertEquals(300000L, digitsToMinor("300000"))
    }

    @Test
    fun ignoraSeparadoresDelTextoFormateado() {
        // El texto mostrado por MoneyField ya viene con separadores (ej. "300.000" o "3,000.00");
        // digitsToMinor debe quedarse solo con los dígitos.
        assertEquals(300000L, digitsToMinor("300.000"))
        assertEquals(300000L, digitsToMinor("3,000.00"))
    }

    @Test
    fun textoVacioOSinDigitosEsCero() {
        assertEquals(0L, digitsToMinor(""))
        assertEquals(0L, digitsToMinor("abc"))
    }
}
