package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Subscription
import com.finanzen.domain.Money
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class SubscriptionsViewModel(
    private val subsRepo: SubscriptionRepository,
    private val accountRepo: AccountRepository,
    private val txRepo: TransactionRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    val subscriptions: StateFlow<List<Subscription>> =
        subsRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // ponytail: catch-up al abrir Suscripciones; subir a app-start si se requiere puntualidad estricta.
        postDueCharges()
    }

    /**
     * Crea la suscripción, registra el cobro de **hoy** como gasto en Movimientos y programa el siguiente.
     * [frequency] = "DAILY" (cada [interval] días) o "MONTHLY" (mismo día del mes).
     */
    fun addSubscription(name: String, amountMinor: Long, frequency: String, interval: Long, remindDaysBefore: Long) {
        val account = ensureAccount()
        val today = todayEpochDay()
        // Cobrar ahora: el gasto aparece de inmediato en Movimientos.
        txRepo.add(
            accountId = account.id,
            categoryId = null,
            amountMinor = amountMinor,
            currency = account.currency,
            epochDay = today,
            note = name,
            kind = "EXPENSE",
        )
        val nextCharge = nextChargeAfter(today, frequency, interval)
        val id = subsRepo.add(
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            categoryId = null,
            accountId = account.id,
            frequency = frequency,
            intervalCount = interval,
            nextChargeDateEpochDay = nextCharge,
            remindDaysBefore = remindDaysBefore,
        )
        scheduler.scheduleReminder(
            id = id,
            title = name,
            body = "Próximo cobro en $remindDaysBefore día(s)",
            atEpochDay = nextCharge - remindDaysBefore,
        )
    }

    /** Convierte "12.50" → 1250 (centavos). null si el texto no es numérico válido. */
    fun parseAmountToMinor(text: String, decimals: Int = 2): Long? = Money.parseToMinor(text, decimals)

    fun deleteSubscription(id: Long) {
        subsRepo.delete(id)
        scheduler.cancel(id)
    }

    /** Registra como gasto cada cobro vencido y avanza la fecha hasta dejarla en el futuro. Idempotente. */
    private fun postDueCharges() {
        val today = todayEpochDay()
        subsRepo.activeNow().forEach { s ->
            var next = s.nextChargeDate
            while (next <= today) {
                txRepo.add(
                    accountId = s.accountId ?: ensureAccount().id,
                    categoryId = s.categoryId,
                    amountMinor = s.amountMinor,
                    currency = s.currency,
                    epochDay = next,
                    note = s.name,
                    kind = "EXPENSE",
                )
                next = nextChargeAfter(next, s.frequency, s.intervalCount)
            }
            if (next != s.nextChargeDate) {
                subsRepo.updateNextCharge(s.id, next)
                scheduler.scheduleReminder(
                    id = s.id,
                    title = s.name,
                    body = "Próximo cobro en ${s.remindDaysBefore} día(s)",
                    atEpochDay = next - s.remindDaysBefore,
                )
            }
        }
    }

    private fun todayEpochDay(): Long = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()

    private fun ensureAccount() = accountRepo.all().firstOrNull()
        ?: accountRepo.add("Efectivo", "CASH", "USD").let { id ->
            accountRepo.all().first { it.id == id }
        }

    companion object {
        /**
         * Próximo cobro **estrictamente posterior** a [fromEpochDay].
         * - DAILY: suma [interval] días (cada N días).
         * - MONTHLY: [interval] es el día del mes (1–31); devuelve su próxima ocurrencia, recortada en
         *   meses cortos (día 31 → 28/29 feb).
         */
        fun nextChargeAfter(fromEpochDay: Long, frequency: String, interval: Long): Long {
            val from = LocalDate.fromEpochDays(fromEpochDay.toInt())
            if (frequency != "MONTHLY") {
                return from.plus(DatePeriod(days = interval.coerceAtLeast(1).toInt())).toEpochDays().toLong()
            }
            val day = interval.coerceIn(1, 31).toInt()
            var cand = dayInMonth(from.year, from.monthNumber, day)
            if (cand <= from) {
                val nm = LocalDate(from.year, from.monthNumber, 1).plus(DatePeriod(months = 1))
                cand = dayInMonth(nm.year, nm.monthNumber, day)
            }
            return cand.toEpochDays().toLong()
        }

        /** Día [day] del mes [m]/[y], recortado al último día si el mes es más corto. */
        private fun dayInMonth(y: Int, m: Int, day: Int): LocalDate {
            val first = LocalDate(y, m, 1)
            val daysInMonth = first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays()
            return LocalDate(y, m, minOf(day, daysInMonth))
        }
    }
}
