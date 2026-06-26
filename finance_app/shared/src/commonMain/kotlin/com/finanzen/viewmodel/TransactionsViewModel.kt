package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TransactionsViewModel(
    private val txRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
) : ViewModel() {

    val transactions: StateFlow<List<TransactionRow>> =
        txRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> =
        categoryRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lectura directa (no depende de que el flow esté activo) para precargar el formulario de edición. */
    fun transactionById(id: Long): TransactionRow? = txRepo.byId(id)

    /** Crea (id null) o actualiza una transacción. La moneda se toma de la cuenta elegida. */
    fun save(
        id: Long?,
        accountId: Long,
        categoryId: Long?,
        amountMinor: Long,
        kind: String,
        note: String,
        dateEpochDay: Long,
    ) {
        val currency = accounts.value.firstOrNull { it.id == accountId }?.currency
            ?: accountRepo.all().firstOrNull { it.id == accountId }?.currency
            ?: "USD"
        if (id == null) {
            txRepo.add(accountId, categoryId, amountMinor, currency, dateEpochDay, note, kind)
        } else {
            txRepo.update(id, accountId, categoryId, amountMinor, currency, dateEpochDay, note, kind)
        }
    }

    /** Crea o actualiza una transferencia entre dos cuentas (misma moneda; la app no maneja FX). */
    fun saveTransfer(
        id: Long?,
        fromAccountId: Long,
        toAccountId: Long,
        amountMinor: Long,
        note: String,
        dateEpochDay: Long,
    ) {
        val currency = accounts.value.firstOrNull { it.id == fromAccountId }?.currency
            ?: accountRepo.all().firstOrNull { it.id == fromAccountId }?.currency
            ?: "USD"
        if (id == null) {
            txRepo.addTransfer(fromAccountId, toAccountId, amountMinor, currency, dateEpochDay, note)
        } else {
            txRepo.updateTransfer(id, fromAccountId, toAccountId, amountMinor, currency, dateEpochDay, note)
        }
    }

    fun delete(id: Long) = txRepo.delete(id)
}
