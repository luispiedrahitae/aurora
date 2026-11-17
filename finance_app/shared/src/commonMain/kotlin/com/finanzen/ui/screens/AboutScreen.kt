package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.IconAvatar
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType

private const val APP_VERSION = "1.0.0"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Acerca de Sage", onBack = onBack)
        ScreenColumn {
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S", style = LocalSageType.current.wordmark, color = MaterialTheme.colorScheme.onPrimary)
                }
                Text("Sage", style = LocalSageType.current.wordmark, modifier = Modifier.padding(top = 12.dp))
                Text(
                    "Si sabes a dónde va, sabes lo que tienes.",
                    style = LocalSageType.current.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text("Versión $APP_VERSION", style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            InfoCard(
                icon = LucideIcons.Shield,
                title = "Privacidad",
                body = "Tus datos viven solo en este dispositivo. No hay servidores, cuentas ni telemetría.",
            )
            InfoCard(
                icon = LucideIcons.Lock,
                title = "Datos y permisos",
                body = "Sage pide huella o rostro para desbloquear la app y notificaciones para tus recordatorios. " +
                    "Nada más: no hay acceso a contactos, ubicación ni internet.",
            )
        }
    }
}

/** Tarjeta con avatar, título y texto. Compartida con [SuggestionsScreen]. */
@Composable
internal fun InfoCard(icon: Painter, title: String, body: String) {
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconAvatar(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = LocalSageType.current.titleCard)
                Text(body, style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
