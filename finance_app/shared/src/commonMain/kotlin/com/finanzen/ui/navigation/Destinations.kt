package com.finanzen.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    Transactions("transactions", "Transacciones", Icons.Outlined.SwapVert),
    Cards("cards", "Tarjetas", Icons.Outlined.CreditCard),
    Analysis("analysis", "Análisis", Icons.Outlined.Analytics),
    More("more", "Más", Icons.Outlined.MoreHoriz),
}
