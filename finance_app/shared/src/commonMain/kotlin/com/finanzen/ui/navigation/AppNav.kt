package com.finanzen.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finanzen.ui.components.AutoSizeText
import com.finanzen.ui.components.SpeedDialAction
import com.finanzen.ui.components.SpeedDialFab
import com.finanzen.ui.screens.AboutScreen
import com.finanzen.ui.screens.AccountsTabScreen
import com.finanzen.ui.screens.AnalysisScreen
import com.finanzen.ui.screens.BackupScreen
import com.finanzen.ui.screens.BudgetsScreen
import com.finanzen.ui.screens.CalendarScreen
import com.finanzen.ui.screens.CategoriesScreen
import com.finanzen.ui.screens.CurrencyScreen
import com.finanzen.ui.screens.DashboardScreen
import com.finanzen.ui.screens.MoreScreen
import com.finanzen.ui.screens.NotificationsScreen
import com.finanzen.ui.screens.ReportsScreen
import com.finanzen.ui.screens.SecuritySettingsScreen
import com.finanzen.ui.screens.SettingsScreen
import com.finanzen.ui.screens.SubscriptionsScreen
import com.finanzen.ui.screens.TransactionFormScreen
import com.finanzen.ui.screens.TransactionsScreen
import com.finanzen.ui.theme.LocalFinanceColors

// Ancho a partir del cual mostramos rail lateral en vez de bottom bar (escritorio/tablet).
private val WIDE_BREAKPOINT = 600.dp

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showNav = TopDestination.entries.any { it.route == currentRoute }
    // El FAB desplegable crea movimientos/suscripciones; solo en Movimientos, para no competir con
    // las acciones propias de Resumen y Análisis.
    val showFab = currentRoute == TopDestination.Transactions.route

    val onSelect: (TopDestination) -> Unit = { dest ->
        if (currentRoute != dest.route) {
            navController.navigate(dest.route) {
                popUpTo(TopDestination.Transactions.route) {
                    saveState = true
                    inclusive = false
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val finance = LocalFinanceColors.current
    // Orden de arriba hacia abajo en el desplegable; "Gasto" queda junto al FAB (lo más usado).
    val speedDialActions = listOf(
        SpeedDialAction("Suscripción", Icons.Outlined.Repeat, MaterialTheme.colorScheme.primary) {
            navController.navigate("subscriptions?add=1")
        },
        SpeedDialAction("Transferencia", Icons.Outlined.SwapHoriz, finance.neutral) {
            navController.navigate("tx_form?kind=TRANSFER")
        },
        SpeedDialAction("Ingreso", Icons.Outlined.ArrowUpward, finance.income) {
            navController.navigate("tx_form?kind=INCOME")
        },
        SpeedDialAction("Gasto", Icons.Outlined.ArrowDownward, finance.expense) {
            navController.navigate("tx_form?kind=EXPENSE")
        },
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_BREAKPOINT && showNav) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    TopDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onSelect(dest) },
                            icon = { Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label) },
                            label = {
                                AutoSizeText(
                                    text = dest.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxFontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    minFontSize = 8.sp,
                                )
                            },
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxSize()) {
                    AppNavHost(navController)
                    if (showFab) SpeedDialFab(speedDialActions, contentDescription = "Agregar movimiento")
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    if (showNav) {
                        NavigationBar {
                            TopDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { onSelect(dest) },
                                    icon = { Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label) },
                                    label = {
                                        AutoSizeText(
                                            text = dest.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxFontSize = MaterialTheme.typography.labelSmall.fontSize,
                                            minFontSize = 8.sp,
                                        )
                                    },
                                )
                            }
                        }
                    }
                },
            ) { inner ->
                // Solo el inset inferior (nav bar). El superior lo aporta cada pantalla una sola vez:
                // las que tienen TopAppBar vía su barra; las "desnudas" vía windowInsetsPadding propio.
                Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
                    AppNavHost(navController)
                    if (showFab) SpeedDialFab(speedDialActions, contentDescription = "Agregar movimiento")
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = TopDestination.Transactions.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(TopDestination.Dashboard.route) {
            DashboardScreen()
        }
        composable(TopDestination.Transactions.route) {
            TransactionsScreen(onEdit = { id -> navController.navigate("tx_form/$id") })
        }
        composable(TopDestination.Accounts.route) { AccountsTabScreen() }
        composable(TopDestination.Budgets.route) { BudgetsScreen() }
        composable(TopDestination.More.route) {
            MoreScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(
            "subscriptions?add={add}",
            arguments = listOf(
                navArgument("add") {
                    type = NavType.StringType
                    defaultValue = "0"
                },
            ),
        ) { entry ->
            SubscriptionsScreen(
                openAddInitially = entry.arguments?.getString("add") == "1",
                onBack = { navController.popBackStack() },
            )
        }
        composable("reports") {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable("security") {
            SecuritySettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("backup") {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("notifications") {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable("currency") {
            CurrencyScreen(onBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(
            "tx_form?kind={kind}",
            arguments = listOf(
                navArgument("kind") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            TransactionFormScreen(
                transactionId = null,
                initialKind = entry.arguments?.getString("kind"),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "tx_form/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            TransactionFormScreen(
                transactionId = entry.arguments?.getLong("id"),
                onBack = { navController.popBackStack() },
            )
        }
        composable("categories") {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable("analysis") {
            AnalysisScreen(onBack = { navController.popBackStack() })
        }
        composable("calendar") {
            CalendarScreen(
                onEdit = { id -> navController.navigate("tx_form/$id") },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
