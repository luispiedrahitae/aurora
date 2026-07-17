package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.BudgetRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.CategoryRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.InvestmentRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.SubscriptionRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Category
import com.finanzen.db.TransactionRow
import com.finanzen.domain.InstallmentMath
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    private val investRepo: InvestmentRepository,
    private val subsRepo: SubscriptionRepository,
) : ViewModel() {

    // Los movimientos ADJUSTMENT (saldo inicial/ajuste de cuenta) solo se muestran en el historial
    // expandido de Cuentas (AccountsTabScreen), no en Movimientos.
    val transactions: StateFlow<List<TransactionRow>> =
        txRepo.observeAll().map { list -> list.filter { it.kind != "ADJUSTMENT" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Crea una subcategoría (sin ícono) bajo [parentId] y devuelve su id para auto-seleccionarla. */
    fun addSubcategory(name: String, kind: String, parentId: Long): Long? {
        if (name.isBlank()) return null
        return categoryRepo.addAndGetId(name = name.trim(), kind = kind, parentId = parentId, icon = "")
    }

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

    /**
     * Borra la transacción y, si era la última vinculada a una inversión o suscripción, limpia
     * también esa fila huérfana (una inversión periódica o una suscripción con más cobros
     * pendientes conserva sus demás transacciones y no se toca).
     */
    fun delete(id: Long) {
        val tx = txRepo.byId(id)
        txRepo.delete(id)
        tx?.investmentId?.let { investId ->
            if (txRepo.contributionsForInvestment(investId).isEmpty()) investRepo.delete(investId)
        }
        tx?.subscriptionId?.let { subId ->
            if (txRepo.contributionsForSubscription(subId).isEmpty()) {
                subsRepo.delete(subId)
                scheduler.cancel(subId)
            }
        }
    }

    /** Motivo por el que un movimiento no se puede borrar directamente desde Movimientos. */
    sealed interface DeleteBlock {
        data class Subscription(val name: String) : DeleteBlock
        data class Investment(val name: String) : DeleteBlock
    }

    /**
     * Null si el movimiento se puede borrar. Si está vinculado a una suscripción o a una
     * inversión periódica que todavía existen, hay que gestionar el borrado desde su propia
     * pantalla primero (una vez borrado el padre, el movimiento queda desbloqueado aquí).
     */
    fun deleteBlockReason(id: Long): DeleteBlock? {
        val tx = txRepo.byId(id) ?: return null
        tx.subscriptionId?.let { subId ->
            subsRepo.byId(subId)?.let { return DeleteBlock.Subscription(it.name) }
        }
        tx.investmentId?.let { investId ->
            investRepo.byId(investId)?.let { inv ->
                if (inv.periodic != 0L) return DeleteBlock.Investment(inv.name)
            }
        }
        return null
    }

    private fun monthOf(epochDay: Long): Long = LocalDate.fromEpochDays(epochDay.toInt()).let { (it.year * 100 + it.monthNumber).toLong() }

    private companion object {
        // Offset para que los ids de notificación de presupuesto no choquen con los recordatorios.
        const val BUDGET_NOTIF_BASE = 1_000_000L
    }
}
