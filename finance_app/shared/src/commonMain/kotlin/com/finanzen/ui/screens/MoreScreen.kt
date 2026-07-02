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
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.theme.LocalSpacing

data class MoreItem(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun MoreScreen(onNavigate: (route: String) -> Unit) {
    val items = listOf(
        MoreItem("Suscripciones", "Cobros recurrentes en Movimientos", Icons.Outlined.Repeat) {
            onNavigate("subscriptions")
        },
        MoreItem("Calendario", "Movimientos por día", Icons.Outlined.CalendarMonth) {
            onNavigate("calendar")
        },
        MoreItem("Análisis", "Gastos por categoría", Icons.Outlined.Analytics) {
            onNavigate("analysis")
        },
        MoreItem("Reportes", "Exportar CSV / PDF", Icons.Outlined.Description) {
            onNavigate("reports")
        },
        MoreItem("Backup", "Exportar / importar tu data", Icons.Outlined.Backup) {
            onNavigate("backup")
        },
        MoreItem("Categorías", "Personalizar", Icons.Outlined.AutoAwesome) {
            onNavigate("categories")
        },
        MoreItem("Notificaciones", "Recordatorios y avisos", Icons.Outlined.Notifications) {
            onNavigate("notifications")
        },
        MoreItem("Seguridad", "Bloqueo con PIN", Icons.Outlined.Settings) {
            onNavigate("security")
        },
        MoreItem("Apariencia", "Tema, color de acento y color dinámico", Icons.Outlined.DarkMode) {
            onNavigate("settings")
        },
        MoreItem("Moneda", "Moneda única de la app", Icons.Outlined.Payments) {
            onNavigate("currency")
        },
        MoreItem("Acerca de FinanZen", "Versión, privacidad, licencias", Icons.Outlined.Info) {
            onNavigate("about")
        },
    )

    val spacing = LocalSpacing.current
    Column(Modifier.fillMaxSize()) {
        MainTabHeader(title = "Más")
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(items, key = { it.title }) { item -> MoreRow(item) }
        }
    }
}

@Composable
private fun MoreRow(item: MoreItem) {
    FinanceCard(modifier = Modifier.fillMaxWidth(), onClick = item.onClick) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = LocalSpacing.current.md).weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
