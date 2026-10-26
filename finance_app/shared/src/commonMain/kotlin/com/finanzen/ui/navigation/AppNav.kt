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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finanzen.ui.components.AddFab
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.screens.AboutScreen
import com.finanzen.ui.screens.AccountsTabScreen
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
import com.finanzen.ui.screens.SuggestionsScreen
import com.finanzen.ui.screens.TransactionFormScreen
import com.finanzen.ui.screens.TransactionsScreen
import com.finanzen.ui.theme.LocalReduceMotion

// Ancho a partir del cual mostramos rail lateral en vez de bottom bar (escritorio/tablet).
private val WIDE_BREAKPOINT = 600.dp

// Pantalla donde el FAB de captura rápida está disponible.
private val FAB_ROUTES = setOf(TopDestination.Transactions.route)

/** Qué mostrar en el bottom sheet de movimiento (nuevo o edición). Reemplaza las antiguas rutas
 *  de navegación "tx_form..." — el sheet vive sobre la pantalla actual en vez de navegar a una
 *  pantalla completa, para que Movimientos/Calendario/el FAB compartan el mismo estado elevado
 *  a [AppNav] sin duplicar lógica de apertura. */
private sealed interface TxSheetRequest {
    data class New(val kind: String? = null) : TxSheetRequest
    data class Edit(val id: Long) : TxSheetRequest
}

/** Mapea cada destino a su ícono Lucide. Función aparte porque `LucideIcons` expone
 *  `Painter` vía `@Composable get()` y no puede vivir como valor de constructor del enum. */
@Composable
private fun topDestinationIcon(dest: TopDestination): Painter = when (dest) {
    TopDestination.Dashboard -> LucideIcons.LayoutDashboard
    TopDestination.Transactions -> LucideIcons.ArrowLeftRight
    TopDestination.Accounts -> LucideIcons.Wallet
    TopDestination.Budgets -> LucideIcons.PieChart
    TopDestination.More -> LucideIcons.MoreHorizontal
}

/** Icono de pestaña. Compartido por ambas variantes (bottom bar y rail), que son dos bloques
 * paralelos. */
@Composable
private fun TabIcon(dest: TopDestination) {
    val sizeModifier = if (dest == TopDestination.More) Modifier.size(20.dp) else Modifier
    Icon(
        painter = topDestinationIcon(dest),
        contentDescription = dest.label,
        modifier = sizeModifier,
    )
}

/**
 * Calcula el tamaño de fuente más grande al que las [labels] del nav caben completas (una sola
 * línea, sin recorte) en el ancho disponible por ítem. Mismo tamaño para todas -- a diferencia
 * de [com.finanzen.ui.components.AutoSizeText], que encoge cada texto por separado y por eso
 * dejaba "Cuentas" más grande que "Movimientos". El techo es siempre `style.fontSize` (el rol
 * `label` fijo de Type.kt no debe superarse); solo baja hacia [minFontSize] cuando la etiqueta
 * más ancha no entra al techo.
 */
