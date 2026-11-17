package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType

@Composable
fun SuggestionsScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Sugerencias", onBack = onBack)
        ScreenColumn {
            FinanceCard(Modifier.fillMaxWidth(), size = BentoTileSize.Hero) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Algo que mejorar?", style = LocalSageType.current.titleSection)
                    Text(
                        "Sage la hace una sola persona. Cada idea, duda o error que reportes se lee.",
                        style = LocalSageType.current.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            InfoCard(
                icon = LucideIcons.Pencil,
                title = "Desarrollador",
                body = "Hecha por Luis Piedrahita. Escribe a $CONTACT_EMAIL.",
            )
            TitledSection("Para que sea útil") {
                listOf(
                    "Qué intentabas hacer y en qué pantalla.",
                    "Qué esperabas que pasara y qué pasó.",
                    "Si es un error, una captura ayuda mucho.",
                ).forEach { tip ->
                    Text("•  $tip", style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // El correo abre la app de correo con el asunto ya escrito.
            SageButton(
                text = "Escribir un correo",
                onClick = { runCatching { uriHandler.openUri("mailto:$CONTACT_EMAIL?subject=Sugerencia%20para%20Sage") } },
                modifier = Modifier.fillMaxWidth(),
                icon = rememberVectorPainter(Icons.Outlined.Email),
            )
            Text(
                "Se abre tu app de correo con el asunto ya escrito.",
                style = LocalSageType.current.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val CONTACT_EMAIL = "luispiedrahitae@gmail.com"
