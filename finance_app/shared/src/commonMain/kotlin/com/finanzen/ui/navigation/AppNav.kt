package com.finanzen.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = TopDestination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopDestination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
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
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.Dashboard.route,
            modifier = Modifier.padding(inner),
        ) {
            composable(TopDestination.Dashboard.route) { DashboardScreen() }
            composable(TopDestination.Transactions.route) {
                TransactionsScreen(
                    onAdd = { navController.navigate("tx_form") },
                    onEdit = { id -> navController.navigate("tx_form/$id") },
                )
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
        }
    }
}
