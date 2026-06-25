package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.CurrencyRepository
import com.finanzen.data.SettingsRepository
import com.finanzen.db.Currency
import com.finanzen.ui.theme.AccentPreset
import com.finanzen.ui.theme.dynamicColorSupported
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

    val accents: List<AccentPreset> = AccentPreset.entries

    /** true solo donde Material You tiene efecto; decide si el toggle de color dinámico se muestra. */
    val dynamicSupported: Boolean = dynamicColorSupported()

    private val mutableAccent = MutableStateFlow(AccentPreset.fromKey(repo.accent()))
    val accent: StateFlow<AccentPreset> = mutableAccent.asStateFlow()

    private val mutableDynamic = MutableStateFlow(dynamicSupported && repo.dynamicColorEnabled())
    val dynamicColor: StateFlow<Boolean> = mutableDynamic.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        repo.setThemeMode(mode.name.lowercase())
        mutableTheme.value = mode
    }

    fun setAccent(preset: AccentPreset) {
        repo.setAccent(preset.name)
        mutableAccent.value = preset
    }

    fun setDynamicColor(enabled: Boolean) {
        repo.setDynamicColorEnabled(enabled)
        mutableDynamic.value = enabled
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
