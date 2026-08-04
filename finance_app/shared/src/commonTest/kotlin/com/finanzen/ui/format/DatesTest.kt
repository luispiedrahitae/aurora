package com.finanzen.ui.format

import com.finanzen.data.CurrencyLocaleInfo
import com.finanzen.domain.DateOrder
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DatesTest {
    private val shortMonths = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val longMonths = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    private fun locale(order: DateOrder) = CurrencyLocaleInfo(
        symbolPosition = com.finanzen.domain.SymbolPosition.PREFIX,
        dateOrder = order,
        shortMonths = shortMonths,
        longMonths = longMonths,
    )

    @Test
    fun formatMesAnioCapitalizaElMesLargo() {
        assertEquals("Julio 2026", formatMesAnio(LocalDate(2026, 7, 1), locale(DateOrder.DMY)))
    }

    @Test
    fun formatDiaMesOrdenDMY() {
        // mesCorto()/mesLargo() ignoran locale.shortMonths/longMonths a propósito (ver comentario
        // ponytail en Dates.kt: la UI es 100% español, solo el orden día/mes/año varía por moneda)
        // y usan siempre MESES_CORTOS, que lleva punto ("jul.").
        val epochDay = LocalDate(2026, 7, 1).toEpochDays().toLong()
        assertEquals("1 jul.", formatDiaMes(epochDay, locale(DateOrder.DMY)))
    }

    @Test
    fun formatDiaMesOrdenMDY() {
        val epochDay = LocalDate(2026, 7, 1).toEpochDays().toLong()
        assertEquals("jul. 1", formatDiaMes(epochDay, locale(DateOrder.MDY)))
    }

    @Test
    fun formatFechaCortaOrdenDMY() {
        val epochDay = LocalDate(2026, 7, 1).toEpochDays().toLong()
        assertEquals("1 jul. 2026", formatFechaCorta(epochDay, locale(DateOrder.DMY)))
    }

    @Test
    fun formatFechaCortaOrdenMDY() {
        val epochDay = LocalDate(2026, 7, 1).toEpochDays().toLong()
        assertEquals("jul. 1, 2026", formatFechaCorta(epochDay, locale(DateOrder.MDY)))
    }

    @Test
    fun formatFechaCortaOrdenYMD() {
        val epochDay = LocalDate(2026, 7, 1).toEpochDays().toLong()
        assertEquals("2026 jul. 1", formatFechaCorta(epochDay, locale(DateOrder.YMD)))
    }

    @Test
    fun formatFechaLargaUsaMesesLargos() {
        assertEquals("1 julio 2026", formatFechaLarga(LocalDate(2026, 7, 1), locale(DateOrder.DMY)))
    }

    @Test
    fun sinLocaleExplicitoUsaElDefaultDeUsd() {
        // Sin pasar `locale`, el parámetro por defecto es DEFAULT_LOCALE_INFO -- pero mesLargo()
        // ignora shortMonths/longMonths a propósito (ver Dates.kt), así que el mes sigue en español
        // igual que con cualquier otro locale; DEFAULT_LOCALE_INFO solo afecta symbolPosition/dateOrder.
        assertEquals("Julio 2026", formatMesAnio(LocalDate(2026, 7, 1)))
    }

    @Test
    fun diaSemanaMapeaCorrectamenteElIndice() {
        val epochDay = LocalDate(2026, 7, 1).toEpochDays().toLong() // 2026-07-01 es miércoles
        assertEquals("miércoles", diaSemana(epochDay))
    }

    @Test
    fun monthPeriodCombinaAnioYMes() {
        assertEquals(202607L, monthPeriod(LocalDate(2026, 7, 5)))
    }

    @Test
    fun periodOfEpochDayCoincideConMonthPeriod() {
        val epochDay = LocalDate(2026, 1, 15).toEpochDays().toLong()
        assertEquals(202601L, periodOfEpochDay(epochDay))
    }
}
