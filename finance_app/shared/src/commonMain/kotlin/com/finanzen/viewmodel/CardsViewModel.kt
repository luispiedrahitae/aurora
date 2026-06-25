package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import com.finanzen.domain.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CardsViewModel(
    private val cardRepo: CardRepository,
    private val planRepo: InstallmentPlanRepository,
    private val accountRepo: AccountRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val cards: StateFlow<List<Card>> =
        cardRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val plans: StateFlow<List<InstallmentPlan>> =
        planRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Moneda única de la app; las tarjetas se respaldan con una cuenta en esta moneda. */
    val baseCurrency: String get() = settingsRepo.baseCurrency()

    val networks: List<String> = listOf("VISA", "MASTERCARD", "AMEX", "OTRA")

    /** Crea una tarjeta y su cuenta de respaldo (CREDIT o DEBIT) en la moneda base. */
    fun addCard(
        network: String,
        last4: String,
        isCredit: Boolean,
        creditLimitMinor: Long?,
        cutoffDay: Long?,
        dueDay: Long?,
    ) {
        if (last4.isBlank()) return
        val accountId = accountRepo.add(
            name = "$network ••••$last4",
            type = if (isCredit) "CREDIT" else "DEBIT",
            currency = settingsRepo.baseCurrency(),
            openingBalanceMinor = 0,
        )
        cardRepo.add(
            accountId = accountId,
            last4 = last4,
            network = network,
            creditLimitMinor = if (isCredit) creditLimitMinor else null,
            cutoffDay = cutoffDay,
            dueDay = dueDay,
        )
    }

    /** Añade un plan de cuotas a una tarjeta existente. */
    fun addPlan(
        cardId: Long,
        description: String,
        totalAmountMinor: Long,
        installments: Long,
        interestRate: Double,
        startEpochDay: Long,
    ) {
        if (installments <= 0) return
        planRepo.add(
            cardId = cardId,
            totalAmountMinor = totalAmountMinor,
            installments = installments,
            interestRate = interestRate,
            startDateEpochDay = startEpochDay,
            description = description,
        )
    }

    /** Convierte "12.50" → 1250 (centavos). null si el texto no es numérico válido. */
    fun parseAmountToMinor(text: String): Long? = Money.parseToMinor(text, 2)

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
