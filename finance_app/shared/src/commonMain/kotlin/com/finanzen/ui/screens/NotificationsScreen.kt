package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

/** Apartado de Notificaciones: avisos de tarjetas/presupuesto + antelación del recordatorio de suscripciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val cardNotif by vm.cardNotifications.collectAsState()
    val budgetNotif by vm.budgetNotifications.collectAsState()
    val remindDays by vm.remindDaysBefore.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader("Avisos") }
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Column {
                        ToggleRow(
                            "Corte y pago de tarjetas",
                            "Recuérdame el día de corte y de pago de mis tarjetas de crédito.",
                            cardNotif,
                            vm::setCardNotifications,
                        )
                        ToggleRow(
                            "Presupuesto alcanzado",
                            "Avísame cuando un gasto alcance el límite de una categoría.",
                            budgetNotif,
                            vm::setBudgetNotifications,
                        )
                    }
                }
            }

            item { SectionHeader("Suscripciones") }
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordar antes del cobro", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (remindDays == 0L) {
                                    "El mismo día del cobro."
                                } else {
                                    "$remindDays día(s) antes de cada cobro."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { vm.setRemindDaysBefore(remindDays - 1) },
                            enabled = remindDays > 0,
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Menos días")
                        }
                        Text(
                            "$remindDays",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(
                            onClick = { vm.setRemindDaysBefore(remindDays + 1) },
                            enabled = remindDays < 30,
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Más días")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
