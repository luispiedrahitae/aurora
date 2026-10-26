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
