package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MainTabHeader

data class MoreItem(val title: String, val subtitle: String, val icon: ImageVector, val enabled: Boolean, val onClick: () -> Unit)

@Composable
fun MoreScreen(onNavigate: (route: String) -> Unit) {
    val items = listOf(
        MoreItem("Suscripciones", "Cobros recurrentes en Movimientos", Icons.Outlined.Repeat, true) {
            onNavigate("subscriptions")
        },
        MoreItem("Calendario", "Movimientos por día", Icons.Outlined.CalendarMonth, true) {
            onNavigate("calendar")
        },
        MoreItem("Análisis", "Gastos por categoría", Icons.Outlined.Analytics, true) {
            onNavigate("analysis")
        },
        MoreItem("Reportes", "Exportar CSV / PDF", Icons.Outlined.Description, true) {
            onNavigate("reports")
        },
        MoreItem("Backup", "Exportar / importar tu data", Icons.Outlined.Backup, true) {
            onNavigate("backup")
        },
        MoreItem("Categorías", "Personalizar", Icons.Outlined.AutoAwesome, true) {
            onNavigate("categories")
        },
        MoreItem("Notificaciones", "Recordatorios y avisos", Icons.Outlined.Notifications, true) {
            onNavigate("notifications")
        },
        MoreItem("Seguridad", "Bloqueo con PIN", Icons.Outlined.Settings, true) {
            onNavigate("security")
        },
        MoreItem("Apariencia", "Tema, color de acento y color dinámico", Icons.Outlined.DarkMode, true) {
            onNavigate("settings")
        },
        MoreItem("Moneda", "Moneda única de la app", Icons.Outlined.Payments, true) {
            onNavigate("currency")
        },
        MoreItem("Acerca de FinanZen", "Versión, privacidad, licencias", Icons.Outlined.Info, true) {
            onNavigate("about")
        },
    )

    Column(Modifier.fillMaxSize()) {
        MainTabHeader(title = "Más")
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.title }) { item -> MoreRow(item) }
        }
    }
}

@Composable
private fun MoreRow(item: MoreItem) {
    val containerAlpha = if (item.enabled) 1.0f else 0.55f
    FinanceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = item.onClick.takeIf { item.enabled },
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = containerAlpha),
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = containerAlpha),
                )
                Text(
                    if (item.enabled) item.subtitle else "${item.subtitle} (pendiente)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.enabled) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
