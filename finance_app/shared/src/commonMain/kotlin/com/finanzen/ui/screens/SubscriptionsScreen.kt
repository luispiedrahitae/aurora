package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzen.db.Account
import com.finanzen.db.Subscription
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.DateField
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IconAvatar
import com.finanzen.ui.components.MoneyField
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.PickerField
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.flatFabElevation
import com.finanzen.ui.format.formatDiaMes
import com.finanzen.ui.theme.LocalDateLocale
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.viewmodel.DashboardViewModel
import com.finanzen.viewmodel.SubscriptionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    openAddInitially: Boolean = false,
    vm: SubscriptionsViewModel = koinViewModel(),
) {
    val subs by vm.subscriptions.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var showForm by remember { mutableStateOf(openAddInitially) }
    var pendingDelete by remember { mutableStateOf<Subscription?>(null) }

    pendingDelete?.let { sub ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar \"${sub.name}\"?") },
            text = { Text("Se dejarán de generar cobros. Los movimientos ya registrados se conservan.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSubscription(sub.id)
                    pendingDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }

    if (showForm) {
        SubscriptionFormDialog(
            accounts = accounts,
            currencyCode = vm.baseCurrency,
            onDismiss = { showForm = false },
            onConfirm = { name, amountMinor, frequency, interval, accountId, startEpochDay ->
                vm.addSubscription(name, amountMinor, frequency, interval, accountId, startEpochDay)
                showForm = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suscripciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Suscripción") },
                elevation = flatFabElevation(),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("Activas (${subs.size})") }
            if (subs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Outlined.Subscriptions,
                            title = "Sin suscripciones",
                            subtitle = "Usa + para añadir una; el cobro se registrará en Movimientos.",
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                items(subs, key = { it.id }) { s ->
                    SubscriptionItem(
                        name = s.name,
                        amountMinor = s.amountMinor,
                        currency = s.currency,
                        nextChargeEpochDay = s.nextChargeDate,
                        onDelete = { pendingDelete = s },
                    )
                }
            }
        }
    }
}

/**
 * Alta de suscripción. La recurrencia se elige con un segmented control:
 * "Cada X días" (DAILY) o "Día del mes" (MONTHLY). `onConfirm(nombre, montoMinor, frequency,
 * interval, accountId, startEpochDay)`. La fecha de inicio (por defecto hoy) puede ser distinta de
 * hoy: pasada (pone al día los cobros atrasados) o futura (no cobra nada hasta que corresponda).
 * La cuenta de pago se puede elegir libremente, incluida una de crédito — deliberadamente no se
 * agrega ningún campo de "Cuotas" aquí, a diferencia de un gasto normal con tarjeta de crédito: una
 * suscripción es un cargo recurrente, no una compra a plazos. Los días de antelación del
 * recordatorio son una preferencia global (Más › Notificaciones).
 */
@Composable
private fun SubscriptionFormDialog(
    accounts: List<Account>,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amountMinor: Long, frequency: String, interval: Long, accountId: Long?, startEpochDay: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amountMinor by remember { mutableStateOf(0L) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var startEpochDay by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()) }
    var monthly by remember { mutableStateOf(true) } // true = Día del mes, false = Cada X días
    var dayOfMonth by remember { mutableStateOf("1") }
    var everyDays by remember { mutableStateOf("30") }

    val dayValid = dayOfMonth.toIntOrNull()?.let { it in 1..31 } == true
    val everyValid = everyDays.toLongOrNull()?.let { it >= 1 } == true
    val recurValid = if (monthly) dayValid else everyValid
    val valid = name.isNotBlank() && amountMinor > 0 && recurValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva suscripción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyField(
                    amountMinor = amountMinor,
                    onAmountChange = { amountMinor = it },
                    currencyCode = currencyCode,
                    label = "Monto",
                    modifier = Modifier.fillMaxWidth(),
                )
                PickerField(
                    label = "Cuenta",
                    options = accounts,
                    selected = selectedAccount,
                    optionLabel = { "${it.name} (${it.currency})" },
                    onSelect = { selectedAccount = it },
                    emptyHint = "Crea una cuenta primero (pestaña Cuentas).",
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(
                    epochDay = startEpochDay,
                    onEpochDayChange = { startEpochDay = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Recurrencia", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = monthly,
                        onClick = { monthly = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {},
                    ) { Text("Día del mes") }
                    SegmentedButton(
                        selected = !monthly,
                        onClick = { monthly = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {},
                    ) { Text("Cada X días") }
                }
                if (monthly) {
                    OutlinedTextField(
                        value = dayOfMonth,
                        onValueChange = { dayOfMonth = it.filter(Char::isDigit).take(2) },
                        label = { Text("Día del mes (1–31)") },
                        isError = dayOfMonth.isNotEmpty() && !dayValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = everyDays,
                        onValueChange = { everyDays = it.filter(Char::isDigit).take(4) },
                        label = { Text("Cada cuántos días") },
                        isError = everyDays.isNotEmpty() && !everyValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val frequency = if (monthly) "MONTHLY" else "DAILY"
                    val interval = if (monthly) dayOfMonth.toLong() else everyDays.toLong()
                    onConfirm(name.trim(), amountMinor, frequency, interval, selectedAccount?.id, startEpochDay)
                },
                enabled = valid,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Tarjeta de suscripción: icono · nombre + próxima fecha · monto (héroe) · borrar discreto. */
@Composable
private fun SubscriptionItem(
    name: String,
    amountMinor: Long,
    currency: String,
    nextChargeEpochDay: Long,
    onDelete: () -> Unit,
) {
    val dateLocale = LocalDateLocale.current
    val finance = LocalFinanceColors.current
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAvatar(Icons.Outlined.Subscriptions)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Próximo: ${formatDiaMes(nextChargeEpochDay, dateLocale)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(
                amountMinor = -amountMinor,
                currency = currency,
                style = MaterialTheme.typography.titleMedium,
                signed = true,
                colorOverride = finance.expense,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
