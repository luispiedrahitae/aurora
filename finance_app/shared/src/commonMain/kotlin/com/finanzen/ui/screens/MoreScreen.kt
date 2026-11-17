package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.MainTabHeader
import com.finanzen.ui.components.MenuRow
import com.finanzen.ui.components.RowDivider
import com.finanzen.ui.components.ScreenList
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.navigation.Routes
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

private data class MoreItem(val title: String, val icon: Painter, val route: String, val value: String? = null)

@Composable
fun MoreScreen(onNavigate: (route: String) -> Unit, settingsVm: SettingsViewModel = koinInject()) {
    val themeMode by settingsVm.themeMode.collectAsState()
    val currency by settingsVm.baseCurrency.collectAsState()
    // Tres grupos: herramientas, ajustes e información se buscan por separado.
    val groups = listOf(
        "Planificación" to listOf(
            MoreItem("Suscripciones", LucideIcons.Repeat, Routes.SUBSCRIPTIONS),
            MoreItem("Fondos de inversión", LucideIcons.TrendingUp, Routes.INVESTMENTS),
            MoreItem("Calendario", LucideIcons.Calendar, Routes.CALENDAR),
            MoreItem("Reportes", LucideIcons.FileText, Routes.REPORTS),
        ),
        "Ajustes" to listOf(
            MoreItem("Categorías", LucideIcons.Tag, Routes.CATEGORIES),
            MoreItem("Apariencia", LucideIcons.Palette, Routes.APPEARANCE, themeMode.label),
            MoreItem("Moneda", LucideIcons.Banknote, Routes.CURRENCY, currency),
            MoreItem("Notificaciones", LucideIcons.Bell, Routes.NOTIFICATIONS),
            MoreItem("Seguridad", LucideIcons.Shield, Routes.SECURITY),
            MoreItem("Copia de seguridad", LucideIcons.Download, Routes.BACKUP),
        ),
        "Información" to listOf(
            MoreItem("Acerca de Sage", LucideIcons.Info, Routes.ABOUT),
            // Lápiz ("escríbenos"), no Info: compartía icono con "Acerca de Sage" y se confundían.
            MoreItem("Sugerencias", LucideIcons.Pencil, Routes.SUGGESTIONS),
        ),
    )

    Column(Modifier.fillMaxSize()) {
        MainTabHeader(title = "Más")
        ScreenList {
            groups.forEach { (title, items) ->
                item(key = title) {
                    TitledSection(title) {
                        GroupCard {
                            items.forEachIndexed { index, item ->
                                if (index > 0) RowDivider()
                                MenuRow(item.title, onClick = { onNavigate(item.route) }, icon = item.icon, value = item.value)
                            }
                        }
                    }
                }
            }
        }
    }
}
