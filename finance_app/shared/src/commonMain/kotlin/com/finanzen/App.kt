package com.finanzen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.finanzen.data.CURRENCY_LOCALE_INFO
import com.finanzen.data.DEFAULT_LOCALE_INFO
import com.finanzen.ui.navigation.AppNav
import com.finanzen.ui.screens.LockScreen
import com.finanzen.ui.theme.FinanZenTheme
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.MoneyFormat
import com.finanzen.viewmodel.LockState
import com.finanzen.viewmodel.SecurityViewModel
import com.finanzen.viewmodel.SettingsViewModel
import com.finanzen.viewmodel.ThemeMode
import org.koin.compose.koinInject

@Composable
fun App() {
    val settingsVm: SettingsViewModel = koinInject()
    val theme by settingsVm.theme.collectAsState()
    val accent by settingsVm.accent.collectAsState()
    val dynamicColor by settingsVm.dynamicColor.collectAsState()
    val symbolPos by settingsVm.symbolPosition.collectAsState()
    val baseCurrency by settingsVm.baseCurrency.collectAsState()
    val darkTheme = when (theme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    // Formato de moneda global (símbolo desde Currency.symbol + posición elegida) para toda la app.
    val moneyFormat = remember(symbolPos, settingsVm.currencies) {
        MoneyFormat(symbolPos, settingsVm.currencies.associateBy { it.code })
    }
    // Convención de fecha (orden + idioma de mes) derivada de la moneda base, para toda la app.
    val dateLocale = remember(baseCurrency) {
        CURRENCY_LOCALE_INFO[baseCurrency] ?: DEFAULT_LOCALE_INFO
    }
    FinanZenTheme(darkTheme = darkTheme, accent = accent, useDynamic = dynamicColor) {
        CompositionLocalProvider(LocalMoneyFormat provides moneyFormat, LocalDateLocale provides dateLocale) {
            val securityVm: SecurityViewModel = koinInject()
            val lockState by securityVm.state.collectAsState()
            when (lockState) {
                LockState.Locked -> LockScreen(securityVm)
                LockState.Unlocked -> AppNav()
            }
        }
    }
}
