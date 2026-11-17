package com.finanzen.ui.navigation

/** Los íconos viven fuera del enum: `LucideIcons` expone `Painter` vía `@Composable get()`
 *  (llama a `painterResource`), y los valores de constructor de un enum se evalúan fuera de
 *  composición. Ver [topDestinationIcon]. */
enum class TopDestination(val route: String, val label: String) {
    Dashboard("dashboard", "Resumen"),
    Transactions("transactions", "Movimientos"),
    Accounts("accounts_tab", "Cuentas"),
    Budgets("budgets_tab", "Presupuesto"),
    More("more", "Más"),
}

/** Rutas de las pantallas secundarias (se abren desde Más o desde otra pantalla, con flecha atrás). */
object Routes {
    const val SUBSCRIPTIONS = "subscriptions"
    const val INVESTMENTS = "investments"
    const val CALENDAR = "calendar"
    const val REPORTS = "reports"
    const val CATEGORIES = "categories"
    const val APPEARANCE = "settings"
    const val CURRENCY = "currency"
    const val NOTIFICATIONS = "notifications"
    const val SECURITY = "security"
    const val BACKUP = "backup"
    const val ABOUT = "about"
    const val SUGGESTIONS = "suggestions"
}
