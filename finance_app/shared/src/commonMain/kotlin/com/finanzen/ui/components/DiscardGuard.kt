package com.finanzen.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Estado de hoja que no se deja cerrar (deslizar abajo, tocar fuera, atrás) mientras [dirty]: en su
 * lugar llama [onDiscardRequest] para que la pantalla muestre [DiscardChangesDialog]. Evita perder lo
 * escrito por un roce accidental.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberGuardedSheetState(dirty: Boolean, onDiscardRequest: () -> Unit): SheetState {
    val currentDirty by rememberUpdatedState(dirty)
    val currentRequest by rememberUpdatedState(onDiscardRequest)
    return rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            if (value == SheetValue.Hidden && currentDirty) {
                currentRequest()
                false
            } else {
                true
            }
        },
    )
}

/** Confirmación al cerrar un formulario con cambios sin guardar. */
@Composable
fun DiscardChangesDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    ConfirmDialog(
        title = "¿Descartar cambios?",
        body = "Lo que escribiste en este formulario no se guardará.",
        confirmLabel = "Descartar",
        dismissLabel = "Seguir editando",
        onConfirm = onDiscard,
        onDismiss = onKeepEditing,
    )
}
