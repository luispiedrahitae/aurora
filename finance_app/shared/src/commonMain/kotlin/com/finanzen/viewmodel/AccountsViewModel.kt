package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.AccountRepository
import com.finanzen.data.CardRepository
import com.finanzen.data.InstallmentPlanRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.data.TransactionRepository
import com.finanzen.db.Account
import com.finanzen.db.Card
import com.finanzen.db.InstallmentPlan
import com.finanzen.db.TransactionRow
import com.finanzen.domain.Money
import com.finanzen.platform.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class AccountsViewModel(
    private val accountRepo: AccountRepository,
    txRepo: TransactionRepository,
    private val cardRepo: CardRepository,
    private val planRepo: InstallmentPlanRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> =
        accountRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saldo actual por cuenta (id → minor) = saldo inicial + ingresos − gastos ± transferencias. */
    val balances: StateFlow<Map<Long, Long>> =
        combine(accountRepo.observeAll(), txRepo.observeAll()) { accts, txs -> computeBalances(accts, txs) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Metadatos de tarjeta por cuenta (cupo/corte/pago/interés) para las cuentas de crédito. */
    val cards: StateFlow<Map<Long, Card>> =
        cardRepo.observeAll().map { list -> list.associateBy { it.accountId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Planes de cuotas agrupados por cuenta (vía su tarjeta de respaldo). */
    val plansByAccount: StateFlow<Map<Long, List<InstallmentPlan>>> =
        combine(cardRepo.observeAll(), planRepo.observeAll()) { cardsList, plans ->
            val accountByCard = cardsList.associate { it.id to it.accountId }
            plans.groupBy { accountByCard[it.cardId] ?: -1L }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** La app es de moneda única; las cuentas nuevas heredan la moneda base (ver Ajustes). */
    val baseCurrency: String get() = settingsRepo.baseCurrency()

    val accountTypes: List<String> = listOf("CASH", "DEBIT", "SAVINGS", "CREDIT")

    /** "12.50" → 1250 (centavos). null si el texto no es numérico válido. */
    fun parseAmountToMinor(text: String): Long? = Money.parseToMinor(text)

    /**
     * Crea una cuenta según su tipo. Para crédito guarda los metadatos de tarjeta (cupo, corte, pago,
     * interés) en la tabla Card; el resto usa el saldo inicial. Devuelve el id de la cuenta creada.
     */
    fun addAccount(
        type: String,
        name: String,
        amountMinor: Long = 0,
        creditLimitMinor: Long? = null,
        cutoffDay: Long? = null,
        dueDay: Long? = null,
        interestRate: Double? = null,
    ): Long? {
        if (name.isBlank()) return null
        return if (type == "CREDIT") {
            val accountId = accountRepo.add(name.trim(), "CREDIT", settingsRepo.baseCurrency(), openingBalanceMinor = 0)
            cardRepo.add(
                accountId = accountId,
                last4 = "",
                network = "OTRA",
                creditLimitMinor = creditLimitMinor,
                cutoffDay = cutoffDay,
                dueDay = dueDay,
                interestRate = interestRate,
            )
            scheduleCardReminders(accountId, name.trim(), cutoffDay, dueDay)
            accountId
        } else {
            accountRepo.add(name.trim(), type, settingsRepo.baseCurrency(), openingBalanceMinor = amountMinor)
        }
    }

    /**
     * Programa recordatorios de corte/pago si el usuario los habilitó en opciones.
     * `// ponytail:` one-shot a la próxima ocurrencia del día; la recurrencia mensual real se añade
     * reprogramando al abrir la app si hace falta.
     */
    private fun scheduleCardReminders(accountId: Long, name: String, cutoffDay: Long?, dueDay: Long?) {
        if (!settingsRepo.cardNotificationsEnabled()) return
        cutoffDay?.let { scheduler.scheduleReminder(CUTOFF_NOTIF_BASE + accountId, "Corte de $name", "Hoy es el corte de tu tarjeta.", nextDayOfMonthEpoch(it.toInt())) }
        dueDay?.let { scheduler.scheduleReminder(DUE_NOTIF_BASE + accountId, "Pago de $name", "Hoy vence el pago de tu tarjeta.", nextDayOfMonthEpoch(it.toInt())) }
    }

    /** Próxima fecha (epoch day) con ese día del mes, contando desde hoy. Acota a la duración del mes. */
    private fun nextDayOfMonthEpoch(day: Int): Long {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        fun candidate(base: LocalDate): LocalDate {
            val first = LocalDate(base.year, base.month, 1)
            val len = (first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays())
            return LocalDate(base.year, base.month, day.coerceIn(1, len))
        }
        val thisMonth = candidate(today)
        val target = if (thisMonth >= today) thisMonth else candidate(today.plus(DatePeriod(months = 1)))
        return target.toEpochDays().toLong()
    }

    /**
     * Borra la cuenta y su tarjeta de respaldo (si es de crédito). Borramos la tarjeta a mano en vez
     * de depender de ON DELETE CASCADE porque el enforcement de FKs no está garantizado en SQLite.
     */
    fun delete(id: Long) {
        cardRepo.byAccount(id)?.let { cardRepo.delete(it.id) }
        accountRepo.delete(id)
    }

    fun deletePlan(id: Long) = planRepo.delete(id)

    companion object {
        // Offsets para que los ids de notificación de tarjeta no choquen entre sí ni con otros.
        private const val CUTOFF_NOTIF_BASE = 2_000_000L
        private const val DUE_NOTIF_BASE = 3_000_000L

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
