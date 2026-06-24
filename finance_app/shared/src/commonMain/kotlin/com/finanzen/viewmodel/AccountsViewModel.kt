package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CurrencyRepository
import com.finanzen.db.Account
import com.finanzen.db.Currency
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AccountsViewModel(
    private val accountRepo: AccountRepository,
    currencyRepo: CurrencyRepository,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Monedas sembradas (USD/EUR/COP/MXN). Estáticas, se leen una vez. */
    val currencies: List<Currency> = currencyRepo.all()

    val accountTypes: List<String> = listOf("CASH", "DEBIT", "SAVINGS", "CREDIT")

    fun add(name: String, type: String, currency: String) {
        if (name.isBlank()) return
        accountRepo.add(name = name.trim(), type = type, currency = currency)
    }

    fun delete(id: Long) = accountRepo.delete(id)
}
