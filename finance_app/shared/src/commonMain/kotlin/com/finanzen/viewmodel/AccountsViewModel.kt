package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.TransactionRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AccountsViewModel(
    private val accountRepo: AccountRepository,
    txRepo: TransactionRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saldo actual por cuenta (id → minor) = saldo inicial + ingresos − gastos ± transferencias. */
    val balances: StateFlow<Map<Long, Long>> =
        combine(accountRepo.observeAll(), txRepo.observeAll()) { accts, txs -> computeBalances(accts, txs) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** La app es de moneda única; las cuentas nuevas heredan la moneda base (ver Ajustes). */
    val baseCurrency: String get() = settingsRepo.baseCurrency()

    val accountTypes: List<String> = listOf("CASH", "DEBIT", "SAVINGS", "CREDIT")

    fun add(name: String, type: String) {
        if (name.isBlank()) return
        accountRepo.add(name = name.trim(), type = type, currency = settingsRepo.baseCurrency())
    }

    fun delete(id: Long) = accountRepo.delete(id)

    companion object {
        /** Agregación pura y testeable. Una TRANSFER resta del origen y suma al destino. */
        fun computeBalances(accounts: List<Account>, txs: List<TransactionRow>): Map<Long, Long> {
            val balance = accounts.associate { it.id to it.openingBalanceMinor }.toMutableMap()
            for (t in txs) {
                when (t.kind) {
                    "INCOME" -> balance[t.accountId] = (balance[t.accountId] ?: 0L) + t.amountMinor
                    "EXPENSE" -> balance[t.accountId] = (balance[t.accountId] ?: 0L) - t.amountMinor
                    "TRANSFER" -> {
                        balance[t.accountId] = (balance[t.accountId] ?: 0L) - t.amountMinor
                        t.transferAccountId?.let { to -> balance[to] = (balance[to] ?: 0L) + t.amountMinor }
                    }
                }
            }
            return balance
        }
    }
}
