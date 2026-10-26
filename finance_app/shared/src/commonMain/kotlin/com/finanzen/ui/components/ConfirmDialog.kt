package com.finanzen.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.finanzen.ui.theme.LocalFinanceColors

/**
 * Generic reusable confirm sheet: title, body message, Cancelar / destructive-styled confirm
 * button. Intended for irreversible/destructive actions (e.g. hard-deleting an archived account,
 * deleting a custom category) — trigger this instead of deleting immediately.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Eliminar",
    dismissLabel: String = "Cancelar",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            // finance.expense (terracota), no colorScheme.error: DESIGN.md "No-Shame Rule" — los
            // destructivos usan el mismo tono que los botones Eliminar de los formularios.
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = LocalFinanceColors.current.expense) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}
