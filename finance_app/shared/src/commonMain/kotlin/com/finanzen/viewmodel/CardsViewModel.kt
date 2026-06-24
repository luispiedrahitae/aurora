package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

class CardsViewModel(
    private val cardRepo: CardRepository,
    private val planRepo: InstallmentPlanRepository,
    private val accountRepo: AccountRepository,
) : ViewModel() {

    val cards: StateFlow<List<Card>> =
        cardRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val plans: StateFlow<List<InstallmentPlan>> =
        planRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lazy: añade una tarjeta de muestra si no hay; si ya hay tarjetas, añade un plan de cuotas a la primera. */
    fun addSample() {
        val existing = cardRepo.all()
        if (existing.isEmpty()) {
            val accountId = accountRepo.add(
                name = "Tarjeta Crédito",
                type = "CREDIT",
                currency = "USD",
                openingBalanceMinor = 0,
            )
            cardRepo.add(
                accountId = accountId,
                last4 = (1000..9999).random().toString(),
                network = listOf("VISA", "MASTERCARD", "AMEX").random(),
                creditLimitMinor = 500_000L,
                cutoffDay = 15L,
                dueDay = 5L,
            )
        } else {
            val firstCard = existing.first()
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()
            val descriptions = listOf("iPhone", "Refrigerador", "Vuelo Bogotá-Madrid", "Bicicleta", "Curso online")
            planRepo.add(
                cardId = firstCard.id,
                totalAmountMinor = Random.nextLong(50_000, 300_000),
                installments = listOf(3L, 6L, 12L, 18L, 24L).random(),
                interestRate = listOf(0.0, 1.2, 2.5, 3.8).random(),
                startDateEpochDay = today,
                description = descriptions.random(),
            )
        }
    }

    /**
     * Borra la tarjeta y su cuenta de respaldo (CREDIT), que se creó junto con ella. Sin esto, la
     * cuenta queda huérfana y sigue apareciendo en el selector de cuentas de las transacciones.
     */
    fun deleteCard(id: Long) {
        val accountId = (cards.value.firstOrNull { it.id == id } ?: cardRepo.all().firstOrNull { it.id == id })?.accountId
        cardRepo.delete(id)
        accountId?.let { accountRepo.delete(it) }
    }

    fun deletePlan(id: Long) = planRepo.delete(id)
}
