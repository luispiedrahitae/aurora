package com.finanzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing

/**
 * Envuelve [content] en un gesto de deslizar-para-eliminar (deslizar de derecha a izquierda revela
 * un fondo terracota con el icono de papelera), el mismo patrón ya usado en Movimientos y Cuentas —
 * evita triplicarlo inline en cada pantalla de "Más" que tenía en su lugar un botón de papelera
 * suelto en la tarjeta. [onSwipeToDelete] decide si el gesto se consuma (`true`, la fila desaparece)
 * o si vuelve a su sitio (`false`) — p. ej. para abrir un diálogo de confirmación en vez de borrar
 * directo. [shape] recorta el fondo rojo al mismo radio que la tarjeta que envuelve (por defecto el
 * de [FinanceCard]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteRow(
    onSwipeToDelete: () -> Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) onSwipeToDelete() else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
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
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    // Terracota (finance.expense*), no el rojo de error: DESIGN.md "No-Shame Rule".
                    .background(LocalFinanceColors.current.expenseContainer)
                    .padding(horizontal = LocalSpacing.current.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(LucideIcons.Trash, contentDescription = "Eliminar", tint = LocalFinanceColors.current.expense)
            }
        },
        content = { content() },
    )
}

/**
 * Deslizar a la izquierda para archivar (Cuentas, Inversiones). Libera la tarjeta de un botón fijo;
 * el gesto no es alcanzable con lector de pantalla, así que la misma acción va en las acciones
 * personalizadas de TalkBack. [onSwipeRight] (opcional, p. ej. reabrir una inversión cerrada) habilita
 * deslizar a la derecha; la tarjeta vuelve a su sitio y la lista la reubica sola.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToArchive(onArchive: () -> Unit, onSwipeRight: (() -> Unit)? = null, swipeRightLabel: String = "", content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onArchive()
                    true
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeRight?.invoke()
                    false
                }

                else -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
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
        backgroundContent = {
            val right = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = LocalSpacing.current.lg),
                contentAlignment = if (right) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (right) {
                    Icon(LucideIcons.ArchiveRestore, contentDescription = swipeRightLabel, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                } else {
                    Icon(LucideIcons.Archive, contentDescription = "Archivar", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        },
        content = { content() },
    )
}

/**
 * Fila de un elemento archivado: deslizar a la derecha restaura, a la izquierda pide el borrado
 * permanente. [onHardDelete] solo abre el ConfirmDialog ("no se puede deshacer"), así que la fila
 * no se descarta y vuelve a su sitio hasta que el usuario confirme o cancele.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedSwipeRow(onRestore: () -> Unit, onHardDelete: () -> Unit, content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRestore()
                    true
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onHardDelete()
                    false
                }

                else -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
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
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = spacing.lg),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(LucideIcons.ArchiveRestore, contentDescription = "Restaurar", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                SwipeToDismissBoxValue.EndToStart -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(finance.expenseContainer)
                        .padding(horizontal = spacing.lg),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(LucideIcons.Trash, contentDescription = "Eliminar permanentemente", tint = finance.expense)
                }

                else -> {}
            }
        },
        content = { content() },
    )
}
