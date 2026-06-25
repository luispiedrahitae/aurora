package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.db.Account
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AccountsViewModel(
    private val accountRepo: AccountRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** La app es de moneda única; las cuentas nuevas heredan la moneda base (ver Ajustes). */
    val baseCurrency: String get() = settingsRepo.baseCurrency()

    val accountTypes: List<String> = listOf("CASH", "DEBIT", "SAVINGS", "CREDIT")

    fun add(name: String, type: String) {
        if (name.isBlank()) return
        accountRepo.add(name = name.trim(), type = type, currency = settingsRepo.baseCurrency())
    }

    fun delete(id: Long) = accountRepo.delete(id)
}
