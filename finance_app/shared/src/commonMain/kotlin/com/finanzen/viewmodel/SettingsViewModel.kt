package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.CurrencyRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.db.Currency
import com.finanzen.domain.SymbolPosition
import com.finanzen.ui.theme.AccentPreset
import com.finanzen.ui.theme.MoneyFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencias de apariencia. Mantiene el tema en memoria (StateFlow) y lo persiste en `Setting`,
 * de forma que `App()` reacciona al instante y el valor sobrevive al reinicio.
 */
class SettingsViewModel(
    private val repo: SettingsRepository,
    currencyRepo: CurrencyRepository,
) : ViewModel() {

    val currencies: List<Currency> = currencyRepo.all()

    private val mutableCurrency = MutableStateFlow(repo.baseCurrency())
    val baseCurrency: StateFlow<String> = mutableCurrency.asStateFlow()

    val accents: List<AccentPreset> = AccentPreset.entries

    private val mutableAccent = MutableStateFlow(AccentPreset.fromKey(repo.accent()))
    val accent: StateFlow<AccentPreset> = mutableAccent.asStateFlow()

    private val mutableCardNotif = MutableStateFlow(repo.cardNotificationsEnabled())
    val cardNotifications: StateFlow<Boolean> = mutableCardNotif.asStateFlow()

    private val mutableBudgetNotif = MutableStateFlow(repo.budgetNotificationsEnabled())
    val budgetNotifications: StateFlow<Boolean> = mutableBudgetNotif.asStateFlow()

    private val mutableRemindDays = MutableStateFlow(repo.reminderDaysBefore())
    val remindDaysBefore: StateFlow<Long> = mutableRemindDays.asStateFlow()

    private val mutableSavingsGoal = MutableStateFlow(repo.savingsGoalPct())
    val savingsGoalPct: StateFlow<Long> = mutableSavingsGoal.asStateFlow()

    private val mutableHideAmounts = MutableStateFlow(repo.hideAmountsEnabled())
    val hideAmounts: StateFlow<Boolean> = mutableHideAmounts.asStateFlow()

    private val mutableReduceMotion = MutableStateFlow(repo.reduceMotionEnabled())
    val reduceMotion: StateFlow<Boolean> = mutableReduceMotion.asStateFlow()

    private val mutableSymbolPos = MutableStateFlow(MoneyFormat.parse(repo.symbolPosition()))
    val symbolPosition: StateFlow<SymbolPosition> = mutableSymbolPos.asStateFlow()

    fun setAccent(preset: AccentPreset) {
        repo.setAccent(preset.name)
        mutableAccent.value = preset
    }

    fun setCardNotifications(enabled: Boolean) {
        repo.setCardNotificationsEnabled(enabled)
        mutableCardNotif.value = enabled
    }

    fun setBudgetNotifications(enabled: Boolean) {
        repo.setBudgetNotificationsEnabled(enabled)
        mutableBudgetNotif.value = enabled
    }

    fun setRemindDaysBefore(days: Long) {
        repo.setReminderDaysBefore(days)
        mutableRemindDays.value = days
    }

    fun setSavingsGoalPct(pct: Long) {
        repo.setSavingsGoalPct(pct)
        mutableSavingsGoal.value = pct
    }

    fun setHideAmounts(enabled: Boolean) {
        repo.setHideAmountsEnabled(enabled)
        mutableHideAmounts.value = enabled
    }

    fun setReduceMotion(enabled: Boolean) {
        repo.setReduceMotionEnabled(enabled)
        mutableReduceMotion.value = enabled
    }

    fun setSymbolPosition(pos: SymbolPosition) {
        repo.setSymbolPosition(pos.name.lowercase())
        mutableSymbolPos.value = pos
    }

    fun setBaseCurrency(code: String) {
        repo.setBaseCurrency(code)
        mutableCurrency.value = code
    }
}
