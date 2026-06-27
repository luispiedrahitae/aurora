package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

class TransactionsViewModel(
    private val txRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val cardRepo: CardRepository,
    private val planRepo: InstallmentPlanRepository,
    private val budgetRepo: BudgetRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    val transactions: StateFlow<List<TransactionRow>> =
        txRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> =
        categoryRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lectura directa (no depende de que el flow esté activo) para precargar el formulario de edición. */
    fun transactionById(id: Long): TransactionRow? = txRepo.byId(id)

    /**
     * Crea (id null) o actualiza una transacción. La moneda se toma de la cuenta elegida.
     * Si es un gasto con tarjeta de crédito y [installments] > 1, registra un plan de cuotas y lo
     * enlaza. `// ponytail:` captura mínima; la amortización/edición de cuotas se diseña aparte.
     */
    fun save(
        id: Long?,
        accountId: Long,
        categoryId: Long?,
        amountMinor: Long,
        kind: String,
        note: String,
        dateEpochDay: Long,
        installments: Long = 1,
        interestRate: Double? = null,
    ) {
        val account = accounts.value.firstOrNull { it.id == accountId }
            ?: accountRepo.all().firstOrNull { it.id == accountId }
        val currency = account?.currency ?: "USD"
        if (id == null) {
            val planId = maybeCreatePlan(account, kind, amountMinor, installments, interestRate, dateEpochDay, note)
            txRepo.add(accountId, categoryId, amountMinor, currency, dateEpochDay, note, kind, planId)
            maybeNotifyBudget(categoryId, kind, amountMinor, dateEpochDay)
        } else {
            txRepo.update(id, accountId, categoryId, amountMinor, currency, dateEpochDay, note, kind)
        }
    }

    /**
     * Si el toggle está activo y este gasto hace que la categoría cruce su límite del mes, notifica.
     * Solo al cruzar (antes < límite, ahora ≥ límite) para no repetir el aviso en cada gasto.
     */
    private fun maybeNotifyBudget(categoryId: Long?, kind: String, amountMinor: Long, dateEpochDay: Long) {
        if (kind != "EXPENSE" || categoryId == null || !settingsRepo.budgetNotificationsEnabled()) return
        val period = BudgetsViewModel.currentPeriodMonth()
        if (monthOf(dateEpochDay) != period) return
        val limit = budgetRepo.limitFor(categoryId, period)
        if (limit <= 0) return
        val spentAfter = txRepo.all()
            .filter { it.kind == "EXPENSE" && it.categoryId == categoryId && monthOf(it.date) == period }
            .sumOf { it.amountMinor }
        val spentBefore = spentAfter - amountMinor
        if (spentBefore < limit && spentAfter >= limit) {
            val name = categories.value.firstOrNull { it.id == categoryId }?.name ?: "tu presupuesto"
            scheduler.notifyNow(BUDGET_NOTIF_BASE + categoryId, "Presupuesto alcanzado", "Alcanzaste el límite de $name este mes.")
        }
    }

    /** Crea un plan de cuotas solo para gastos con tarjeta de crédito y más de una cuota. */
    private fun maybeCreatePlan(
        account: Account?,
        kind: String,
        amountMinor: Long,
        installments: Long,
        interestRate: Double?,
        dateEpochDay: Long,
        note: String,
    ): Long? {
        if (kind != "EXPENSE" || account?.type != "CREDIT" || installments <= 1) return null
        val card = cardRepo.byAccount(account.id) ?: return null
        return planRepo.add(card.id, amountMinor, installments, interestRate ?: 0.0, dateEpochDay, note)
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

    private fun monthOf(epochDay: Long): Long = LocalDate.fromEpochDays(epochDay.toInt()).let { (it.year * 100 + it.monthNumber).toLong() }

    private companion object {
        // Offset para que los ids de notificación de presupuesto no choquen con los recordatorios.
        const val BUDGET_NOTIF_BASE = 1_000_000L
    }
}
