package com.finanzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.SageRadius

/**
 * Control segmentado Sage (Todos/Ingresos/Gastos, Mes/Año, Claro/Oscuro): fondo hundido y la opción
 * elegida sobre la superficie con borde. Con [fill] las opciones se reparten todo el ancho.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(MaterialTheme.shapes.small)
            .background(scheme.surfaceVariant)
            .padding(2.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(scheme.surface)
                                .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.small)
                        } else {
                            Modifier
                        },
                    )
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(option) })
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = LocalSageType.current.label,
                    color = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Chip Sage: filtro o acción compacta. Elegido = fondo suave del acento con borde del acento. */
@Composable
fun SageChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: Painter? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, style = LocalSageType.current.label, color = scheme.onSurface)
        }
    }
}

/** Tono de una insignia. */
enum class BadgeTone { Neutral, Income, Expense, Warning, Danger }

/** Insignia Sage: texto de 12 sobre fondo suave del tono, radio 6, con icono opcional. */
@Composable
fun SageBadge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.Neutral, icon: Painter? = null) {
    val finance = LocalFinanceColors.current
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        BadgeTone.Neutral -> scheme.surfaceVariant to scheme.onSurface
        BadgeTone.Income -> finance.incomeContainer to finance.income
        BadgeTone.Expense -> finance.expenseContainer to finance.expense
        BadgeTone.Warning -> finance.warningContainer to finance.warning
        BadgeTone.Danger -> scheme.errorContainer to scheme.error
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SageRadius.sm))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        Text(text, style = LocalSageType.current.caption.copy(fontWeight = FontWeight.Medium), color = fg)
    }
}

/** Fila tocable completa para listas de opciones (menú Más, ajustes). */
fun Modifier.rowClickable(onClick: () -> Unit, label: String? = null): Modifier = this.clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
