package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Subscription
import com.finanzen.domain.RecurrenceSchedule
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SubscriptionsViewModel(
    private val subsRepo: SubscriptionRepository,
    private val accountRepo: AccountRepository,
    private val txRepo: TransactionRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    val subscriptions: StateFlow<List<Subscription>> =
        subsRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val baseCurrency: String get() = settingsRepo.baseCurrency()

    init {
        // ponytail: catch-up al abrir Suscripciones; subir a app-start si se requiere puntualidad estricta.
        postDueCharges()
    }

    /**
     * Crea la suscripción con [startEpochDay] como primer cobro programado (puede ser hoy, pasado o
     * futuro) y una cuenta elegida por el usuario ([accountId], `null` cae a [ensureAccount]). El
     * cobro inicial se resuelve reutilizando [postDueChargesFor] en vez de duplicar el "cobrar ahora":
     * si [startEpochDay] es hoy o pasado, cobra de inmediato (y pone al día atrasos si aplica); si es
     * futuro, no cobra nada todavía. [frequency] = "DAILY" (cada [interval] días) o "MONTHLY" (mismo
     * día del mes).
     */
    fun addSubscription(name: String, amountMinor: Long, frequency: String, interval: Long, accountId: Long?, startEpochDay: Long) {
        val account = accountId?.let { id -> accountRepo.all().firstOrNull { it.id == id } } ?: ensureAccount()
        val remindDaysBefore = settingsRepo.reminderDaysBefore()
        val id = subsRepo.add(
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            categoryId = null,
            accountId = account.id,
            frequency = frequency,
            intervalCount = interval,
            nextChargeDateEpochDay = startEpochDay,
            remindDaysBefore = remindDaysBefore,
        )
        subsRepo.byId(id)?.let { postDueChargesFor(it) }
    }

    fun deleteSubscription(id: Long) {
        subsRepo.delete(id)
        scheduler.cancel(id)
    }

    /** Registra como gasto cada cobro vencido y avanza la fecha hasta dejarla en el futuro. Idempotente. */
    private fun postDueCharges() {
        subsRepo.activeNow().forEach { postDueChargesFor(it) }
    }

    private fun postDueChargesFor(s: Subscription) {
        val today = todayEpochDay()
        val due = RecurrenceSchedule.occurrencesDueUpTo(s.nextChargeDate, today, s.frequency, s.intervalCount)
        due.dates.forEach { chargeDay ->
            txRepo.add(
                accountId = s.accountId ?: ensureAccount().id,
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
            scheduler.scheduleReminder(
                id = s.id,
                title = s.name,
                body = "Próximo cobro en ${s.remindDaysBefore} día(s)",
                atEpochDay = due.next - s.remindDaysBefore,
            )
        }
    }

    private fun todayEpochDay(): Long = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()

    private fun ensureAccount() = accountRepo.all().firstOrNull()
        ?: accountRepo.add("Efectivo", "CASH", "USD").let { id ->
            accountRepo.all().first { it.id == id }
        }
}
