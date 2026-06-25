package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.RecurringExpenseRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.db.RecurringExpense
import com.finanzen.db.Subscription
import com.finanzen.domain.Money
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SubscriptionsViewModel(
    private val subsRepo: SubscriptionRepository,
    private val recurRepo: RecurringExpenseRepository,
    private val accountRepo: AccountRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    val subscriptions: StateFlow<List<Subscription>> =
        subsRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recurring: StateFlow<List<RecurringExpense>> =
        recurRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSubscription(name: String, amountMinor: Long, nextChargeEpochDay: Long, remindDaysBefore: Long) {
        val account = ensureAccount()
        val id = subsRepo.add(
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            categoryId = null,
            accountId = account.id,
            frequency = "MONTHLY",
            intervalCount = 1L,
            nextChargeDateEpochDay = nextChargeEpochDay,
            remindDaysBefore = remindDaysBefore,
        )
        scheduler.scheduleReminder(
            id = id,
            title = name,
            body = "Próximo cobro en $remindDaysBefore día(s)",
            atEpochDay = nextChargeEpochDay - remindDaysBefore,
        )
    }

    fun addRecurring(name: String, amountMinor: Long, nextChargeEpochDay: Long, remindDaysBefore: Long) {
        val account = ensureAccount()
        val id = recurRepo.add(
            name = name,
            amountMinor = amountMinor,
            currency = account.currency,
            categoryId = null,
            accountId = account.id,
            frequency = "MONTHLY",
            intervalCount = 1L,
            nextChargeDateEpochDay = nextChargeEpochDay,
            remindDaysBefore = remindDaysBefore,
        )
        scheduler.scheduleReminder(
            id = id,
            title = name,
            body = "Próximo cobro en $remindDaysBefore día(s)",
            atEpochDay = nextChargeEpochDay - remindDaysBefore,
        )
    }

    /** Convierte "12.50" → 1250 (centavos). null si el texto no es numérico válido. */
    fun parseAmountToMinor(text: String, decimals: Int = 2): Long? = Money.parseToMinor(text, decimals)

    fun deleteSubscription(id: Long) {
        subsRepo.delete(id)
        scheduler.cancel(id)
    }

    fun deleteRecurring(id: Long) {
        recurRepo.delete(id)
        scheduler.cancel(id)
    }

    private fun ensureAccount() = accountRepo.all().firstOrNull()
        ?: accountRepo.add("Efectivo", "CASH", "USD").let { id ->
            accountRepo.all().first { it.id == id }
        }
}
