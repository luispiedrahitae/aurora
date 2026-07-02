package com.finanzen.data

import com.finanzen.db.FinanzenDb

/** Lectura/escritura de preferencias de UI sobre la tabla `Setting` (key/value). */
class SettingsRepository(private val db: FinanzenDb) {

    fun themeMode(): String = db.settingQueries.get(KEY_THEME).executeAsOneOrNull() ?: THEME_SYSTEM

    fun setThemeMode(mode: String) = db.settingQueries.put(KEY_THEME, mode)

    /** Preset de acento (nombre del enum AccentPreset). */
    fun accent(): String = db.settingQueries.get(KEY_ACCENT).executeAsOneOrNull() ?: DEFAULT_ACCENT

    fun setAccent(key: String) = db.settingQueries.put(KEY_ACCENT, key)

    /**
     * Color dinámico (Material You). Apagado por defecto: la app se muestra con su acento de marca
     * (morado) en todos los dispositivos; el usuario puede activar Material You si lo prefiere.
     */
    fun dynamicColorEnabled(): Boolean = db.settingQueries.get(KEY_DYNAMIC).executeAsOneOrNull()?.toBooleanStrictOrNull() ?: false

    fun setDynamicColorEnabled(enabled: Boolean) = db.settingQueries.put(KEY_DYNAMIC, enabled.toString())

    /** Recordatorios de corte/pago de tarjetas de crédito. Apagado por defecto. */
    fun cardNotificationsEnabled(): Boolean = db.settingQueries.get(KEY_NOTIFY_CARDS).executeAsOneOrNull()?.toBooleanStrictOrNull() ?: false

    fun setCardNotificationsEnabled(enabled: Boolean) = db.settingQueries.put(KEY_NOTIFY_CARDS, enabled.toString())

    /** Aviso al alcanzar el límite de un presupuesto. Apagado por defecto. */
    fun budgetNotificationsEnabled(): Boolean = db.settingQueries.get(KEY_NOTIFY_BUDGET).executeAsOneOrNull()?.toBooleanStrictOrNull() ?: false

    fun setBudgetNotificationsEnabled(enabled: Boolean) = db.settingQueries.put(KEY_NOTIFY_BUDGET, enabled.toString())

    /** Oculta los montos del resumen (balance/ingresos/gastos) por privacidad. Apagado por defecto. */
    fun hideAmountsEnabled(): Boolean = db.settingQueries.get(KEY_HIDE_AMOUNTS).executeAsOneOrNull()?.toBooleanStrictOrNull() ?: false

    fun setHideAmountsEnabled(enabled: Boolean) = db.settingQueries.put(KEY_HIDE_AMOUNTS, enabled.toString())

    /** Posición del símbolo de moneda en los montos: prefix (default) | suffix | none. */
    fun symbolPosition(): String = db.settingQueries.get(KEY_SYMBOL_POS).executeAsOneOrNull() ?: "prefix"

    fun setSymbolPosition(pos: String) = db.settingQueries.put(KEY_SYMBOL_POS, pos)

    /** Días de antelación del recordatorio de cobro de suscripciones. Global; default 2. */
    fun reminderDaysBefore(): Long = db.settingQueries.get(KEY_NOTIFY_REMIND_DAYS).executeAsOneOrNull()?.toLongOrNull() ?: 2

    fun setReminderDaysBefore(days: Long) = db.settingQueries.put(KEY_NOTIFY_REMIND_DAYS, days.toString())

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
        const val KEY_ACCENT = "app.accent"
        const val DEFAULT_ACCENT = "Violet"
        const val KEY_DYNAMIC = "app.dynamic_color"
        const val KEY_NOTIFY_CARDS = "notif.cards"
        const val KEY_NOTIFY_BUDGET = "notif.budget"
        const val KEY_NOTIFY_REMIND_DAYS = "notif.remind_days"
        const val KEY_HIDE_AMOUNTS = "ui.hide_amounts"
        const val KEY_SYMBOL_POS = "ui.symbol_pos"
    }
}
