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

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun separadorDeMilesConDecimalDaElResultadoCorrecto() {
        // FIX: el separador decimal es el que aparece más a la derecha ('.' en "1,234.56"); la coma
        // anterior se trata como agrupador de miles y se descarta.
        assertEquals(123456L, Money.parseToMinor("1,234.56"))
        // Formato latino: coma decimal, punto de miles.
        assertEquals(123456L, Money.parseToMinor("1.234,56"))
    }

    @Test
    fun montoNegativoAplicaElSignoAlResultadoCompleto() {
        // FIX: el signo se aplica al resultado completo (whole*factor + frac), no solo a la parte
        // entera -> "-12.50" da -1250, no -1150.
        assertEquals(-1250L, Money.parseToMinor("-12.50"))
    }

    @Test
    fun decimalsCeroConTextoFraccionarioTruncaEnVezDeDevolverNull() {
        // FIX: con decimals=0 la parte fraccionaria se descarta en vez de intentar parsear un string
        // vacío (que antes lanzaba y devolvía null).
        assertEquals(12L, Money.parseToMinor("12.50", decimals = 0))
    }
}
