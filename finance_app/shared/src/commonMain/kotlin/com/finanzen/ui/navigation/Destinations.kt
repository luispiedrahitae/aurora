package com.finanzen.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector

/** [icon] se usa sin seleccionar; [selectedIcon] (filled) cuando el tab está activo — patrón M3. */
enum class TopDestination(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Dashboard("dashboard", "Resumen", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    Transactions("transactions", "Movimientos", Icons.Outlined.SwapVert, Icons.Filled.SwapVert),
    Accounts("accounts_tab", "Cuentas", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    Budgets("budgets_tab", "Presupuesto", Icons.Outlined.PieChart, Icons.Filled.PieChart),
    More("more", "Más", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz),
}
