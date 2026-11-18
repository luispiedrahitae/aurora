package com.finanzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalSageType

/**
 * Opción de selección suelta (tipos de cuenta, sí/no): misma piel que [SageChip] elegido o no, pero
 * pensada para repartirse el ancho con `Modifier.weight`. Para opciones excluyentes de una barra
 * preferir [SegmentedControl].
 */
@Composable
fun SelectorPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        shape = MaterialTheme.shapes.small,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outline),
    ) {
        Box(Modifier.padding(contentPadding), contentAlignment = Alignment.Center) {
            Text(
                text,
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = LocalSageType.current.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                color = scheme.onSurface,
            )
        }
    }
}
