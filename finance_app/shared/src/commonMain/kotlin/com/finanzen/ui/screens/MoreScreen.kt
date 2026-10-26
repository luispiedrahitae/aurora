package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSpacing

data class MoreItem(val title: String, val icon: Painter, val onClick: () -> Unit)

@Composable
fun MoreScreen(onNavigate: (route: String) -> Unit) {
    val items = listOf(
        MoreItem("Suscripciones", LucideIcons.Repeat) {
            onNavigate("subscriptions")
        },
        MoreItem("Inversiones", LucideIcons.TrendingUp) {
            onNavigate("investments")
        },
        MoreItem("Calendario", LucideIcons.Calendar) {
            onNavigate("calendar")
        },
        MoreItem("Reportes", LucideIcons.FileText) {
            onNavigate("reports")
        },
        MoreItem("Backup", LucideIcons.Download) {
            onNavigate("backup")
        },
        MoreItem("Categorías", LucideIcons.Tag) {
            onNavigate("categories")
        },
        MoreItem("Notificaciones", LucideIcons.Bell) {
            onNavigate("notifications")
        },
        MoreItem("Seguridad", LucideIcons.Shield) {
            onNavigate("security")
        },
        MoreItem("Apariencia", LucideIcons.Palette) {
            onNavigate("settings")
        },
        MoreItem("Moneda", LucideIcons.Banknote) {
            onNavigate("currency")
        },
        MoreItem("Acerca de Sage", LucideIcons.Info) {
            onNavigate("about")
        },
        MoreItem("Sugerencias", LucideIcons.Info) {
            onNavigate("suggestions")
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
            }
            Icon(
                LucideIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
