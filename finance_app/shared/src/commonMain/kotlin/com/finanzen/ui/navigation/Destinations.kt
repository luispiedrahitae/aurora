package com.finanzen.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    Transactions("transactions", "Movimientos", Icons.Outlined.SwapVert),
    Accounts("accounts_tab", "Cuentas", Icons.Outlined.AccountBalanceWallet),
    Budgets("budgets_tab", "Presupuesto", Icons.Outlined.PieChart),
    More("more", "Más", Icons.Outlined.MoreHoriz),
}
