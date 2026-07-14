package com.finanzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.BentoTileSize
import com.finanzen.ui.components.EmptyState
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.theme.LocalAccentColor
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.glassSurface
import com.finanzen.viewmodel.AssistantUiState
import com.finanzen.viewmodel.AssistantViewModel
import com.finanzen.viewmodel.ChatMessage
import com.finanzen.viewmodel.ModelState
import com.finanzen.viewmodel.Role
import org.koin.compose.viewmodel.koinViewModel

/**
 * Chat con el asistente IA on-device. La única superficie con `glassSurface` en esta pantalla es
 * la barra de entrada de texto (regla de "un solo tile de vidrio por pantalla" — el resto de la
 * pantalla, incluidas las burbujas de mensaje, se queda plano/tonal).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(onBack: () -> Unit, vm: AssistantViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.onScreenEntered() }
    DisposableEffect(Unit) { onDispose { vm.onScreenLeft() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asistente IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (state.modelState) {
                ModelState.CHECKING -> CenteredProgress("Verificando dispositivo…")
                ModelState.UNSUPPORTED_DEVICE -> CenteredEmpty(
                    icon = Icons.Outlined.SmartToy,
                    title = "No disponible en este dispositivo",
                    subtitle = "El asistente IA necesita más RAM y almacenamiento libre de los que tiene este equipo. El resto de FinanZen sigue funcionando igual.",
                )
                ModelState.NOT_DOWNLOADED -> DownloadPrompt(vm)
                ModelState.DOWNLOADING -> DownloadProgress(state)
                ModelState.LOADING -> CenteredProgress("Cargando modelo…")
                ModelState.CORRUPT, ModelState.ERROR -> CenteredEmpty(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Algo salió mal",
                    subtitle = state.error ?: "No se pudo preparar el asistente. Intenta importar el modelo de nuevo.",
                )
                ModelState.READY -> ChatBody(state = state, onSend = vm::ask)
            }
        }
    }
}

@Composable
private fun CenteredProgress(label: String) {
    val spacing = LocalSpacing.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(label, modifier = Modifier.padding(top = spacing.md), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CenteredEmpty(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    val spacing = LocalSpacing.current
    Box(Modifier.fillMaxSize().padding(spacing.xl), contentAlignment = Alignment.Center) {
        EmptyState(icon = icon, title = title, subtitle = subtitle)
    }
}

@Composable
private fun DownloadPrompt(vm: AssistantViewModel) {
    val spacing = LocalSpacing.current
    Column(
        Modifier.fillMaxSize().padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.SmartToy,
            title = "Pregúntale a tu asistente",
            subtitle = "Responde solo con tus datos financieros, 100% en tu dispositivo. Necesitas descargar una vez el modelo Gemma 4 E4B (~3.66 GB) — se hace en segundo plano, por Wi-Fi, sin salir de la app.",
        )
        FinanceCard(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.lg),
            onClick = vm::startDownload,
            size = BentoTileSize.Small,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Descargar modelo (~3.66 GB)",
                    modifier = Modifier.padding(start = spacing.sm),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DownloadProgress(state: AssistantUiState) {
    val spacing = LocalSpacing.current
    val progress = if (state.totalBytes > 0) (state.downloadedBytes.toFloat() / state.totalBytes) else 0f
    Column(
        Modifier.fillMaxSize().padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Descargando modelo del asistente…", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = spacing.lg),
        )
        Text(
            if (state.totalBytes > 0) "${formatGb(state.downloadedBytes)} de ${formatGb(state.totalBytes)}" else "Iniciando…",
            modifier = Modifier.padding(top = spacing.sm),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Podés seguir usando el resto de FinanZen mientras tanto.",
            modifier = Modifier.padding(top = spacing.md),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatGb(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    val whole = gb.toInt()
    val frac = ((gb - whole) * 10).toInt()
    return "$whole.$frac GB"
}

@Composable
private fun ChatBody(state: AssistantUiState, onSend: (String) -> Unit) {
    val spacing = LocalSpacing.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        if (state.messages.isEmpty()) {
            CenteredEmpty(
                icon = Icons.Outlined.SmartToy,
                title = "Pregúntale a tu asistente",
                subtitle = "Responde solo con tus datos financieros, 100% en tu dispositivo.",
            )
            Box(Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                itemsIndexed(state.messages, key = { _, m -> m.id }) { _, message -> MessageBubble(message) }
            }
        }
        if (state.error != null) {
            ErrorBanner(state.error)
        }
        Composer(enabled = !state.isGenerating, onSend = onSend)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val spacing = LocalSpacing.current
    val isUser = message.role == Role.USER
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Surface(shape = CircleShape, color = LocalAccentColor.current, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        FinanceCard(
            modifier = Modifier.padding(horizontal = spacing.sm).fillMaxWidth(0.8f),
            size = BentoTileSize.Small,
        ) {
            Text(
                message.text.ifBlank { "…" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = finance.warning)
        Text(message, modifier = Modifier.padding(start = spacing.sm), color = finance.warning, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Composer(enabled: Boolean, onSend: (String) -> Unit) {
    val spacing = LocalSpacing.current
    var input by remember { mutableStateOf("") }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(spacing.lg)
            .glassSurface()
            .padding(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Pregunta sobre tus finanzas…") },
            enabled = enabled,
            singleLine = true,
        )
        IconButton(
            onClick = {
                if (input.isNotBlank()) {
                    onSend(input)
                    input = ""
                }
            },
            enabled = enabled && input.isNotBlank(),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar")
        }
    }
}
