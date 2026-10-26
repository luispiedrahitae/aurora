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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.finanzen.db.Subscription
import com.finanzen.ui.components.BentoTileSize
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
    val accounts by vm.accounts.collectAsState()
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    var pendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun requestDelete(sub: Subscription) {
        pendingDeleteIds = pendingDeleteIds + sub.id
        // vm.viewModelScope, no rememberCoroutineScope(): el deshacer/borrado debe sobrevivir a la
        // navegación fuera de esta pantalla, no cancelarse con la composición.
        vm.viewModelScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Suscripción eliminada",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                pendingDeleteIds = pendingDeleteIds - sub.id
            } else {
                vm.deleteSubscription(sub.id)
                pendingDeleteIds = pendingDeleteIds - sub.id
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SubScreenHeader(title = "Suscripciones", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (subs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = LucideIcons.Repeat,
                                title = "Sin suscripciones",
                                subtitle = "Créala desde Nuevo movimiento; el cobro se registrará en Movimientos.",
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    }
                } else {
                    item {
                        // Total mensual comprometido: la cifra que responde "¿cuánto me cuestan al mes?".
                        val monthlyCost = remember(subs) { DashboardViewModel.computeSubscriptionMonthlyCost(subs) }
                        FinanceCard(modifier = Modifier.fillMaxWidth(), size = BentoTileSize.Small) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Costo mensual equivalente", style = MaterialTheme.typography.labelLarge)
                                MoneyText(
                                    amountMinor = monthlyCost,
                                    currency = vm.baseCurrency,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
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
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Texto de recurrencia corto para la fila de suscripción: "Día N" (mensual) o "Cada N días". */
private fun recurrenceText(frequency: String, intervalCount: Long): String = when (frequency) {
    "MONTHLY" -> "Día $intervalCount"
    "DAILY" -> "Cada $intervalCount días"
    else -> frequency
}

/** Tarjeta de suscripción: icono · nombre + cuenta · recurrencia · monto. Se elimina deslizando. */
@Composable
private fun SubscriptionItem(
    name: String,
    amountMinor: Long,
    currency: String,
    accountName: String?,
    recurrenceText: String,
) {
    val finance = LocalFinanceColors.current
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAvatar(Icons.Outlined.Subscriptions)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(accountName, recurrenceText).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
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
