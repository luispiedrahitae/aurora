package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.finanzen.domain.Money
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.viewmodel.SubscriptionsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    vm: SubscriptionsViewModel = koinViewModel(),
) {
    val subs by vm.subscriptions.collectAsState()
    val recur by vm.recurring.collectAsState()
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()
    // Qué formulario está abierto: "sub", "recur" o null.
    var openForm by remember { mutableStateOf<String?>(null) }

    if (openForm != null) {
        val isRecurring = openForm == "recur"
        ScheduleFormDialog(
            title = if (isRecurring) "Nuevo gasto recurrente" else "Nueva suscripción",
            parseAmount = vm::parseAmountToMinor,
            onDismiss = { openForm = null },
            onConfirm = { name, amountMinor, inDays, remindDays ->
                val nextCharge = today + inDays
                if (isRecurring) {
                    vm.addRecurring(name, amountMinor, nextCharge, remindDays)
                } else {
                    vm.addSubscription(name, amountMinor, nextCharge, remindDays)
                }
                openForm = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suscripciones y recurrentes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { openForm = "sub" },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Suscripción") },
                )
                ExtendedFloatingActionButton(
                    onClick = { openForm = "recur" },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Recurrente") },
                )
            }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("Suscripciones (${subs.size})") }
            if (subs.isEmpty()) {
                item { EmptyText("Sin suscripciones. Usa + para añadir una.") }
            } else {
                items(subs, key = { "s-${it.id}" }) { s ->
                    ScheduleItem(
                        title = s.name,
                        amountMinor = s.amountMinor,
                        currency = s.currency,
                        nextChargeEpochDay = s.nextChargeDate,
                        today = today,
                        frequency = "${s.frequency} ×${s.intervalCount}",
                        remindDays = s.remindDaysBefore,
                        onDelete = { vm.deleteSubscription(s.id) },
                    )
                }
            }

            item { SectionHeader("Gastos recurrentes (${recur.size})") }
            if (recur.isEmpty()) {
                item { EmptyText("Sin recurrentes. Usa + para añadir un gasto fijo.") }
            } else {
                items(recur, key = { "r-${it.id}" }) { r ->
                    ScheduleItem(
                        title = r.name,
                        amountMinor = r.amountMinor,
                        currency = r.currency,
                        nextChargeEpochDay = r.nextChargeDate,
                        today = today,
                        frequency = "${r.frequency} ×${r.intervalCount}",
                        remindDays = r.remindDaysBefore,
                        onDelete = { vm.deleteRecurring(r.id) },
                    )
                }
            }
        }
    }
}

/**
 * Formulario de alta para suscripción o recurrente. `onConfirm(nombre, montoMinor, enDías, recordarDíasAntes)`.
 * ponytail: el próximo cobro se pide como "dentro de N días"; un calendario sería más natural si se pide.
 */
@Composable
private fun ScheduleFormDialog(
    title: String,
    parseAmount: (String) -> Long?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amountMinor: Long, inDays: Long, remindDays: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var inDays by remember { mutableStateOf("30") }
    var remindDays by remember { mutableStateOf("2") }

    val amountMinor = parseAmount(amount)
    val daysValid = inDays.toLongOrNull()?.let { it >= 0 } == true
    val remindValid = remindDays.toLongOrNull()?.let { it >= 0 } == true
    val valid = name.isNotBlank() && amountMinor != null && amountMinor > 0 && daysValid && remindValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                OutlinedTextField(
                    value = inDays,
                    onValueChange = { inDays = it.filter(Char::isDigit) },
                    label = { Text("Próximo cobro en (días)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = remindDays,
                    onValueChange = { remindDays = it.filter(Char::isDigit) },
                    label = { Text("Recordar (días antes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), amountMinor!!, inDays.toLong(), remindDays.toLong()) },
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

@Composable
private fun ScheduleItem(
    title: String,
    amountMinor: Long,
    currency: String,
    nextChargeEpochDay: Long,
    today: Long,
    frequency: String,
    remindDays: Long,
    onDelete: () -> Unit,
) {
    val daysUntil = nextChargeEpochDay - today
    val daysLabel = when {
        daysUntil < 0L -> "vencida hace ${-daysUntil}d"
        daysUntil == 0L -> "hoy"
        daysUntil == 1L -> "mañana"
        else -> "en ${daysUntil}d"
    }
    FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Box {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    "${Money(amountMinor, currency).format()} $currency · $frequency · próxima $daysLabel · recuerda ${remindDays}d antes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
