package com.finanzen.ui.format

import kotlinx.datetime.LocalDate

private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val MESES_LARGOS = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

fun mesCorto(monthNumber: Int): String = MESES_CORTOS[monthNumber - 1]

fun mesLargo(monthNumber: Int): String = MESES_LARGOS[monthNumber - 1]

/** "Julio 2026" — para cabeceras de mes/selector. */
fun formatMesAnio(date: LocalDate): String = "${mesLargo(date.monthNumber)} ${date.year}"

/** "1 jul" — cabecera de día sin año (acordeón de Movimientos). */
fun formatDiaMes(epochDay: Long): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    return "${d.dayOfMonth} ${mesCorto(d.monthNumber)}"
}

/** "1 jul 2026" — fecha completa compacta (formulario de movimiento). */
fun formatFechaCorta(epochDay: Long): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    return "${d.dayOfMonth} ${mesCorto(d.monthNumber)} ${d.year}"
}

/** "1 de julio 2026" — fecha completa larga (Calendario). */
fun formatFechaLarga(date: LocalDate): String = "${date.dayOfMonth} de ${mesLargo(date.monthNumber).lowercase()} ${date.year}"
