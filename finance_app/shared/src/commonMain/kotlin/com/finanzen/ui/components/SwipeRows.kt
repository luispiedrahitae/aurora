package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Lanza [action] y, si devuelve `false` (la acción solo abrió una confirmación o no aplica), devuelve
 * la fila a su sitio. Con `true` la fila queda descartada y la lista la quita sola.
 */
private fun CoroutineScope.settle(state: SwipeToDismissBoxState, action: () -> Boolean) {
    if (!action()) launch { state.reset() }
}

/** Fondo que aparece detrás de la fila al deslizar: color suave, icono en el color pleno. */
@Composable
private fun SwipeBackground(icon: Painter, description: String, bg: Color, fg: Color, alignEnd: Boolean, shape: Shape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(bg)
            .padding(horizontal = LocalSpacing.current.lg),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Icon(icon, contentDescription = description, tint = fg)
    }
}

/**
 * Deslizar de derecha a izquierda para eliminar: revela un fondo terracota con la papelera.
 * [onSwipeToDelete] decide si el gesto se consuma (`true`, la fila desaparece) o si vuelve a su sitio
 * (`false`), p. ej. para abrir una confirmación en vez de borrar directo. [shape] recorta el fondo al
 * radio del contenedor.
 */
@Composable
fun SwipeToDeleteRow(
    onSwipeToDelete: () -> Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val finance = LocalFinanceColors.current
    SwipeToDismissBox(
        state = state,
        // El gesto no es alcanzable con lector de pantalla: misma acción como acción de TalkBack.
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Eliminar") {
                    onSwipeToDelete()
                    true
                },
            )
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { scope.settle(state, onSwipeToDelete) },
        backgroundContent = {
            // Terracota (gasto), no el rojo de error: borrar no es una alarma.
            SwipeBackground(LucideIcons.Trash, "Eliminar", finance.expenseContainer, finance.expense, alignEnd = true, shape = shape)
        },
        content = { content() },
    )
}

/**
 * Deslizar a la izquierda para archivar (Cuentas, Fondos). La misma acción va en las acciones de
 * TalkBack. [onSwipeRight] (opcional, p. ej. reabrir un fondo cerrado) habilita deslizar a la derecha;
 * la tarjeta vuelve a su sitio y la lista la reubica sola.
 */
@Composable
fun SwipeToArchive(onArchive: () -> Unit, onSwipeRight: (() -> Unit)? = null, swipeRightLabel: String = "", content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.semantics {
            customActions = listOfNotNull(
                CustomAccessibilityAction("Archivar") {
                    onArchive()
                    true
                },
                onSwipeRight?.let { action ->
                    CustomAccessibilityAction(swipeRightLabel) {
                        action()
                        true
                    }
                },
            )
        },
        enableDismissFromStartToEnd = onSwipeRight != null,
        onDismiss = { value ->
            scope.settle(state) {
                when (value) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        onArchive()
                        true
                    }

                    SwipeToDismissBoxValue.StartToEnd -> {
                        onSwipeRight?.invoke()
                        false
                    }

                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        },
        backgroundContent = {
            val right = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            SwipeBackground(
                icon = if (right) LucideIcons.ArchiveRestore else LucideIcons.Archive,
                description = if (right) swipeRightLabel else "Archivar",
                bg = scheme.surfaceVariant,
                fg = scheme.onSurfaceVariant,
                alignEnd = !right,
                shape = MaterialTheme.shapes.large,
            )
        },
        content = { content() },
    )
}

/**
 * Fila de un elemento archivado: deslizar a la derecha restaura, a la izquierda pide el borrado
 * permanente. [onHardDelete] solo abre la confirmación, así que la fila vuelve a su sitio hasta que el
 * usuario confirme o cancele.
 */
@Composable
fun ArchivedSwipeRow(onRestore: () -> Unit, onHardDelete: () -> Unit, content: @Composable () -> Unit) {
    val finance = LocalFinanceColors.current
    val scheme = MaterialTheme.colorScheme
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Restaurar") {
                    onRestore()
                    true
                },
                CustomAccessibilityAction("Eliminar permanentemente") {
                    onHardDelete()
                    true
                },
            )
        },
        onDismiss = { value ->
            scope.settle(state) {
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onRestore()
                        true
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        onHardDelete()
                        false
                    }

                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        },
        backgroundContent = {
            when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd ->
                    SwipeBackground(LucideIcons.ArchiveRestore, "Restaurar", scheme.surfaceVariant, scheme.onSurfaceVariant, alignEnd = false, shape = MaterialTheme.shapes.large)

                SwipeToDismissBoxValue.EndToStart ->
                    SwipeBackground(LucideIcons.Trash, "Eliminar permanentemente", finance.expenseContainer, finance.expense, alignEnd = true, shape = MaterialTheme.shapes.large)

                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        content = { content() },
    )
}
