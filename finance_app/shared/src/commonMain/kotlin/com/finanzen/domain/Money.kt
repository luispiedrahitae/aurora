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
        val divisor = pow10(decimals)
        // kotlin.math.abs(Long.MIN_VALUE) desborda (su magnitud no cabe en Long) y sigue siendo
        // negativo. Dividir/restar primero y recién ahí tomar abs evita el desborde: ni el cociente
        // ni el resto de Long.MIN_VALUE llegan a ese extremo.
        val whole = kotlin.math.abs(amountMinor / divisor).toString()
        val frac = kotlin.math.abs(amountMinor % divisor).toString().padStart(decimals, '0')
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
        /**
         * Convierte "12.50" / "12,50" / "1,234.56" / "-12.50" → subunidad (1250 / 1250 / 123456 /
         * -1250). El separador decimal es el que aparece más a la derecha (si hay coma Y punto, el
         * otro es agrupador de miles y se descarta); el signo se aplica al resultado completo, no
         * solo a la parte entera. Con `decimals == 0` la parte fraccionaria se trunca en vez de
         * lanzar. null si no queda ningún dígito válido.
         */
        fun parseToMinor(text: String, decimals: Int = 2): Long? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            val negative = trimmed.startsWith("-")
            val unsigned = trimmed.removePrefix("-").removePrefix("+")
            if (unsigned.isEmpty()) return null

            val lastDot = unsigned.lastIndexOf('.')
            val lastComma = unsigned.lastIndexOf(',')
            val decimalSepIndex = when {
                lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
                lastDot >= 0 -> lastDot
                lastComma >= 0 -> lastComma
                else -> -1
            }

            var factor = 1L
            repeat(decimals) { factor *= 10 }

            return try {
                val magnitude = if (decimalSepIndex < 0) {
                    val digitsOnly = unsigned.filter { it.isDigit() }
                    if (digitsOnly.isEmpty()) return null
                    digitsOnly.toLong() * factor
                } else {
                    val wholeDigits = unsigned.substring(0, decimalSepIndex).filter { it.isDigit() }.ifEmpty { "0" }
                    val fracDigits = unsigned.substring(decimalSepIndex + 1).filter { it.isDigit() }
                    val fracValue = if (decimals == 0) 0L else fracDigits.padEnd(decimals, '0').take(decimals).toLong()
                    wholeDigits.toLong() * factor + fracValue
                }
                if (negative) -magnitude else magnitude
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
