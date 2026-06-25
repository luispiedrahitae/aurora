package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.CurrencyRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.db.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Preferencias de apariencia. Mantiene el tema en memoria (StateFlow) y lo persiste en `Setting`,
 * de forma que `App()` reacciona al instante y el valor sobrevive al reinicio.
 */
class SettingsViewModel(
    private val repo: SettingsRepository,
    currencyRepo: CurrencyRepository,
) : ViewModel() {

    private val mutableTheme = MutableStateFlow(parse(repo.themeMode()))
    val theme: StateFlow<ThemeMode> = mutableTheme.asStateFlow()

    val currencies: List<Currency> = currencyRepo.all()

    private val mutableCurrency = MutableStateFlow(repo.baseCurrency())
    val baseCurrency: StateFlow<String> = mutableCurrency.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        repo.setThemeMode(mode.name.lowercase())
        mutableTheme.value = mode
    }

    fun setBaseCurrency(code: String) {
        repo.setBaseCurrency(code)
        mutableCurrency.value = code
    }

    companion object {
        fun parse(raw: String): ThemeMode = when (raw.lowercase()) {
            SettingsRepository.THEME_LIGHT -> ThemeMode.LIGHT
            SettingsRepository.THEME_DARK -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }
}
