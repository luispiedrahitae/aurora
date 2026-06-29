package com.finanzen.viewmodel

import com.finanzen.viewmodel.SubscriptionsViewModel.Companion.chargesDueUpTo
import com.finanzen.viewmodel.SubscriptionsViewModel.Companion.nextChargeAfter
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionScheduleTest {
    private fun epoch(y: Int, m: Int, d: Int) = LocalDate(y, m, d).toEpochDays().toLong()
    private fun dateOf(epochDay: Long) = LocalDate.fromEpochDays(epochDay.toInt())
    private fun fmt(epochDays: List<Long>) = epochDays.map { dateOf(it).toString() }

    @Test
    fun dailySumaLosDias() {
        val from = epoch(2026, 6, 1)
        assertEquals(epoch(2026, 6, 8), nextChargeAfter(from, "DAILY", 7))
        assertEquals(epoch(2026, 7, 1), nextChargeAfter(from, "DAILY", 30))
    }

    @Test
    fun monthlyDaLaProximaOcurrenciaDelDia() {
        // interval = día del mes. Si el día aún no pasó este mes, es este mes; si ya pasó, el siguiente.
        assertEquals(epoch(2026, 6, 15), nextChargeAfter(epoch(2026, 6, 5), "MONTHLY", 15))
        assertEquals(epoch(2026, 7, 15), nextChargeAfter(epoch(2026, 6, 20), "MONTHLY", 15))
    }

    @Test
    fun monthlyRecortaEnMesesCortos() {
        // Día 31 desde 31 ene (mismo día) -> avanza a 28 feb (2026 no bisiesto). ponytail: se recorta.
        assertEquals(epoch(2026, 2, 28), nextChargeAfter(epoch(2026, 1, 31), "MONTHLY", 31))
    }

    @Test
    fun intervaloCeroNoSeQuedaEnElMismoDia() {
        // Defensivo: interval<1 se eleva a 1, nunca devuelve el mismo día (evitaría bucle infinito en catch-up).
        val from = epoch(2026, 6, 5)
        assertEquals(epoch(2026, 6, 6), nextChargeAfter(from, "DAILY", 0))
        assertEquals(true, dateOf(nextChargeAfter(from, "MONTHLY", 0)) > dateOf(from)) // día 1 -> próximo mes
        assertEquals(true, dateOf(nextChargeAfter(from, "DAILY", 0)) > dateOf(from))
    }

    // ---- Catch-up: "transcurren X días / llega el día del mes" → cobros generados en Movimientos ----

    @Test
    fun catchUpCadaNDiasGeneraTodosLosCobrosTranscurridos() {
        // Cada 7 días desde el 1 jun; hoy 30 jun → cobros 1,8,15,22,29 jun y el próximo queda el 6 jul.
        val due = chargesDueUpTo(epoch(2026, 6, 1), epoch(2026, 6, 30), "DAILY", 7)
        assertEquals(
            listOf("2026-06-01", "2026-06-08", "2026-06-15", "2026-06-22", "2026-06-29"),
            fmt(due.charges),
        )
        assertEquals(epoch(2026, 7, 6), due.next)
    }

    @Test
    fun catchUpDiaDelMesGeneraUnCobroPorMes() {
        // Día 15, primer cobro 15 may; hoy 20 jul → 15 may, 15 jun, 15 jul; próximo 15 ago.
        val due = chargesDueUpTo(epoch(2026, 5, 15), epoch(2026, 7, 20), "MONTHLY", 15)
        assertEquals(listOf("2026-05-15", "2026-06-15", "2026-07-15"), fmt(due.charges))
        assertEquals(epoch(2026, 8, 15), due.next)
    }

    @Test
    fun mesDe28DiasNoBisiestoRecortaYLuegoRecupera() {
        // Día 31 desde 31 ene 2026 (feb tiene 28); hoy 5 mar → 31 ene, 28 feb (recortado); próximo 31 mar.
        val due = chargesDueUpTo(epoch(2026, 1, 31), epoch(2026, 3, 5), "MONTHLY", 31)
        assertEquals(listOf("2026-01-31", "2026-02-28"), fmt(due.charges))
        assertEquals(epoch(2026, 3, 31), due.next) // recupera el día 31 tras el mes corto
    }

    @Test
    fun mesDe29DiasBisiestoUsaEl29DeFebrero() {
        // Día 31 desde 31 ene 2024 (bisiesto, feb tiene 29); hoy 5 mar → 31 ene, 29 feb; próximo 31 mar.
        val due = chargesDueUpTo(epoch(2024, 1, 31), epoch(2024, 3, 5), "MONTHLY", 31)
        assertEquals(listOf("2024-01-31", "2024-02-29"), fmt(due.charges))
        assertEquals(epoch(2024, 3, 31), due.next)
    }

    @Test
    fun mesDe30DiasRecortaElDia31() {
        // Día 31 desde 31 mar (abr tiene 30); hoy 1 may → 31 mar, 30 abr (recortado); próximo 31 may.
        val due = chargesDueUpTo(epoch(2026, 3, 31), epoch(2026, 5, 1), "MONTHLY", 31)
        assertEquals(listOf("2026-03-31", "2026-04-30"), fmt(due.charges))
        assertEquals(epoch(2026, 5, 31), due.next)
    }

    @Test
    fun sinCobrosVencidosNoGeneraNadaYConservaLaFecha() {
        // nextCharge en el futuro respecto a hoy → cero cobros, fecha intacta (idempotencia al reabrir).
        val due = chargesDueUpTo(epoch(2026, 7, 5), epoch(2026, 6, 29), "MONTHLY", 5)
        assertEquals(emptyList<Long>(), due.charges)
        assertEquals(epoch(2026, 7, 5), due.next)
    }
}
