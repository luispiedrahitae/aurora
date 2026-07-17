package com.finanzen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Una acción del FAB desplegable: etiqueta, ícono y color del ícono. */
data class SpeedDialAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

/**
 * FAB tipo "speed-dial": un botón principal que despliega hacia arriba acciones etiquetadas.
 * Ocupa toda el área disponible para pintar el scrim; el botón se ancla abajo a la derecha.
 * Las acciones se muestran de arriba a abajo en el orden recibido (la última queda junto al FAB).
 */
@Composable
fun SpeedDialFab(
    actions: List<SpeedDialAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Agregar",
) {
    Box(modifier.fillMaxSize()) {
        // Scrim: oscurece el fondo y cierra al tocar fuera.
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.matchParentSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onExpandedChange(false) },
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.forEachIndexed { index, action ->
                // Entrada escalonada: las de más arriba aparecen un poco después.
                val delay = (actions.size - 1 - index) * 35
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(160, delayMillis = delay)) +
                        slideInVertically(tween(180, delayMillis = delay)) { it / 3 } +
                        scaleIn(tween(160, delayMillis = delay), initialScale = 0.8f),
                    exit = fadeOut(tween(100)) + slideOutVertically(tween(120)) { it / 3 } + scaleOut(tween(100)),
                ) {
                    SpeedDialRow(action) { onExpandedChange(false) }
                }
            }

            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
            ) {
                Icon(
                    if (expanded) Icons.Outlined.Close else Icons.Outlined.Add,
                    contentDescription = if (expanded) "Cerrar" else contentDescription,
                )
            }
        }
    }
}

@Composable
private fun SpeedDialRow(action: SpeedDialAction, onChosen: () -> Unit) {
    val click = {
        onChosen()
        action.onClick()
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            onClick = click,
        ) {
            Text(
                action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        SmallFloatingActionButton(
            onClick = click,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
            ),
        ) {
            Icon(action.icon, contentDescription = action.label, tint = action.tint)
        }
    }
}