@Composable
private fun rememberFittedNavLabelFontSize(
    labels: List<String>,
    availableWidthPerItem: Dp,
    style: TextStyle,
    minFontSize: TextUnit = 9.sp,
): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val maxFontSize = style.fontSize
    val availableWidthPx = with(density) { availableWidthPerItem.roundToPx() }
    return remember(labels, availableWidthPx, style, maxFontSize, minFontSize) {
        var size = maxFontSize
        while (size.value > minFontSize.value) {
            val widest = labels.maxOf { label ->
                measurer.measure(
                    text = AnnotatedString(label),
                    style = style.copy(fontSize = size),
                    maxLines = 1,
                    softWrap = false,
                ).size.width
            }
            if (widest <= availableWidthPx) break
            size = (size.value - 1f).sp
        }
        size
    }
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showNav = TopDestination.entries.any { it.route == currentRoute }
    // El FAB de captura rápida vive solo en Movimientos, la pantalla dueña de esa acción.
    // Resumen y Análisis son de solo lectura.
    val showFab = currentRoute in FAB_ROUTES
    var txSheetRequest by remember { mutableStateOf<TxSheetRequest?>(null) }

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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_BREAKPOINT && showNav) {
            Row(Modifier.fillMaxSize()) {
                // Riel flat (sin vidrio — DESIGN.md: "The Soft Shadow, No Glass Rule") con un
                // hairline en el borde que toca el contenido, no un rectángulo completo.
                val railHairline = MaterialTheme.colorScheme.outlineVariant
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            railHairline,
                            Offset(size.width, 0f),
                            Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
                ) {
                    TopDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onSelect(dest) },
                            icon = { TabIcon(dest) },
                            label = {
                                BoxWithConstraints(contentAlignment = Alignment.Center) {
                                    val railLabelFontSize = rememberFittedNavLabelFontSize(
                                        labels = remember { TopDestination.entries.map { it.label } },
                                        availableWidthPerItem = maxWidth,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = railLabelFontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            },
                            // primaryContainer/onPrimaryContainer siguen el acento elegido en
                            // Apariencia en los dos presets (Accent.kt); el default de M3
                            // (secondaryContainer) no — bajo Terracota queda fijo en verde salvia.
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxSize()) {
                    AppNavHost(navController, onEditTransaction = { id -> txSheetRequest = TxSheetRequest.Edit(id) })
                    if (showFab) {
                        AddFab(
                            label = "Movimiento",
                            onClick = { txSheetRequest = TxSheetRequest.New() },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        )
                    }
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    if (showNav) {
                        val barHairline = MaterialTheme.colorScheme.outlineVariant
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier.drawBehind {
                                drawLine(
                                    barHairline,
                                    Offset(0f, 0f),
                                    Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            },
                        ) {
                            TopDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { onSelect(dest) },
                                    icon = { TabIcon(dest) },
                                    label = {
                                        BoxWithConstraints(contentAlignment = Alignment.Center) {
                                            val barLabelFontSize = rememberFittedNavLabelFontSize(
                                                labels = remember { TopDestination.entries.map { it.label } },
                                                availableWidthPerItem = maxWidth,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                            )
                                            Text(
                                                text = dest.label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = barLabelFontSize,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    },
                                    // Ver comentario equivalente en NavigationRailItem arriba.
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { inner ->
                // Solo el inset inferior (nav bar). El superior lo aporta cada pantalla una sola vez:
                // las que tienen TopAppBar vía su barra; las "desnudas" vía windowInsetsPadding propio.
                Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
                    AppNavHost(navController, onEditTransaction = { id -> txSheetRequest = TxSheetRequest.Edit(id) })
                    if (showFab) {
                        AddFab(
                            label = "Movimiento",
                            onClick = { txSheetRequest = TxSheetRequest.New() },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    txSheetRequest?.let { req ->
        TransactionFormScreen(
            transactionId = (req as? TxSheetRequest.Edit)?.id,
            initialKind = (req as? TxSheetRequest.New)?.kind,
            onDismiss = { txSheetRequest = null },
        )
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    onEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            DashboardScreen(onOpenSubscriptions = { navController.navigate("subscriptions") })
        }
        composable(TopDestination.Transactions.route) {
            TransactionsScreen(onEdit = onEditTransaction)
        }
        composable(TopDestination.Accounts.route) { AccountsTabScreen() }
        composable(TopDestination.Budgets.route) { BudgetsScreen() }
        composable(TopDestination.More.route) {
            MoreScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("subscriptions") {
            SubscriptionsScreen(onBack = { navController.popBackStack() })
        }
        composable("investments") {
            InvestmentsScreen(onBack = { navController.popBackStack() })
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
        composable("suggestions") {
            SuggestionsScreen(onBack = { navController.popBackStack() })
        }
        composable("categories") {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable("calendar") {
            CalendarScreen(
                onEdit = onEditTransaction,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
