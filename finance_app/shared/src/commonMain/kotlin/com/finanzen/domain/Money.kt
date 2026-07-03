package com.finanzen.domain

import kotlin.jvm.JvmInline

/**
 * Cantidad monetaria expresada en la subunidad mínima de la moneda (centavos para USD/EUR/COP).
 * Evita errores de redondeo de Double. La conversión a string respeta los decimales declarados.
 */
data class Money(val amountMinor: Long, val currency: String) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return copy(amountMinor = amountMinor + other.amountMinor)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return copy(amountMinor = amountMinor - other.amountMinor)
    }

    fun format(decimals: Int = 2, decimalSeparator: String = ".", groupSeparator: String = ""): String {
        val sign = if (amountMinor < 0) "-" else ""
        val abs = kotlin.math.abs(amountMinor)
        val divisor = pow10(decimals)
        val whole = (abs / divisor).toString()
        val frac = (abs % divisor).toString().padStart(decimals, '0')
        val groupedWhole = if (groupSeparator.isEmpty()) {
            whole
        } else {
            whole.reversed().chunked(3).joinToString(groupSeparator).reversed()
        }
        return if (decimals == 0) "$sign$groupedWhole" else "$sign$groupedWhole$decimalSeparator$frac"
    }

    private fun pow10(n: Int): Long {
        var r = 1L
        repeat(n) { r *= 10 }
        return r
    }

    companion object {
        /** Convierte "12.50" / "12,50" → 1250 (subunidad). null si no es numérico válido. */
        fun parseToMinor(text: String, decimals: Int = 2): Long? {
            val cleaned = text.replace(',', '.').trim()
            if (cleaned.isEmpty()) return null
            val dot = cleaned.indexOf('.')
            var factor = 1L
            repeat(decimals) { factor *= 10 }
            return try {
                if (dot < 0) {
                    cleaned.toLong() * factor
                } else {
                    val whole = cleaned.substring(0, dot).ifEmpty { "0" }.toLong()
                    val frac = cleaned.substring(dot + 1).padEnd(decimals, '0').take(decimals).toLong()
                    whole * factor + frac
                }
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}

@JvmInline
value class CurrencyCode(val raw: String) {
    init {
        require(raw.length == 3) { "ISO 4217 expected, got '$raw'" }
    }
}
