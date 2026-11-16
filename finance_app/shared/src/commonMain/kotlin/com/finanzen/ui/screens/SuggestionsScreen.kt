package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.icons.LucideIcons

@Composable
fun SuggestionsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Sugerencias", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                InfoCard(
                    icon = LucideIcons.Pencil,
                    title = "Desarrollador",
                    body = "Hecha por Luis Piedrahita. ¿Dudas o sugerencias? Escribe a $CONTACT_EMAIL.",
                )
            }
            // El correo era texto plano: ahora abre la app de correo directamente.
            item {
                val uriHandler = LocalUriHandler.current
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri("mailto:$CONTACT_EMAIL?subject=Sugerencia%20para%20Sage") } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Escribir un correo") }
            }
        }
    }
}

private const val CONTACT_EMAIL = "luispiedrahitae@gmail.com"
