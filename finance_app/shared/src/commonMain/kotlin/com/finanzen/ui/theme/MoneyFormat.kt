package com.finanzen.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.finanzen.data.CURRENCY_LOCALE_INFO
import com.finanzen.data.DEFAULT_LOCALE_INFO
import com.finanzen.db.Currency
import com.finanzen.domain.Money
import com.finanzen.domain.SymbolPosition

/**
 * Formato de moneda global elegido por el usuario (Más › Moneda). El símbolo (`$`, `€`) sale de
 * `Currency.symbol`; la posición es una preferencia (o AUTO, derivada de la convención real de
 * cada moneda vía [CURRENCY_LOCALE_INFO]). Centraliza el formateo para que TODO valor monetario
 * de la app respete la misma opción. El signo `-`/`+` queda por fuera del símbolo.
 */
data class MoneyFormat(
    val position: SymbolPosition = SymbolPosition.AUTO,
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
        val effectivePosition = resolvePosition(currency)
        if (effectivePosition == SymbolPosition.NONE || sym.isEmpty()) return withSign
        return if (effectivePosition == SymbolPosition.SUFFIX) {
            "$withSign $sym"
        } else {
            when {
                withSign.startsWith("-") -> "-$sym${withSign.drop(1)}"
                withSign.startsWith("+") -> "+$sym${withSign.drop(1)}"
                else -> "$sym$withSign"
            }
        }
    }

    /** Resuelve AUTO a la posición real de [currency] según CLDR; deja los overrides manuales tal cual. */
    fun resolvePosition(currency: String): SymbolPosition = if (position == SymbolPosition.AUTO) {
        CURRENCY_LOCALE_INFO[currency]?.symbolPosition ?: SymbolPosition.PREFIX
    } else {
        position
    }

    companion object {
        fun parse(raw: String): SymbolPosition = when (raw.lowercase()) {
            "prefix" -> SymbolPosition.PREFIX
            "suffix" -> SymbolPosition.SUFFIX
            "none" -> SymbolPosition.NONE
            else -> SymbolPosition.AUTO
        }
    }
}

val LocalMoneyFormat = staticCompositionLocalOf { MoneyFormat() }

/** Convención de fecha (orden + nombres de mes) de la moneda base actual. Ver [CURRENCY_LOCALE_INFO]. */
val LocalDateLocale = staticCompositionLocalOf { DEFAULT_LOCALE_INFO }
