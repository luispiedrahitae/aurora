package com.finanzen.viewmodel

import com.finanzen.data.AccountRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Subscription
import com.finanzen.domain.RecurrenceSchedule
import com.finanzen.platform.NotificationScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Cobra las suscripciones vencidas. Compartida entre el catch-up de arranque de la app (ver
 * `AppModule.kt`, `single(createdAtStart = true)`) y `SubscriptionsViewModel` (al abrir la
 * pantalla o al crear una suscripción nueva) para no duplicar la lógica.
 */
class SubscriptionCatchUp(
    private val subsRepo: SubscriptionRepository,
    private val accountRepo: AccountRepository,
    private val txRepo: TransactionRepository,
    private val scheduler: NotificationScheduler,
) {
    /** Registra como gasto cada cobro vencido y avanza la fecha hasta dejarla en el futuro. Idempotente. */
    fun run() {
        subsRepo.activeNow().forEach { runFor(it) }
    }

    fun runFor(s: Subscription) {
        val today = todayEpochDay()
        val due = RecurrenceSchedule.occurrencesDueUpTo(s.nextChargeDate, today, s.frequency, s.intervalCount)
        due.dates.forEach { chargeDay ->
            txRepo.add(
                accountId = s.accountId ?: accountRepo.ensureAny().id,
                categoryId = s.categoryId,
                amountMinor = s.amountMinor,
                currency = s.currency,
                epochDay = chargeDay,
                note = s.name,
                kind = "EXPENSE",
                subscriptionId = s.id,
            )
        }
        if (due.next != s.nextChargeDate) {
            subsRepo.updateNextCharge(s.id, due.next)
        }
        // Se reprograma siempre (no solo cuando avanza la fecha) para que una suscripción recién
        // creada con fecha futura también tenga su primer recordatorio agendado: scheduleReminder
        // es idempotente (WorkManager con ExistingWorkPolicy.REPLACE en Android), así que no hay
        // costo por repetirlo cuando due.next no cambió.
        scheduler.scheduleReminder(
            id = s.id,
            title = s.name,
            body = "Próximo cobro en ${s.remindDaysBefore} día(s)",
            atEpochDay = due.next - s.remindDaysBefore,
        )
    }

    private fun todayEpochDay(): Long = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()
}
