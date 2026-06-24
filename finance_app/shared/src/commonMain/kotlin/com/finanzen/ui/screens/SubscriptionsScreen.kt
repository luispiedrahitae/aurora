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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                    onClick = { vm.addSampleSubscription() },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Suscripción") },
                )
                ExtendedFloatingActionButton(
                    onClick = { vm.addSampleRecurring() },
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
                item { EmptyText("Sin suscripciones. Usa + para añadir una de muestra.") }
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
