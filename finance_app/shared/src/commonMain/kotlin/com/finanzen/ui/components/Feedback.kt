package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.SageRadius
import kotlinx.coroutines.launch

/**
 * Aviso con "Deshacer" abajo, como overlay (el FAB lo dibuja AppNav encima del NavHost y el Scaffold
 * de la pantalla no lo ve). Con [aboveFab] se apila sobre el FAB. Va como último hijo de un
 * contenedor a pantalla completa.
 */
@Composable
fun BottomSnackbarHost(state: SnackbarHostState, aboveFab: Boolean = false) {
    val spacing = LocalSpacing.current
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg)
            .padding(bottom = if (aboveFab) 88.dp else spacing.lg),
        contentAlignment = Alignment.BottomCenter,
    ) {
        SnackbarHost(state)
    }
}

/** Icono "i" con tooltip, para explicar un control o gráfico sin recargar la fila con texto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTooltip(text: String, contentDescription: String = "Más información", modifier: Modifier = Modifier) {
    val state = rememberTooltipState(isPersistent = false)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = state,
        modifier = modifier,
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                LucideIcons.Info,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Mensaje de validación bajo un campo: icono + texto en color de error, nunca solo color. */
@Composable
fun FieldError(message: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.error
    Row(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
    ) {
        Icon(LucideIcons.AlertTriangle, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(message, color = color, style = LocalSageType.current.caption)
    }
}

/** Tono de un aviso en línea. */
enum class NoticeTone { Info, Success, Warning }

/**
 * Aviso en línea dentro de una pantalla (exportado, advertencia antes de restaurar, nota informativa):
 * fondo suave del tono, icono y texto. Sin borde.
 */
@Composable
fun InlineNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: NoticeTone = NoticeTone.Info,
    trailing: @Composable (() -> Unit)? = null,
) {
    val finance = LocalFinanceColors.current
    val scheme = MaterialTheme.colorScheme
    val (bg, fg, icon) = when (tone) {
        NoticeTone.Info -> Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, LucideIcons.Info)
        NoticeTone.Success -> Triple(finance.incomeContainer, finance.income, LucideIcons.Check)
        NoticeTone.Warning -> Triple(finance.warningContainer, finance.warning, LucideIcons.AlertTriangle)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, style = LocalSageType.current.bodySmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Icono de 28 dp dentro de un cuadrado hundido de 56 dp: cabecera común de los estados vacío y error. */
@Composable
private fun StateIcon(icon: Painter, bg: Color, fg: Color) {
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(SageRadius.lg)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(28.dp))
    }
}

/** Estado vacío: icono, título, explicación y, opcional, la acción que lo resuelve. */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateLayout(modifier) {
        StateIcon(icon, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        StateTexts(title, subtitle)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            SageButton(actionLabel, onAction, kind = SageButtonKind.Secondary)
        }
    }
}

/** Estado de error: qué pasó, cómo seguir y un botón para reintentar. */
@Composable
fun ErrorState(
    title: String,
    subtitle: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Reintentar",
) {
    StateLayout(modifier) {
        StateIcon(LucideIcons.AlertTriangle, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        StateTexts(title, subtitle)
        Spacer(Modifier.height(4.dp))
        SageButton(retryLabel, onRetry, kind = SageButtonKind.Secondary)
    }
}

/** Ancho relativo de las líneas falsas de título y subtítulo en el estado de carga. */
private const val SKELETON_TITLE_WIDTH = 0.6f
private const val SKELETON_SUBTITLE_WIDTH = 0.35f

/** Estado de carga: bloques grises con la forma del contenido que va a llegar. */
@Composable
fun LoadingState(modifier: Modifier = Modifier, rows: Int = 3) {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(SageRadius.md)).background(bg))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(SKELETON_TITLE_WIDTH).height(14.dp).clip(RoundedCornerShape(SageRadius.sm)).background(bg))
                    Box(Modifier.fillMaxWidth(SKELETON_SUBTITLE_WIDTH).height(12.dp).clip(RoundedCornerShape(SageRadius.sm)).background(bg))
                }
                Box(Modifier.size(width = 72.dp, height = 14.dp).clip(RoundedCornerShape(SageRadius.sm)).background(bg))
            }
        }
    }
}

@Composable
private fun StateLayout(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun StateTexts(title: String, subtitle: String) {
    Spacer(Modifier.height(4.dp))
    Text(title, style = LocalSageType.current.titleCard, textAlign = TextAlign.Center)
    Text(
        subtitle,
        style = LocalSageType.current.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 280.dp),
    )
}
