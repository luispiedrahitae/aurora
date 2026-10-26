package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.displayFontFamily

private const val APP_VERSION = "1.0.0"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Acerca de Sage", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sage", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = displayFontFamily()))
                    Text(
                        "Sabiduría financiera, sin ruido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Versión $APP_VERSION",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                InfoCard(
                    icon = LucideIcons.Shield,
                    title = "Privacidad",
                    body = "Tus datos viven solo en este dispositivo. No hay servidores, cuentas ni telemetría.",
                )
            }

            item {
                InfoCard(
                    icon = LucideIcons.Lock,
                    title = "Datos y permisos",
                    body = "Sage pide huella/rostro para desbloquear la app y notificaciones para tus recordatorios. " +
                        "Nada más: no hay acceso a contactos, ubicación ni internet.",
                )
            }

            item {
                InfoCard(
                    icon = LucideIcons.FileText,
                    title = "Licencias y tecnología",
                    body = "Construida con Kotlin Multiplatform y Compose Multiplatform. Software de código abierto " +
                        "bajo sus respectivas licencias.",
                )
            }

            item {
                InfoCard(
                    icon = LucideIcons.Palette,
                    title = "Iconos",
                    body = "Iconos de interfaz: Lucide (ISC). Las marcas y logotipos pertenecen a sus respectivos dueños.",
                )
            }
        }
    }
}

@Composable
private fun InfoCard(icon: Painter, title: String, body: String) {
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
