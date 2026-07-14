package com.finanzen.domain

import com.finanzen.domain.RecurrenceSchedule.monthlyEquivalent
import com.finanzen.domain.RecurrenceSchedule.nextOccurrenceAfter
import com.finanzen.domain.RecurrenceSchedule.occurrencesDueUpTo
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecurrenceScheduleTest {
    private fun epoch(y: Int, m: Int, d: Int) = LocalDate(y, m, d).toEpochDays().toLong()
    private fun dateOf(epochDay: Long) = LocalDate.fromEpochDays(epochDay.toInt())
    private fun fmt(epochDays: List<Long>) = epochDays.map { dateOf(it).toString() }

    @Test
    fun dailySumaLosDias() {
        val from = epoch(2026, 6, 1)
        assertEquals(epoch(2026, 6, 8), nextOccurrenceAfter(from, "DAILY", 7))
        assertEquals(epoch(2026, 7, 1), nextOccurrenceAfter(from, "DAILY", 30))
    }

    @Test
    fun monthlyDaLaProximaOcurrenciaDelDia() {
        // interval = día del mes. Si el día aún no pasó este mes, es este mes; si ya pasó, el siguiente.
        assertEquals(epoch(2026, 6, 15), nextOccurrenceAfter(epoch(2026, 6, 5), "MONTHLY", 15))
        assertEquals(epoch(2026, 7, 15), nextOccurrenceAfter(epoch(2026, 6, 20), "MONTHLY", 15))
    }

    @Test
    fun monthlyRecortaEnMesesCortos() {
        // Día 31 desde 31 ene (mismo día) -> avanza a 28 feb (2026 no bisiesto). ponytail: se recorta.
        assertEquals(epoch(2026, 2, 28), nextOccurrenceAfter(epoch(2026, 1, 31), "MONTHLY", 31))
    }

    @Test
    fun intervaloCeroNoSeQuedaEnElMismoDia() {
        // Defensivo: interval<1 se eleva a 1, nunca devuelve el mismo día (evitaría bucle infinito en catch-up).
        val from = epoch(2026, 6, 5)
        assertEquals(epoch(2026, 6, 6), nextOccurrenceAfter(from, "DAILY", 0))
        assertEquals(true, dateOf(nextOccurrenceAfter(from, "MONTHLY", 0)) > dateOf(from)) // día 1 -> próximo mes
        assertEquals(true, dateOf(nextOccurrenceAfter(from, "DAILY", 0)) > dateOf(from))
    }

    // ---- Catch-up: "transcurren X días / llega el día del mes" → ocurrencias generadas en Movimientos ----

    @Test
    fun catchUpCadaNDiasGeneraTodasLasOcurrenciasTranscurridas() {
        // Cada 7 días desde el 1 jun; hoy 30 jun → ocurrencias 1,8,15,22,29 jun y la próxima queda el 6 jul.
        val due = occurrencesDueUpTo(epoch(2026, 6, 1), epoch(2026, 6, 30), "DAILY", 7)
        assertEquals(
            listOf("2026-06-01", "2026-06-08", "2026-06-15", "2026-06-22", "2026-06-29"),
            fmt(due.dates),
        )
        assertEquals(epoch(2026, 7, 6), due.next)
    }

    @Test
    fun catchUpDiaDelMesGeneraUnaOcurrenciaPorMes() {
        // Día 15, primera ocurrencia 15 may; hoy 20 jul → 15 may, 15 jun, 15 jul; próxima 15 ago.
        val due = occurrencesDueUpTo(epoch(2026, 5, 15), epoch(2026, 7, 20), "MONTHLY", 15)
        assertEquals(listOf("2026-05-15", "2026-06-15", "2026-07-15"), fmt(due.dates))
        assertEquals(epoch(2026, 8, 15), due.next)
    }

    @Test
    fun mesDe28DiasNoBisiestoRecortaYLuegoRecupera() {
        // Día 31 desde 31 ene 2026 (feb tiene 28); hoy 5 mar → 31 ene, 28 feb (recortado); próxima 31 mar.
        val due = occurrencesDueUpTo(epoch(2026, 1, 31), epoch(2026, 3, 5), "MONTHLY", 31)
        assertEquals(listOf("2026-01-31", "2026-02-28"), fmt(due.dates))
        assertEquals(epoch(2026, 3, 31), due.next) // recupera el día 31 tras el mes corto
    }

    @Test
    fun mesDe29DiasBisiestoUsaEl29DeFebrero() {
        // Día 31 desde 31 ene 2024 (bisiesto, feb tiene 29); hoy 5 mar → 31 ene, 29 feb; próxima 31 mar.
        val due = occurrencesDueUpTo(epoch(2024, 1, 31), epoch(2024, 3, 5), "MONTHLY", 31)
        assertEquals(listOf("2024-01-31", "2024-02-29"), fmt(due.dates))
        assertEquals(epoch(2024, 3, 31), due.next)
    }

    @Test
    fun mesDe30DiasRecortaElDia31() {
        // Día 31 desde 31 mar (abr tiene 30); hoy 1 may → 31 mar, 30 abr (recortado); próxima 31 may.
        val due = occurrencesDueUpTo(epoch(2026, 3, 31), epoch(2026, 5, 1), "MONTHLY", 31)
        assertEquals(listOf("2026-03-31", "2026-04-30"), fmt(due.dates))
        assertEquals(epoch(2026, 5, 31), due.next)
    }

    @Test
    fun sinOcurrenciasVencidasNoGeneraNadaYConservaLaFecha() {
        // next en el futuro respecto a hoy → cero ocurrencias, fecha intacta (idempotencia al reabrir).
        val due = occurrencesDueUpTo(epoch(2026, 7, 5), epoch(2026, 6, 29), "MONTHLY", 5)
        assertEquals(emptyList<Long>(), due.dates)
        assertEquals(epoch(2026, 7, 5), due.next)
    }

    // ---- Costo mensual equivalente ----

    @Test
    fun monthlyEquivalentSuscripcionMonthlyEsElMontoTalCual() {
        // interval en MONTHLY es el día del mes, no un multiplicador: no divide el monto.
        assertEquals(5_000, monthlyEquivalent(5_000, "MONTHLY", 15))
    }

    @Test
    fun monthlyEquivalentSuscripcionDailyPromediaPorIntervalo() {
        // 1000 x 30.437 / 7 = 4348.14... -> 4348.
        assertEquals(4_348, monthlyEquivalent(1_000, "DAILY", 7))
    }

    // ---- Edge cases QA (ver reporte de hallazgos) ----

    @Test
    fun nextOccurrenceAfterConIntervaloNegativoParaMonthlySeElevaAUno() {
        // interval.coerceIn(1,31): un intervalo negativo (nunca debería llegar así, pero defensivo)
        // se eleva a 1, igual que interval=0. Día 1 de junio 2026 ya pasó (from=15) -> próximo: julio 1.
        assertEquals(epoch(2026, 7, 1), nextOccurrenceAfter(epoch(2026, 6, 15), "MONTHLY", -5))
    }

    @Test
    fun nextOccurrenceAfterConIntervaloMayorA31SeAcotaA31() {
        // interval.coerceIn(1,31): 45 se acota a 31, mismo resultado que monthlyRecortaEnMesesCortos.
        assertEquals(epoch(2026, 2, 28), nextOccurrenceAfter(epoch(2026, 1, 31), "MONTHLY", 45))
    }

    @Test
    fun monthlyEquivalentConMontoCeroEsCero() {
        assertEquals(0, monthlyEquivalent(0, "DAILY", 7))
        assertEquals(0, monthlyEquivalent(0, "MONTHLY", 15))
    }

    @Test
    fun monthlyEquivalentConMontoNegativoEsSimetrico() {
        // Un monto negativo (p.ej. un reembolso recurrente) escala igual que el positivo, en espejo.
        assertEquals(-4_348, monthlyEquivalent(-1_000, "DAILY", 7))
    }

    @Test
    fun occurrencesDueUpToConFirstEpochDayIgualATodayEsInclusivo() {
        // Límite inclusivo: si la primera ocurrencia cae justo hoy, cuenta como vencida.
        val due = occurrencesDueUpTo(epoch(2026, 6, 1), epoch(2026, 6, 1), "MONTHLY", 1)
        assertEquals(listOf("2026-06-01"), fmt(due.dates))
        assertEquals(epoch(2026, 7, 1), due.next)
    }
}
