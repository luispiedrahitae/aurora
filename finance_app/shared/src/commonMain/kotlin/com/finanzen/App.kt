package com.finanzen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.finanzen.ui.navigation.AppNav
import com.finanzen.ui.screens.LockScreen
import com.finanzen.ui.theme.FinanZenTheme
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
    val darkTheme = when (theme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    FinanZenTheme(darkTheme = darkTheme, accent = accent, useDynamic = dynamicColor) {
        val securityVm: SecurityViewModel = koinInject()
        val lockState by securityVm.state.collectAsState()
        when (lockState) {
            LockState.Locked -> LockScreen(securityVm)
            LockState.Unlocked -> AppNav()
        }
    }
}
