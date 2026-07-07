package com.finanzen.ui.format

import com.finanzen.data.CurrencyLocaleInfo
import com.finanzen.data.DEFAULT_LOCALE_INFO
import com.finanzen.domain.DateOrder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

fun mesCorto(monthNumber: Int, locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO): String = locale.shortMonths[monthNumber - 1]

fun mesLargo(monthNumber: Int, locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO): String = locale.longMonths[monthNumber - 1]

/** "Julio 2026" / "July 2026" — para cabeceras de mes/selector. */
fun formatMesAnio(date: LocalDate, locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO): String = "${mesLargo(date.monthNumber, locale).replaceFirstChar { it.uppercase() }} ${date.year}"

/** Clave "AAAAMM" para agrupar/filtrar por mes (Movimientos, Análisis, Cuentas). */
fun monthPeriod(date: LocalDate): Long = (date.year * 100 + date.monthNumber).toLong()

fun periodOfEpochDay(epochDay: Long): Long = monthPeriod(LocalDate.fromEpochDays(epochDay.toInt()))

/** "1 jul" / "Jul 1" — cabecera de día sin año (acordeón de Movimientos), orden según [locale]. */
fun formatDiaMes(epochDay: Long, locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    val mes = mesCorto(d.monthNumber, locale)
    return if (locale.dateOrder == DateOrder.MDY) "$mes ${d.dayOfMonth}" else "${d.dayOfMonth} $mes"
}

private val DIAS_SEMANA = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

// ponytail: nombres de día fijos en español (no CLDR por moneda como shortMonths/longMonths).
// Si hace falta el mismo nivel de detalle que los meses, habría que añadir shortWeekdays/
// longWeekdays a CurrencyLocaleInfo y regenerar las 149 entradas de WorldLocales.kt.
fun diaSemana(epochDay: Long): String = DIAS_SEMANA[LocalDate.fromEpochDays(epochDay.toInt()).dayOfWeek.isoDayNumber - 1]

/** "1 jul 2026" / "Jul 1, 2026" / "2026 jul 1" — fecha completa compacta (formulario de movimiento). */
fun formatFechaCorta(epochDay: Long, locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    return formatDiaMesAnio(d.dayOfMonth, mesCorto(d.monthNumber, locale), d.year, locale.dateOrder)
}

/** "1 julio 2026" / "July 1, 2026" / "2026 julio 1" — fecha completa larga (Calendario). */
fun formatFechaLarga(date: LocalDate, locale: CurrencyLocaleInfo = DEFAULT_LOCALE_INFO): String = formatDiaMesAnio(date.dayOfMonth, mesLargo(date.monthNumber, locale), date.year, locale.dateOrder)

// ponytail: orden genérico día/mes/año sin conectores gramaticales (ej. el "de" del español,
// "d. MMMM" del alemán): DateOrder solo captura el orden, no el patrón completo de cada idioma.
// Si hace falta el patrón exacto por idioma, habría que guardar el patrón CLDR completo en vez
// del enum de 3 valores — fuera de alcance de este cambio.
private fun formatDiaMesAnio(day: Int, mes: String, year: Int, order: DateOrder): String = when (order) {
    DateOrder.MDY -> "$mes $day, $year"
    DateOrder.YMD -> "$year $mes $day"
    DateOrder.DMY -> "$day $mes $year"
}
