package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                    body = "Hecha por Luis Piedrahita. ¿Dudas o sugerencias? Escribe a luispiedrahitae@gmail.com.",
                )
            }
        }
    }
}
