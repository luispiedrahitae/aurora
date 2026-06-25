package com.finanzen.data

import com.finanzen.db.FinanzenDb

/** Lectura/escritura de preferencias de UI sobre la tabla `Setting` (key/value). */
class SettingsRepository(private val db: FinanzenDb) {

    fun themeMode(): String = db.settingQueries.get(KEY_THEME).executeAsOneOrNull() ?: THEME_SYSTEM

    fun setThemeMode(mode: String) = db.settingQueries.put(KEY_THEME, mode)

    /** Moneda única de la app. La app no maneja FX; todos los montos se asumen en esta moneda. */
    fun baseCurrency(): String = db.settingQueries.get(KEY_CURRENCY).executeAsOneOrNull() ?: DEFAULT_CURRENCY

    /**
     * Cambia la moneda base y re-etiqueta cuentas y transacciones existentes.
     * ponytail: re-etiqueta sin convertir montos (no hay FX). Si algún día se soporta multi-moneda
     * real, aquí iría la conversión con tasas.
     */
    fun setBaseCurrency(code: String) = db.transaction {
        db.settingQueries.put(KEY_CURRENCY, code)
        db.accountQueries.setAllCurrency(code)
        db.transactionQueries.setAllCurrency(code)
    }

    companion object {
        const val KEY_THEME = "ui.theme"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val KEY_CURRENCY = "app.currency"
        const val DEFAULT_CURRENCY = "USD"
    }
}
