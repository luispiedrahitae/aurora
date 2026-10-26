package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.sageSwitchColors
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

/** Apartado de Notificaciones: avisos de tarjetas/presupuesto + antelación del recordatorio de suscripciones. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val cardNotif by vm.cardNotifications.collectAsState()
    val budgetNotif by vm.budgetNotifications.collectAsState()
    val remindDays by vm.remindDaysBefore.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Notificaciones", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Avisos")
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
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Suscripciones")
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
                                Icon(LucideIcons.Minus, contentDescription = "Menos días")
                            }
                            Text(
                                "$remindDays",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(
                                onClick = { vm.setRemindDaysBefore(remindDays + 1) },
                                enabled = remindDays < 30,
                            ) {
                                Icon(LucideIcons.Plus, contentDescription = "Más días")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // Toda la fila conmuta (Role.Switch); el Switch es solo visual para no duplicar el foco.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, colors = sageSwitchColors())
    }
}
