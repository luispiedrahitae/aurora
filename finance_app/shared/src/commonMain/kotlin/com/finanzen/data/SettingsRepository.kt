package com.finanzen.data

import com.finanzen.db.FinanzenDb

/** Lectura/escritura de preferencias de UI sobre la tabla `Setting` (key/value). */
class SettingsRepository(private val db: FinanzenDb) {

    fun themeMode(): String = db.settingQueries.get(KEY_THEME).executeAsOneOrNull() ?: THEME_SYSTEM

    fun setThemeMode(mode: String) = db.settingQueries.put(KEY_THEME, mode)

    companion object {
        const val KEY_THEME = "ui.theme"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
}
