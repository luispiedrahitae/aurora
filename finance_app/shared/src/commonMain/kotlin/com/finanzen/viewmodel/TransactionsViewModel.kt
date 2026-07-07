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
import com.finanzen.domain.InstallmentMath
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

    val allAccounts: StateFlow<List<Account>> =
        accountRepo.observeAllIncludingArchived().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> =
        categoryRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lectura directa (no depende de que el flow esté activo) para precargar el formulario de edición. */
    fun transactionById(id: Long): TransactionRow? = txRepo.byId(id)

    /** Tarjeta asociada a la cuenta, si es de tipo CREDIT — para el preview de cuotas en el formulario. */
    fun cardFor(accountId: Long) = cardRepo.byAccount(accountId)

    /**
     * Crea (id null) o actualiza una transacción. La moneda se toma de la cuenta elegida.
     * Si es un gasto con tarjeta de crédito y [installments] > 1, [amountMinor] es el monto TOTAL
     * de la compra: se crea un plan de cuotas (interés tomado de la tarjeta) y esta transacción
     * queda como la cuota 1 (monto = cuota mensual, no el total); las cuotas 2..N las genera
     * `AccountsViewModel.postDueInstallments()` automáticamente mes a mes.
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
    ) {
        val account = accounts.value.firstOrNull { it.id == accountId }
            ?: accountRepo.allIncludingArchived().firstOrNull { it.id == accountId }
        val currency = account?.currency ?: "USD"
        if (id == null) {
            val plan = maybeCreatePlan(account, kind, categoryId, amountMinor, installments, dateEpochDay, note)
            val postedAmount = plan?.firstInstallmentMinor ?: amountMinor
            txRepo.add(accountId, categoryId, postedAmount, currency, dateEpochDay, note, kind, plan?.planId)
            maybeNotifyBudget(categoryId, kind, postedAmount, dateEpochDay)
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

    private data class PlanCreation(val planId: Long, val firstInstallmentMinor: Long)

    /** Crea un plan de cuotas solo para gastos con tarjeta de crédito y más de una cuota. */
    private fun maybeCreatePlan(
        account: Account?,
        kind: String,
        categoryId: Long?,
        amountMinor: Long,
        installments: Long,
        dateEpochDay: Long,
        note: String,
    ): PlanCreation? {
        if (kind != "EXPENSE" || account?.type != "CREDIT" || installments <= 1) return null
        val card = cardRepo.byAccount(account.id) ?: return null
        val rate = card.interestRate ?: 0.0
        val planId = planRepo.add(card.id, categoryId, amountMinor, installments, rate, dateEpochDay, note)
        val firstInstallment = InstallmentMath.monthlyPaymentMinor(amountMinor, installments, rate)
        return PlanCreation(planId, firstInstallment)
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
            ?: accountRepo.allIncludingArchived().firstOrNull { it.id == fromAccountId }?.currency
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
