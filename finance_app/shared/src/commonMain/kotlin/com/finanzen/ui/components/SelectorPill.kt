package com.finanzen.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.PillShape

/**
 * Pastilla de selección del handoff: botones independientes con espacio entre ellos (no un
 * `SegmentedButtonRow` unido) — la activa con relleno sólido de acento y texto claro en negrita,
 * las inactivas en un tono neutro pálido. Estándar compartido entre pantallas: Gasto/Ingreso/
 * Transferencia en [com.finanzen.ui.screens.TransactionFormScreen] y Efectivo/Débito/Ahorros/
 * Crédito en la pantalla de cuentas — [contentPadding] es el único eje que varía entre ellas.
 */
@Composable
fun SelectorPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = LocalSpacing.current.md, horizontal = LocalSpacing.current.sm),
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = PillShape,
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = contentPadding,
    ) {
        Text(
            text,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
