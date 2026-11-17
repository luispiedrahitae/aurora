package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.InlineNotice
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwitchRow
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

private const val MAX_REMIND_DAYS = 30L

/** Avisos de tarjetas y presupuesto, y cuántos días antes recordar cada cobro de suscripción. */
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
        ScreenColumn {
            TitledSection("Avisos") {
                GroupCard {
                    SwitchRow(
                        title = "Corte y pago de tarjetas",
                        subtitle = "Recuérdame el día de corte y de pago de mis tarjetas de crédito.",
                        checked = cardNotif,
                        onCheckedChange = vm::setCardNotifications,
                    )
                    RowDivider()
                    SwitchRow(
                        title = "Presupuesto alcanzado",
                        subtitle = "Avísame cuando un gasto alcance el límite de una categoría.",
                        checked = budgetNotif,
                        onCheckedChange = vm::setBudgetNotifications,
                    )
                }
            }
            TitledSection("Suscripciones") {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Recordar antes del cobro", style = LocalSageType.current.titleCard)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedIconButton(
                                onClick = { vm.setRemindDaysBefore(remindDays - 1) },
                                enabled = remindDays > 0,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(48.dp),
                            ) { Icon(LucideIcons.Minus, contentDescription = "Menos días") }
                            Text(
                                remindDaysLabel(remindDays),
                                style = LocalSageType.current.amountMd,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = 96.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            )
                            OutlinedIconButton(
                                onClick = { vm.setRemindDaysBefore(remindDays + 1) },
                                enabled = remindDays < MAX_REMIND_DAYS,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(48.dp),
                            ) { Icon(LucideIcons.Plus, contentDescription = "Más días") }
                        }
                        Text(
                            if (remindDays == 0L) "El aviso llega el mismo día del cobro." else "El aviso llega ${remindDaysLabel(remindDays)} antes de cada cobro.",
                            style = LocalSageType.current.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            InlineNotice("Si el teléfono tiene las notificaciones de Sage apagadas, actívalas en los ajustes del sistema.")
        }
    }
}

private fun remindDaysLabel(days: Long): String = when (days) {
    0L -> "El mismo día"
    1L -> "1 día"
    else -> "$days días"
}
