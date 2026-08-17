package com.finanzen.ui.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.finanzen.ui.components.flatFabElevation
import com.finanzen.ui.screens.AboutScreen
import com.finanzen.ui.screens.AccountsTabScreen
import com.finanzen.ui.screens.AssistantScreen
import com.finanzen.ui.screens.BackupScreen
import com.finanzen.ui.screens.BudgetsScreen
import com.finanzen.ui.screens.CalendarScreen
import com.finanzen.ui.screens.CategoriesScreen
import com.finanzen.ui.screens.CurrencyScreen
import com.finanzen.ui.screens.DashboardScreen
import com.finanzen.ui.screens.InvestmentsScreen
import com.finanzen.ui.screens.MoreScreen
import com.finanzen.ui.screens.NotificationsScreen
import com.finanzen.ui.screens.ReportsScreen
import com.finanzen.ui.screens.SecuritySettingsScreen
import com.finanzen.ui.screens.SettingsScreen
import com.finanzen.ui.screens.SubscriptionsScreen
import com.finanzen.ui.screens.TransactionFormScreen
import com.finanzen.ui.screens.TransactionsScreen
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalReduceMotion

// Ancho a partir del cual mostramos rail lateral en vez de bottom bar (escritorio/tablet).
private val WIDE_BREAKPOINT = 600.dp

// Pantalla donde el FAB de captura rápida y el asistente IA están disponibles.
private val FAB_ROUTES = setOf(TopDestination.Transactions.route)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showNav = TopDestination.entries.any { it.route == currentRoute }
    // El FAB desplegable crea movimientos/suscripciones; vive solo en Movimientos, la pantalla
    // dueña de esa acción. Resumen y Análisis son de solo lectura.
    val showFab = currentRoute in FAB_ROUTES
    var fabExpanded by remember { mutableStateOf(false) }

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
            navController.navigate("subscriptions?add=true")
        },
        SpeedDialAction("Inversión", Icons.Outlined.TrendingUp, MaterialTheme.colorScheme.primary) {
            navController.navigate("investments?add=true")
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
                // Riel translúcido (recorta el material de vidrio a "flat" hasta que haya blur de
                // fondo real disponible sin dependencias nuevas — ver Glass.kt) con un hairline en el
                // borde que toca el contenido, no un rectángulo completo.
                NavigationRail(
                    containerColor = finance.glassSurface,
                    modifier = Modifier.drawBehind {
                        drawLine(finance.glassBorder, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                    },
                ) {
                    TopDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onSelect(dest) },
                            icon = {
                                val iconModifier = if (dest == TopDestination.More) Modifier.size(20.dp) else Modifier
                                Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label, modifier = iconModifier)
                            },
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
                    if (showFab) {
                        SpeedDialFab(
                            speedDialActions,
                            expanded = fabExpanded,
                            onExpandedChange = { fabExpanded = it },
                            contentDescription = "Agregar movimiento",
                        )
                        if (!fabExpanded) {
                            SmallFloatingActionButton(
                                onClick = { navController.navigate("assistant") },
                                elevation = flatFabElevation(),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp + 56.dp + 12.dp),
                            ) {
                                Icon(Icons.Outlined.SmartToy, contentDescription = "Asistente IA")
                            }
                        }
                    }
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    if (showNav) {
                        NavigationBar(
                            containerColor = finance.glassSurface,
                            modifier = Modifier.drawBehind {
                                drawLine(finance.glassBorder, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                            },
                        ) {
                            TopDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { onSelect(dest) },
                                    icon = {
                                        val iconModifier = if (dest == TopDestination.More) Modifier.size(20.dp) else Modifier
                                        Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label, modifier = iconModifier)
                                    },
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
                    if (showFab) {
                        SpeedDialFab(
                            speedDialActions,
                            expanded = fabExpanded,
                            onExpandedChange = { fabExpanded = it },
                            contentDescription = "Agregar movimiento",
                        )
                        if (!fabExpanded) {
                            SmallFloatingActionButton(
                                onClick = { navController.navigate("assistant") },
                                elevation = flatFabElevation(),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp + 56.dp + 12.dp),
                            ) {
                                Icon(Icons.Outlined.SmartToy, contentDescription = "Asistente IA")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    // Las lambdas de transición del NavHost no son @Composable: el flag se captura aquí fuera.
    val reduceMotion = LocalReduceMotion.current
    val enterSpec: FiniteAnimationSpec<Float> = if (reduceMotion) snap() else tween(220)
    val exitSpec: FiniteAnimationSpec<Float> = if (reduceMotion) snap() else tween(180)
    NavHost(
        navController = navController,
        startDestination = TopDestination.Transactions.route,
        modifier = modifier,
        enterTransition = { fadeIn(enterSpec) },
        exitTransition = { fadeOut(exitSpec) },
        popEnterTransition = { fadeIn(enterSpec) },
        popExitTransition = { fadeOut(exitSpec) },
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
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            SubscriptionsScreen(
                openAddInitially = entry.arguments?.getBoolean("add") == true,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "investments?add={add}",
            arguments = listOf(
                navArgument("add") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            InvestmentsScreen(
                openAddInitially = entry.arguments?.getBoolean("add") == true,
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
        composable("assistant") {
            AssistantScreen(onBack = { navController.popBackStack() })
        }
        composable("calendar") {
            CalendarScreen(
                onEdit = { id -> navController.navigate("tx_form/$id") },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
