package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.finanzen.db.Subscription
import com.finanzen.ui.components.AddFab
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.BottomSnackbarHost
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FabClearance
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.IconAvatar
import com.finanzen.ui.components.ListRow
import com.finanzen.ui.components.LoadingState
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.Overline
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwipeToDeleteRow
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.format.diaSemana
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalMoneyFormat
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.SubscriptionsViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

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

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays() }
    val dateLocale = LocalDateLocale.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SubScreenHeader(title = "Suscripciones", onBack = onBack)
            val visible = subs.filter { it.id !in pendingDeleteIds }.sortedBy { it.nextChargeDate }
            ScreenList(bottomPadding = FabClearance) {
                if (!loaded) {
                    item { LoadingState() }
                } else if (visible.isEmpty()) {
                    item {
                        EmptyState(
                            icon = LucideIcons.Repeat,
                            title = "Sin suscripciones",
                            subtitle = "Añade una y cada cobro se registrará solo en Movimientos.",
                            actionLabel = "Añadir suscripción",
                            onAction = { creating = true },
                        )
                    }
                } else {
                    item {
                        // La cifra que responde "¿cuánto me cuestan al mes?".
                        val monthlyCost = remember(visible) { DashboardViewModel.computeSubscriptionMonthlyCost(visible) }
                        FinanceCard(Modifier.fillMaxWidth(), size = BentoTileSize.Hero) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Overline("Costo mensual")
                                MoneyText(
                                    amountMinor = -monthlyCost,
                                    currency = vm.baseCurrency,
                                    style = LocalSageType.current.amountHero,
                                    signed = true,
                                    colorOverride = LocalFinanceColors.current.expense,
                                )
                                Text(
                                    "${LocalMoneyFormat.current.format(monthlyCost * MONTHS_PER_YEAR, vm.baseCurrency, false)} al año en " +
                                        "${visible.size} ${if (visible.size == 1) "suscripción" else "suscripciones"}. Cada cobro se registra solo en Movimientos.",
                                    style = LocalSageType.current.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    val next = visible.first()
                    item {
                        TitledSection("Próximo cobro") {
                            GroupCard {
                                SubscriptionRow(
                                    sub = next,
                                    subtitle = listOfNotNull(
                                        "${diaSemana(next.nextChargeDate).replaceFirstChar { it.uppercase() }} ${formatDiaMes(next.nextChargeDate, dateLocale)}",
                                        daysUntilLabel(next.nextChargeDate - today),
                                        accountsById[next.accountId]?.name,
                                    ).joinToString(" · "),
                                    onClick = { editingId = next.id },
                                    onDelete = { requestDelete(next) },
                                )
                            }
                        }
                    }
                    if (visible.size > 1) {
                        item {
                            TitledSection("Las demás") {
                                GroupCard {
                                    visible.drop(1).forEachIndexed { index, s ->
                                        if (index > 0) RowDivider()
                                        SubscriptionRow(
                                            sub = s,
                                            subtitle = listOfNotNull(
                                                recurrenceText(s.frequency, s.intervalCount),
                                                "próximo ${formatDiaMes(s.nextChargeDate, dateLocale)}",
                                                accountsById[s.accountId]?.name,
                                            ).joinToString(" · "),
                                            onClick = { editingId = s.id },
                                            onDelete = { requestDelete(s) },
                                        )
                                    }
                                }
                                Text(
                                    "Toca una suscripción para cambiar el monto o el día de cobro; desliza a la izquierda para cancelarla.",
                                    style = LocalSageType.current.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        AddFab(
            label = "Nueva suscripción",
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
        BottomSnackbarHost(snackbarHostState, aboveFab = true)
    }
}

private const val MONTHS_PER_YEAR = 12

private fun daysUntilLabel(days: Long): String = when {
    days < 0 -> "pendiente"
    days == 0L -> "hoy"
    days == 1L -> "mañana"
    else -> "en $days días"
}

/** Texto de recurrencia corto para la fila de suscripción. Nunca muestra el código crudo de la BD. */
private fun recurrenceText(frequency: String, intervalCount: Long): String = when (frequency) {
    "MONTHLY" -> "Día $intervalCount de cada mes"
    "DAILY" -> if (intervalCount == 1L) "Cada día" else "Cada $intervalCount días"
    "WEEKLY" -> "Semanal"
    "YEARLY" -> "Anual"
    else -> "Periódica"
}

/** Fila de suscripción: avatar, nombre, detalle y monto. Tocar edita; deslizar a la izquierda cancela. */
@Composable
private fun SubscriptionRow(sub: Subscription, subtitle: String, onClick: () -> Unit, onDelete: () -> Unit) {
    SwipeToDeleteRow(onSwipeToDelete = {
        onDelete()
        true
    }) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            ListRow(
                title = sub.name,
                subtitle = subtitle,
                leading = { IconAvatar(LucideIcons.Repeat) },
                onClick = onClick,
                trailing = {
                    MoneyText(
                        amountMinor = -sub.amountMinor,
                        currency = sub.currency,
                        style = LocalSageType.current.amountMd,
                        signed = true,
                        colorOverride = LocalFinanceColors.current.expense,
                    )
                },
            )
        }
    }
}
