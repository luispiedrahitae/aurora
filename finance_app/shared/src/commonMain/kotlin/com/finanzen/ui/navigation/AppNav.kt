package com.finanzen.ui.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.SageMotion

// Ancho a partir del cual mostramos rail lateral en vez de barra inferior (escritorio/tablet).
private val WIDE_BREAKPOINT = 600.dp

// Pantalla donde el FAB de captura rápida está disponible.
private val FAB_ROUTES = setOf(TopDestination.Transactions.route)

/** Qué mostrar en la hoja de movimiento (nuevo o edición): vive sobre la pantalla actual para que
 *  Movimientos, Calendario y el FAB compartan el mismo estado. */
private sealed interface TxSheetRequest {
    data class New(val kind: String? = null) : TxSheetRequest
    data class Edit(val id: Long) : TxSheetRequest
}

/** Icono Lucide de cada pestaña (fuera del enum porque `LucideIcons` se resuelve en composición). */
@Composable
private fun topDestinationIcon(dest: TopDestination): Painter = when (dest) {
    TopDestination.Dashboard -> LucideIcons.LayoutDashboard
    TopDestination.Transactions -> LucideIcons.ArrowLeftRight
    TopDestination.Accounts -> LucideIcons.Wallet
    TopDestination.Budgets -> LucideIcons.PieChart
    TopDestination.More -> LucideIcons.MoreHorizontal
}

/**
 * Pestaña Sage: icono dentro de una píldora de 56×28 (fondo suave del acento si está elegida) y
 * etiqueta de 12 debajo. Toda la columna es el objetivo táctil (64 dp de alto).
 */
@Composable
private fun NavTab(dest: TopDestination, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 28.dp)
                .clip(MaterialTheme.shapes.small)
                .then(if (selected) Modifier.background(scheme.primaryContainer) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                topDestinationIcon(dest),
                contentDescription = null,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            dest.label,
            style = LocalSageType.current.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Barra inferior: superficie con borde superior fino y las cinco pestañas repartidas. */
@Composable
private fun SageBottomBar(currentRoute: String?, onSelect: (TopDestination) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                TopDestination.entries.forEach { dest ->
                    NavTab(dest, currentRoute == dest.route, { onSelect(dest) }, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Rail lateral para pantallas anchas: mismas pestañas, en columna, con borde a la derecha. */
@Composable
private fun SageRail(currentRoute: String?, onSelect: (TopDestination) -> Unit) {
    Row(Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier
                .width(88.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopDestination.entries.forEach { dest ->
                NavTab(dest, currentRoute == dest.route, { onSelect(dest) }, Modifier.fillMaxWidth())
            }
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showNav = TopDestination.entries.any { it.route == currentRoute }
    // El FAB de captura rápida vive solo en Movimientos, la pantalla dueña de esa acción.
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
    val content: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier.background(MaterialTheme.colorScheme.background)) {
            AppNavHost(navController, onEditTransaction = { id -> txSheetRequest = TxSheetRequest.Edit(id) })
            if (showFab) {
                AddFab(
                    label = "Nuevo movimiento",
                    onClick = { txSheetRequest = TxSheetRequest.New() },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (maxWidth >= WIDE_BREAKPOINT && showNav) {
            Row(Modifier.fillMaxSize()) {
                SageRail(currentRoute, onSelect)
                content(Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                content(Modifier.weight(1f).fillMaxWidth())
                if (showNav) {
                    SageBottomBar(currentRoute, onSelect)
                } else {
                    Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(0.dp))
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
    val enterSpec: FiniteAnimationSpec<Float> = if (reduceMotion) snap() else tween(SageMotion.MEDIUM)
    val exitSpec: FiniteAnimationSpec<Float> = if (reduceMotion) snap() else tween(SageMotion.SHORT)
    val back: () -> Unit = { navController.popBackStack() }
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
            DashboardScreen(onOpenSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) })
        }
        composable(TopDestination.Transactions.route) {
            TransactionsScreen(onEdit = onEditTransaction)
        }
        composable(TopDestination.Accounts.route) { AccountsTabScreen() }
        composable(TopDestination.Budgets.route) { BudgetsScreen(onOpenCategories = { navController.navigate(Routes.CATEGORIES) }) }
        composable(TopDestination.More.route) {
            MoreScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Routes.SUBSCRIPTIONS) { SubscriptionsScreen(onBack = back) }
        composable(Routes.INVESTMENTS) { InvestmentsScreen(onBack = back) }
        composable(Routes.REPORTS) { ReportsScreen(onBack = back) }
        composable(Routes.SECURITY) { SecuritySettingsScreen(onBack = back) }
        composable(Routes.BACKUP) { BackupScreen(onBack = back) }
        composable(Routes.APPEARANCE) { SettingsScreen(onBack = back) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = back) }
        composable(Routes.CURRENCY) { CurrencyScreen(onBack = back) }
        composable(Routes.ABOUT) { AboutScreen(onBack = back) }
        composable(Routes.SUGGESTIONS) { SuggestionsScreen(onBack = back) }
        composable(Routes.CATEGORIES) { CategoriesScreen(onBack = back) }
        composable(Routes.CALENDAR) { CalendarScreen(onEdit = onEditTransaction, onBack = back) }
    }
}
