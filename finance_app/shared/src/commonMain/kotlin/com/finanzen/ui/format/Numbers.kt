package com.finanzen.ui.format

import kotlin.math.abs
import kotlin.math.roundToLong

/** Porcentaje en estilo es-CO: "9 %", "1,5 %", "−2,3 %". Hasta [maxDecimals] decimales, sin ceros sobrantes. */
fun formatPercent(fraction: Double, maxDecimals: Int = 1): String {
    val factor = TEN_POW[maxDecimals.coerceIn(0, TEN_POW.lastIndex)]
    val scaled = (abs(fraction) * PERCENT * factor).roundToLong()
    val whole = scaled / factor
    val decimals = (scaled % factor).toString().padStart(maxDecimals, '0').trimEnd('0')
    val sign = if (fraction < 0 && scaled != 0L) "−" else ""
    return if (decimals.isEmpty()) "$sign$whole %" else "$sign$whole,$decimals %"
}

/** Número con coma decimal y hasta [decimals] decimales fijos, sin separador de miles: 32.84 → "32,8". */
fun formatDecimal(value: Double, decimals: Int = 1): String {
    val factor = TEN_POW[decimals.coerceIn(0, TEN_POW.lastIndex)]
    val scaled = (abs(value) * factor).roundToLong()
    val sign = if (value < 0 && scaled != 0L) "−" else ""
    val whole = scaled / factor
    return if (decimals <= 0) "$sign$whole" else "$sign$whole,${(scaled % factor).toString().padStart(decimals, '0')}"
}

/** Texto de un campo de porcentaje ("1,5", "9", "0.75") a fracción (0.015). Nulo si no es un número. */
fun parsePercent(text: String): Double? = text.trim().replace(',', '.').replace("%", "").trim().toDoubleOrNull()?.div(PERCENT)

/** Fracción a texto editable de campo de porcentaje: 0.015 → "1,5", 0.09 → "9". */
fun percentFieldText(fraction: Double): String = formatPercent(fraction, maxDecimals = 2).removeSuffix(" %").replace("−", "-")

private const val PERCENT = 100.0
private val TEN_POW = longArrayOf(1, 10, 100, 1_000)
