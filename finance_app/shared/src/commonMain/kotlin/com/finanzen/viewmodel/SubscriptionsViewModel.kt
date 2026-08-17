package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.db.Account
import com.finanzen.db.Subscription
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SubscriptionsViewModel(
    private val subsRepo: SubscriptionRepository,
    private val accountRepo: AccountRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: NotificationScheduler,
    private val catchUp: SubscriptionCatchUp,
    private val categoryRepo: CategoryRepository,
) : ViewModel() {

    val subscriptions: StateFlow<List<Subscription>> =
        subsRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val baseCurrency: String get() = settingsRepo.baseCurrency()

    init {
        // Idempotente: también corre en el catch-up de arranque (AppModule.kt), esto cubre un
        // proceso que lleva mucho tiempo vivo en background sin reiniciarse.
        catchUp.run()
    }

    /**
     * Crea la suscripción con [startEpochDay] como primer cobro programado (puede ser hoy, pasado o
     * futuro) y una cuenta elegida por el usuario ([accountId], `null` cae a [AccountRepository.ensureAny]).
     * El cobro inicial se resuelve reutilizando [SubscriptionCatchUp.runFor] en vez de duplicar el
     * "cobrar ahora": si [startEpochDay] es hoy o pasado, cobra de inmediato (y pone al día atrasos si
     * aplica); si es futuro, no cobra nada todavía. [frequency] = "DAILY" (cada [interval] días) o
     * "MONTHLY" (mismo día del mes).
     */
    fun addSubscription(name: String, amountMinor: Long, frequency: String, interval: Long, accountId: Long?, startEpochDay: Long) {
        val account = accountId?.let { id -> accountRepo.all().firstOrNull { it.id == id } } ?: accountRepo.ensureAny()
        val remindDaysBefore = settingsRepo.reminderDaysBefore()
        val id = subsRepo.add(
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            categoryId = categoryRepo.systemLeaf("EXPENSE", "Suscripciones", "Suscripción"),
            accountId = account.id,
            frequency = frequency,
            intervalCount = interval,
            nextChargeDateEpochDay = startEpochDay,
            remindDaysBefore = remindDaysBefore,
        )
        subsRepo.byId(id)?.let { catchUp.runFor(it) }
    }

    fun deleteSubscription(id: Long) {
        subsRepo.delete(id)
        scheduler.cancel(id)
    }
}
