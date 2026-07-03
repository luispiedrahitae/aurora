package com.finanzen.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.finanzen.db.Currency
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
    val currencies: Map<String, Currency> = emptyMap(),
) {
    fun format(amountMinor: Long, currency: String, signed: Boolean = false, decimals: Int? = null): String {
        val row = currencies[currency]
        val effectiveDecimals = decimals ?: row?.decimals?.toInt() ?: 2
        val base = Money(amountMinor, currency).format(
            effectiveDecimals,
            row?.decimalSeparator ?: ".",
            row?.groupSeparator ?: "",
        )
        val withSign = if (signed && amountMinor > 0) "+$base" else base
        val sym = row?.symbol ?: ""
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
