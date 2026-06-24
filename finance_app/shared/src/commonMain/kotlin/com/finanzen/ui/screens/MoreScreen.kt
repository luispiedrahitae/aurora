package com.finanzen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MoreItem(val title: String, val subtitle: String, val icon: ImageVector, val enabled: Boolean, val onClick: () -> Unit)

@Composable
fun MoreScreen(onNavigate: (route: String) -> Unit) {
    val items = listOf(
        MoreItem("Suscripciones y recurrentes", "Recordatorios de cobros", Icons.Outlined.Repeat, true) {
            onNavigate("subscriptions")
        },
        MoreItem("Presupuestos", "Por categoría y mes", Icons.Outlined.PieChart, true) {
            onNavigate("budgets")
        },
        MoreItem("Reportes", "Exportar CSV / PDF", Icons.Outlined.Description, true) {
            onNavigate("reports")
        },
        MoreItem("Backup encriptado", "Exportar / importar tu data", Icons.Outlined.Backup, true) {
            onNavigate("backup")
        },
        MoreItem("Cuentas", "Crear cuentas y elegir moneda", Icons.Outlined.AccountBalance, true) {
            onNavigate("accounts")
        },
        MoreItem("Categorías", "Personalizar", Icons.Outlined.AutoAwesome, true) {
            onNavigate("categories")
        },
        MoreItem("Seguridad", "Bloqueo con PIN", Icons.Outlined.Settings, true) {
            onNavigate("security")
        },
        MoreItem("Apariencia", "Tema claro / oscuro / sistema", Icons.Outlined.DarkMode, true) {
            onNavigate("settings")
        },
        MoreItem("Acerca de FinanZen", "Versión, privacidad, licencias", Icons.Outlined.Info, true) {
            onNavigate("about")
        },
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.title }) { item -> MoreRow(item) }
    }
}

@Composable
private fun MoreRow(item: MoreItem) {
    val containerAlpha = if (item.enabled) 1.0f else 0.55f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled, onClick = item.onClick),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
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
