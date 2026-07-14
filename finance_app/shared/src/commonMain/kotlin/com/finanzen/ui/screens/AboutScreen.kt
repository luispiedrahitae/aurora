package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.platform.platformName
import com.finanzen.ui.components.FinanceCard

private const val APP_VERSION = "1.0.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de FinanZen") },
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("FinanZen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Finanzas personales 100% locales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Versión $APP_VERSION · $platformName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                InfoCard(
                    title = "Privacidad",
                    body = "Tus datos viven solo en este dispositivo. No hay servidores, ni cuentas, ni telemetría, " +
                        "ni publicidad. La app no requiere conexión a internet para funcionar. El asistente IA " +
                        "opcional es la única función que usa internet, y solo para descargar el modelo una vez; " +
                        "después responde 100% en tu dispositivo. Los backups que exportes se cifran con AES-GCM " +
                        "usando una clave derivada de tu PIN: sin ese PIN, el archivo no se puede leer.",
                )
            }

            item {
                InfoCard(
                    title = "Datos y permisos",
                    body = "FinanZen no recopila información personal. Solo usa almacenamiento local para la base de " +
                        "datos y, cuando tú lo pidas, para guardar reportes o backups. No se comparte nada con terceros.",
                )
            }

            item {
                InfoCard(
                    title = "Licencias y tecnología",
                    body = "Construida con Kotlin Multiplatform y Compose Multiplatform. Persistencia con SQLDelight, " +
                        "inyección de dependencias con Koin. Software de código abierto bajo sus respectivas licencias.",
                )
            }

            item {
                InfoCard(
                    title = "Iconos",
                    body = "Iconos generales: Material Symbols (Apache 2.0). Logos de servicios: Simple Icons (CC0). " +
                        "Las marcas y logotipos pertenecen a sus respectivos dueños; se muestran solo para que " +
                        "identifiques tus categorías y no implican afiliación ni patrocinio.",
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
