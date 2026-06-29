package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MoneyText
import com.finanzen.ui.components.SectionHeader
import com.finanzen.viewmodel.SubscriptionsViewModel
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

private val MESES_SUB = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

private fun fechaCorta(epochDay: Long): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    return "${d.dayOfMonth} ${MESES_SUB[d.monthNumber - 1]}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    openAddInitially: Boolean = false,
    vm: SubscriptionsViewModel = koinViewModel(),
) {
    val subs by vm.subscriptions.collectAsState()
    var showForm by remember { mutableStateOf(openAddInitially) }

    if (showForm) {
        SubscriptionFormDialog(
            parseAmount = vm::parseAmountToMinor,
            onDismiss = { showForm = false },
            onConfirm = { name, amountMinor, frequency, interval, remindDays ->
                vm.addSubscription(name, amountMinor, frequency, interval, remindDays)
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
                item { EmptyText("Sin suscripciones. Usa + para añadir una; el cobro se registrará en Movimientos.") }
            } else {
                items(subs, key = { it.id }) { s ->
                    SubscriptionItem(
                        name = s.name,
                        amountMinor = s.amountMinor,
                        currency = s.currency,
                        nextChargeEpochDay = s.nextChargeDate,
                        onDelete = { vm.deleteSubscription(s.id) },
                    )
                }
            }
        }
    }
}

/**
 * Alta de suscripción. La recurrencia se elige con un segmented control:
 * "Cada X días" (DAILY) o "Día del mes" (MONTHLY). `onConfirm(nombre, montoMinor, frequency, interval, recordarDíasAntes)`.
 */
@Composable
private fun SubscriptionFormDialog(
    parseAmount: (String) -> Long?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amountMinor: Long, frequency: String, interval: Long, remindDays: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf(true) } // true = Día del mes, false = Cada X días
    var dayOfMonth by remember { mutableStateOf("1") }
    var everyDays by remember { mutableStateOf("30") }
    var remindDays by remember { mutableStateOf("2") }

    val amountMinor = parseAmount(amount)
    val dayValid = dayOfMonth.toIntOrNull()?.let { it in 1..31 } == true
    val everyValid = everyDays.toLongOrNull()?.let { it >= 1 } == true
    val remindValid = remindDays.toLongOrNull()?.let { it >= 0 } == true
    val recurValid = if (monthly) dayValid else everyValid
    val valid = name.isNotBlank() && amountMinor != null && amountMinor > 0 && recurValid && remindValid

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
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto (ej. 12.99)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Recurrencia", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = monthly,
                        onClick = { monthly = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Día del mes") }
                    SegmentedButton(
                        selected = !monthly,
                        onClick = { monthly = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
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
                OutlinedTextField(
                    value = remindDays,
                    onValueChange = { remindDays = it.filter(Char::isDigit).take(3) },
                    label = { Text("Recordar (días antes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val frequency = if (monthly) "MONTHLY" else "DAILY"
                    val interval = if (monthly) dayOfMonth.toLong() else everyDays.toLong()
                    onConfirm(name.trim(), amountMinor!!, frequency, interval, remindDays.toLong())
                },
                enabled = valid,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun EmptyText(text: String) {
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Subscriptions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Próximo: ${fechaCorta(nextChargeEpochDay)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(
                amountMinor = amountMinor,
                currency = currency,
                style = MaterialTheme.typography.titleMedium,
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
