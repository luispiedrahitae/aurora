package com.finanzen.ui.theme

import com.finanzen.db.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatTest {
    private val usd = Currency("USD", "\$", 2, 1.0, "US Dollar", ".", ",")
    private val eur = Currency("EUR", "€", 2, 1.0, "Euro", ",", ".")

    @Test
    fun prefijoConMonedaConocida() {
        val fmt = MoneyFormat(SymbolPosition.PREFIX, mapOf("USD" to usd))
        assertEquals("\$1,234.56", fmt.format(123456, "USD"))
    }

    @Test
    fun sufijoConMonedaConocida() {
        val fmt = MoneyFormat(SymbolPosition.SUFFIX, mapOf("EUR" to eur))
        assertEquals("1.234,56 €", fmt.format(123456, "EUR"))
    }

    @Test
    fun sinSimboloIgnoraPosicion() {
        // Misma moneda (USD, sí tiene símbolo definido), pero position = NONE: el string no debe
        // llevar el símbolo aunque exista.
        val fmt = MoneyFormat(SymbolPosition.NONE, mapOf("USD" to usd))
        assertEquals("1,234.56", fmt.format(123456, "USD"))
    }

    @Test
    fun monedaDesconocidaUsaFallbackSinSimboloNiAgrupacion() {
        val fmt = MoneyFormat(SymbolPosition.PREFIX, mapOf("USD" to usd))
        // "XYZ" no está en el mapa: sin símbolo (sym vacío), separadores fallback "."/"" , decimals fallback 2
        assertEquals("12.34", fmt.format(1234, "XYZ"))
    }

    @Test
    fun decimalsPorDefectoUsaElDeLaMoneda() {
        val copSinDecimales = Currency("COP", "\$", 0, 1.0, "Colombian Peso", ",", ".")
        val fmt = MoneyFormat(SymbolPosition.PREFIX, mapOf("COP" to copSinDecimales))
        // sin pasar `decimals` explícito, debe usar el 0 de la moneda (no el default de la función,
        // que es 2): con decimals=0 el divisor es 1, así que amountMinor se trata como unidades
        // enteras (no centavos) y solo aplica agrupación de miles (punto, por groupSeparator="."):
        // 123456 -> "123.456" -> con símbolo prefijo -> "$123.456"
        assertEquals("\$123.456", fmt.format(123456, "COP"))
    }

    @Test
    fun signedAgregaSignoPositivoFueraDelSimbolo() {
        val fmt = MoneyFormat(SymbolPosition.PREFIX, mapOf("USD" to usd))
        assertEquals("+\$12.34", fmt.format(1234, "USD", signed = true))
    }
}
