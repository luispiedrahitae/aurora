package com.finanzen.viewmodel

import com.finanzen.viewmodel.SubscriptionsViewModel.Companion.nextChargeAfter
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionScheduleTest {
    private fun epoch(y: Int, m: Int, d: Int) = LocalDate(y, m, d).toEpochDays().toLong()
    private fun dateOf(epochDay: Long) = LocalDate.fromEpochDays(epochDay.toInt())

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
}
