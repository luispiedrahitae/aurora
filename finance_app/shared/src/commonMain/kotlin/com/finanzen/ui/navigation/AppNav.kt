package com.finanzen.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finanzen.ui.screens.AboutScreen
import com.finanzen.ui.screens.AccountsScreen
import com.finanzen.ui.screens.AnalysisScreen
import com.finanzen.ui.screens.BackupScreen
import com.finanzen.ui.screens.BudgetsScreen
import com.finanzen.ui.screens.CalendarScreen
import com.finanzen.ui.screens.CardsScreen
import com.finanzen.ui.screens.CategoriesScreen
import com.finanzen.ui.screens.DashboardScreen
import com.finanzen.ui.screens.MoreScreen
import com.finanzen.ui.screens.ReportsScreen
import com.finanzen.ui.screens.SecuritySettingsScreen
import com.finanzen.ui.screens.SettingsScreen
import com.finanzen.ui.screens.SubscriptionsScreen
import com.finanzen.ui.screens.TransactionFormScreen
import com.finanzen.ui.screens.TransactionsScreen

// Ancho a partir del cual mostramos rail lateral en vez de bottom bar (escritorio/tablet).
private val WIDE_BREAKPOINT = 600.dp

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showNav = TopDestination.entries.any { it.route == currentRoute }
    // El FAB central solo crea transacciones, así que solo aparece donde tiene sentido.
    // Cards/Subscriptions tienen su propio FAB; Análisis/Más no necesitan uno.
    val showFab = currentRoute == TopDestination.Dashboard.route || currentRoute == TopDestination.Transactions.route

    val onSelect: (TopDestination) -> Unit = { dest ->
        if (currentRoute != dest.route) {
            navController.navigate(dest.route) {
                popUpTo(TopDestination.Dashboard.route) {
                    saveState = true
                    inclusive = false
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val onAdd = { navController.navigate("tx_form") }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_BREAKPOINT && showNav) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    header = {
                        if (showFab) {
                            FloatingActionButton(onClick = onAdd) {
                                Icon(Icons.Outlined.Add, contentDescription = "Agregar transacción")
                            }
                        }
                    },
                ) {
                    TopDestination.entries.forEach { dest ->
                        NavigationRailItem(
                            selected = currentRoute == dest.route,
                            onClick = { onSelect(dest) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
                AppNavHost(navController, Modifier.weight(1f))
            }
        } else {
            Scaffold(
                bottomBar = {
                    if (showNav) {
                        NavigationBar {
                            TopDestination.entries.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { onSelect(dest) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (showFab) {
                        FloatingActionButton(onClick = onAdd) {
                            Icon(Icons.Outlined.Add, contentDescription = "Agregar transacción")
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.Center,
            ) { inner ->
                AppNavHost(navController, Modifier.padding(inner))
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = TopDestination.Dashboard.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(TopDestination.Dashboard.route) { DashboardScreen() }
        composable(TopDestination.Transactions.route) {
            TransactionsScreen(onEdit = { id -> navController.navigate("tx_form/$id") })
        }
        composable(TopDestination.Cards.route) { CardsScreen() }
        composable(TopDestination.Analysis.route) { AnalysisScreen() }
        composable(TopDestination.More.route) {
            MoreScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("subscriptions") {
            SubscriptionsScreen(onBack = { navController.popBackStack() })
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
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable("tx_form") {
            TransactionFormScreen(transactionId = null, onBack = { navController.popBackStack() })
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
        composable("accounts") {
            AccountsScreen(onBack = { navController.popBackStack() })
        }
        composable("budgets") {
            BudgetsScreen(onBack = { navController.popBackStack() })
        }
        composable("calendar") {
            CalendarScreen(
                onEdit = { id -> navController.navigate("tx_form/$id") },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
