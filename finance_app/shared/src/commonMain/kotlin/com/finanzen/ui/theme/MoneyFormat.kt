package com.finanzen.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.finanzen.domain.Money

/** Dónde va el símbolo de moneda respecto al número. */
enum class SymbolPosition { PREFIX, SUFFIX, NONE }

/**
 * Formato de moneda global elegido por el usuario (Más › Moneda). El símbolo (`$`, `€`) sale de
 * `Currency.symbol`; la posición es una preferencia. Centraliza el formateo para que TODO valor
 * monetario de la app respete la misma opción. El signo `-`/`+` queda por fuera del símbolo.
 */
data class MoneyFormat(
    val position: SymbolPosition = SymbolPosition.PREFIX,
    val symbols: Map<String, String> = emptyMap(),
) {
    fun format(amountMinor: Long, currency: String, signed: Boolean = false, decimals: Int = 2): String {
        val base = Money(amountMinor, currency).format(decimals)
        val withSign = if (signed && amountMinor > 0) "+$base" else base
        val sym = symbols[currency] ?: ""
        if (position == SymbolPosition.NONE || sym.isEmpty()) return withSign
        return if (position == SymbolPosition.SUFFIX) {
            "$withSign $sym"
        } else {
            when {
                withSign.startsWith("-") -> "-$sym${withSign.drop(1)}"
                withSign.startsWith("+") -> "+$sym${withSign.drop(1)}"
                else -> "$sym$withSign"
            }
        }
    }

    companion object {
        fun parse(raw: String): SymbolPosition = when (raw.lowercase()) {
            "suffix" -> SymbolPosition.SUFFIX
            "none" -> SymbolPosition.NONE
            else -> SymbolPosition.PREFIX
        }
    }
}

val LocalMoneyFormat = staticCompositionLocalOf { MoneyFormat() }
