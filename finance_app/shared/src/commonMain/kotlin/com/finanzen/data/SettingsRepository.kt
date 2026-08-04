package com.finanzen.data

import com.finanzen.db.FinanzenDb

/** Lectura/escritura de preferencias de UI sobre la tabla `Setting` (key/value). */
class SettingsRepository(private val db: FinanzenDb) {

    fun themeMode(): String = db.settingQueries.get(KEY_THEME).executeAsOneOrNull() ?: THEME_DARK

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

    /** Reduce/omite animaciones de la UI (accesibilidad). Apagado por defecto. */
    fun reduceMotionEnabled(): Boolean = db.settingQueries.get(KEY_REDUCE_MOTION).executeAsOneOrNull()?.toBooleanStrictOrNull() ?: false

    fun setReduceMotionEnabled(enabled: Boolean) = db.settingQueries.put(KEY_REDUCE_MOTION, enabled.toString())

    /** Posición del símbolo de moneda en los montos: auto (default, según CLDR) | prefix | suffix | none. */
    fun symbolPosition(): String = db.settingQueries.get(KEY_SYMBOL_POS).executeAsOneOrNull() ?: "auto"

    fun setSymbolPosition(pos: String) = db.settingQueries.put(KEY_SYMBOL_POS, pos)

    /** Días de antelación del recordatorio de cobro de suscripciones. Global; default 2. */
    fun reminderDaysBefore(): Long = db.settingQueries.get(KEY_NOTIFY_REMIND_DAYS).executeAsOneOrNull()?.toLongOrNull() ?: 2

    fun setReminderDaysBefore(days: Long) = db.settingQueries.put(KEY_NOTIFY_REMIND_DAYS, days.toString())

    /** Meta de la tasa de ahorro mostrada en el gauge del Resumen, en porcentaje (0-100). Default 20. */
    fun savingsGoalPct(): Long = db.settingQueries.get(KEY_SAVINGS_GOAL_PCT).executeAsOneOrNull()?.toLongOrNull() ?: 20

    fun setSavingsGoalPct(pct: Long) = db.settingQueries.put(KEY_SAVINGS_GOAL_PCT, pct.toString())

    /** Activación explícita del asistente IA on-device (opt-in). Apagado por defecto. */
    fun assistantOptIn(): Boolean = db.settingQueries.get(KEY_ASSISTANT_OPT_IN).executeAsOneOrNull()?.toBooleanStrictOrNull() ?: false

    fun setAssistantOptIn(enabled: Boolean) = db.settingQueries.put(KEY_ASSISTANT_OPT_IN, enabled.toString())

    /** Estado del modelo del asistente: NOT_DOWNLOADED | DOWNLOADING | READY | CORRUPT | ERROR. */
    fun assistantModelState(): String = db.settingQueries.get(KEY_ASSISTANT_MODEL_STATE).executeAsOneOrNull() ?: STATE_NOT_DOWNLOADED

    fun setAssistantModelState(state: String) = db.settingQueries.put(KEY_ASSISTANT_MODEL_STATE, state)

    /** Moneda única de la app. La app no maneja FX; todos los montos se asumen en esta moneda. */
    fun baseCurrency(): String = db.settingQueries.get(KEY_CURRENCY).executeAsOneOrNull() ?: DEFAULT_CURRENCY

    /**
     * Cambia la moneda base y re-etiqueta cuentas, transacciones, presupuestos, cupos de tarjeta y
     * planes de cuotas existentes, reescalando cada monto por la diferencia de decimales entre la
     * moneda vieja y la nueva para que el número que el usuario ve se mantenga igual (40.000 en COP
     * sigue siendo 40.000 al pasar a USD). No es conversión de moneda: `rateToBase` sigue sin usarse,
     * esto solo corrige que cada moneda representa sus unidades mínimas con una cantidad de decimales
     * distinta (COP=0, USD=2...). Suscripciones/gastos recurrentes tienen su propia columna `currency`
     * independiente y quedan fuera de este re-etiquetado. Solo se re-etiquetan cuentas/transacciones
     * que estaban en la moneda base ANTERIOR (`WHERE currency = oldCode`): una cuenta que ya estaba en
     * una moneda distinta (p. ej. restaurada de un backup multi-moneda) no se toca.
     */
    fun setBaseCurrency(code: String) {
        val oldCode = baseCurrency()
        if (oldCode == code) return
        val oldDecimals = db.currencyQueries.selectByCode(oldCode).executeAsOneOrNull()?.decimals?.toInt() ?: 2
        val newDecimals = db.currencyQueries.selectByCode(code).executeAsOneOrNull()?.decimals?.toInt() ?: 2
        val diff = newDecimals - oldDecimals
        val mult = if (diff >= 0) pow10(diff) else 1L
        val div = if (diff < 0) pow10(-diff) else 1L
        db.transaction {
            db.settingQueries.put(KEY_CURRENCY, code)
            db.accountQueries.setAllCurrency(newCode = code, mult = mult, div = div, oldCode = oldCode)
            db.transactionQueries.setAllCurrency(newCode = code, mult = mult, div = div, oldCode = oldCode)
            db.budgetQueries.rescaleAllLimits(mult, div)
            db.cardQueries.rescaleAllCreditLimits(mult, div)
            db.installmentPlanQueries.rescaleAllTotals(mult, div)
        }
    }

    private fun pow10(n: Int): Long {
        var r = 1L
        repeat(n) { r *= 10 }
        return r
    }

    companion object {
        const val KEY_THEME = "ui.theme"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val KEY_CURRENCY = "app.currency"
        const val DEFAULT_CURRENCY = "USD"
        const val KEY_ACCENT = "app.accent"
        const val DEFAULT_ACCENT = "Teal"
        const val KEY_DYNAMIC = "app.dynamic_color"
        const val KEY_NOTIFY_CARDS = "notif.cards"
        const val KEY_NOTIFY_BUDGET = "notif.budget"
        const val KEY_NOTIFY_REMIND_DAYS = "notif.remind_days"
        const val KEY_SAVINGS_GOAL_PCT = "ui.savings_goal_pct"
        const val KEY_HIDE_AMOUNTS = "ui.hide_amounts"
        const val KEY_REDUCE_MOTION = "ui.reduce_motion"
        const val KEY_SYMBOL_POS = "ui.symbol_pos"
        const val KEY_ASSISTANT_OPT_IN = "assistant.opt_in"
        const val KEY_ASSISTANT_MODEL_STATE = "assistant.model_state"
        const val STATE_NOT_DOWNLOADED = "NOT_DOWNLOADED"
        const val STATE_DOWNLOADING = "DOWNLOADING"
        const val STATE_READY = "READY"
        const val STATE_CORRUPT = "CORRUPT"
        const val STATE_ERROR = "ERROR"
    }
}
