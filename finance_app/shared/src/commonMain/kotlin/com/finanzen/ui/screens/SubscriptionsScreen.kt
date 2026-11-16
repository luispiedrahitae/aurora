package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.db.Subscription
import com.finanzen.ui.components.AddFab
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.BottomSnackbarHost
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IconAvatar
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwipeToDeleteRow
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.SubscriptionsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    vm: SubscriptionsViewModel = koinViewModel(),
) {
    val subs by vm.subscriptions.collectAsState()
    val loaded by vm.loaded.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val accounts by vm.accounts.collectAsState()
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    var pendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun requestDelete(sub: Subscription) {
        pendingDeleteIds = pendingDeleteIds + sub.id
        // rememberCoroutineScope + finally, no vm.viewModelScope: sin SnackbarHost en pantalla,
        // showSnackbar nunca retorna y el borrado quedaba colgado. Salir cancela la corrutina y el
        // finally confirma el borrado salvo que el usuario haya tocado "Deshacer".
        scope.launch {
            var undone = false
            try {
                undone = snackbarHostState.showSnackbar(
                    message = "Suscripción eliminada",
                    actionLabel = "Deshacer",
                    duration = SnackbarDuration.Short,
                ) == SnackbarResult.ActionPerformed
            } finally {
                if (!undone) vm.deleteSubscription(sub.id)
                pendingDeleteIds = pendingDeleteIds - sub.id
            }
        }
    }

    editingId?.let { id ->
        TransactionFormScreen(transactionId = null, subscriptionId = id, onDismiss = { editingId = null })
    }
    if (creating) {
        TransactionFormScreen(transactionId = null, initialKind = "SUBSCRIPTION", onDismiss = { creating = false })
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // Alta directa aquí, como "+ Presupuesto": el estado vacío ya no manda a otra pantalla.
        floatingActionButton = { AddFab(label = "Suscripción", onClick = { creating = true }) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SubScreenHeader(title = "Suscripciones", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Abajo 96dp para que el FAB no tape la última tarjeta (igual que Movimientos).
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!loaded) {
                    // Sin contenido hasta la primera lectura: evita el "Sin suscripciones" fugaz.
                } else if (subs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = LucideIcons.Repeat,
                                title = "Sin suscripciones",
                                subtitle = "Usa el botón + para añadir una; cada cobro se registrará en Movimientos.",
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    }
                } else {
                    item {
                        // Total mensual comprometido: la cifra que responde "¿cuánto me cuestan al mes?".
                        val monthlyCost = remember(subs) { DashboardViewModel.computeSubscriptionMonthlyCost(subs) }
                        // Mismo padding (16dp) que SubscriptionItem: el texto arranca alineado con sus iconos.
                        FinanceCard(modifier = Modifier.fillMaxWidth(), size = BentoTileSize.Small, contentPadding = PaddingValues(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Costo mensual", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                MoneyText(
                                    amountMinor = monthlyCost,
                                    currency = vm.baseCurrency,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    colorOverride = LocalFinanceColors.current.expense,
                                )
                            }
                        }
                    }
                    items(subs.filter { it.id !in pendingDeleteIds }, key = { it.id }) { s ->
                        SwipeToDeleteRow(onSwipeToDelete = {
                            requestDelete(s)
                            true
                        }) {
                            SubscriptionItem(
                                name = s.name,
                                amountMinor = s.amountMinor,
                                currency = s.currency,
                                accountName = accountsById[s.accountId]?.name,
                                recurrenceText = recurrenceText(s.frequency, s.intervalCount),
                                onClick = { editingId = s.id },
                            )
                        }
                    }
                }
            }
        }
        BottomSnackbarHost(snackbarHostState)
    }
}

/** Texto de recurrencia corto para la fila de suscripción. Nunca muestra el código crudo de la BD. */
private fun recurrenceText(frequency: String, intervalCount: Long): String = when (frequency) {
    "MONTHLY" -> "Día $intervalCount"
    "DAILY" -> if (intervalCount == 1L) "Cada día" else "Cada $intervalCount días"
    "WEEKLY" -> "Semanal"
    "YEARLY" -> "Anual"
    else -> "Periódica"
}

/** Tarjeta de suscripción: icono · nombre + cuenta · recurrencia · monto. Tocar edita, deslizar elimina. */
@Composable
private fun SubscriptionItem(
    name: String,
    amountMinor: Long,
    currency: String,
    accountName: String?,
    recurrenceText: String,
    onClick: () -> Unit,
) {
    val finance = LocalFinanceColors.current
    FinanceCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAvatar(LucideIcons.Repeat)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(accountName, recurrenceText).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(
                amountMinor = -amountMinor,
                currency = currency,
                style = MaterialTheme.typography.bodyLarge,
                signed = true,
                colorOverride = finance.expense,
            )
        }
    }
}
