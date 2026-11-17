package com.finanzen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finanzen.ui.format.PeriodMode
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.PillShape

/** Selector de mes con flechas prev/next, reutilizado por las pantallas que filtran por periodo.
 * Con [onLabelClick], la etiqueta central es tocable y vuelve al periodo actual ("hoy"). */
@Composable
fun MonthSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = LocalSpacing.current.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(LucideIcons.ChevronLeft, contentDescription = "Mes anterior", tint = MaterialTheme.colorScheme.primary)
        }
        SelectorLabel(label, onLabelClick, onClickLabel = "Volver al mes actual")
        IconButton(onClick = onNext) {
            Icon(LucideIcons.ChevronRight, contentDescription = "Mes siguiente", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Selector de anio con flechas prev/next, mismo patron que [MonthSelector] pero para pantallas
 * anuales (Resumen). */
@Composable
fun YearSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = LocalSpacing.current.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(LucideIcons.ChevronLeft, contentDescription = "Año anterior", tint = MaterialTheme.colorScheme.primary)
        }
        SelectorLabel(label, onLabelClick, onClickLabel = "Volver al año actual")
        IconButton(onClick = onNext) {
            Icon(LucideIcons.ChevronRight, contentDescription = "Año siguiente", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Chip compacto de un toque que alterna entre modo Mensual y Anual — pensado para la esquina
 * superior derecha de [MainTabHeader] (`action`), a la altura del título; el selector prev/next del
 * periodo elegido sigue siendo [MonthSelector]/[YearSelector] debajo, sin cambios.
 */
@Composable
fun PeriodModeChip(mode: PeriodMode, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val label = when (mode) {
        PeriodMode.MONTH -> "Mes"
        PeriodMode.YEAR -> "Año"
    }
    val nextLabel = when (mode) {
        PeriodMode.MONTH -> "año"
        PeriodMode.YEAR -> "mes"
    }
    Surface(
        onClick = onToggle,
        modifier = modifier.minimumInteractiveComponentSize().semantics {
            role = Role.Switch
            contentDescription = "Vista: $label. Cambiar a vista de $nextLabel"
        },
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Icon(
                LucideIcons.ChevronsUpDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Etiqueta central de los selectores de periodo; tocable como boton cuando hay accion "hoy". */
@Composable
private fun SelectorLabel(label: String, onClick: (() -> Unit)?, onClickLabel: String) {
    val clickable = if (onClick != null) {
        Modifier
            .clip(PillShape)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.xs)
    } else {
        Modifier
    }
    Text(label, modifier = clickable, style = MaterialTheme.typography.titleMedium)
}
