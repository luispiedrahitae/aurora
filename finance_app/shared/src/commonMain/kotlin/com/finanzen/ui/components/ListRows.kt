package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing

/**
 * Tarjeta que agrupa filas (menú, ajustes, listas cortas): padding vertical mínimo para que las filas
 * lleven su propio aire y un divisor fino entre ellas (lo pone [RowDivider]).
 */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    FinanceCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

/** Divisor de 1 dp entre filas de una [GroupCard]. */
@Composable
fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Título de sección y su contenido, separados 8 dp. */
@Composable
fun TitledSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
        SectionHeader(title)
        content()
    }
}

/**
 * Fila de menú: icono en el color de acento, título, valor actual opcional en gris y chevron. Toda la
 * fila es el botón (56 dp de alto mínimo).
 */
@Composable
fun MenuRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    value: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .rowClickable(onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Text(title, style = LocalSageType.current.titleCard, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = LocalSageType.current.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(LucideIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** Fila con interruptor: título, explicación opcional y el [Switch]. Toda la fila alterna el valor. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TwoLineText(title, subtitle)
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = sageSwitchColors())
    }
}

/** Opción de una lista de radio: círculo, título, detalle opcional y un texto a la derecha (ejemplo). */
@Composable
fun RadioRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline,
            ),
        )
        TwoLineText(title, subtitle)
        if (trailing != null) {
            Text(trailing, style = LocalSageType.current.amountMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowScope.TwoLineText(title: String, subtitle: String?) {
    Column(Modifier.weight(1f)) {
        Text(title, style = LocalSageType.current.titleCard)
        if (subtitle != null) {
            Text(subtitle, style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Fila de lista con avatar: título y detalle a la izquierda, contenido final (monto, insignia) a la
 * derecha. Con [onClick] la fila entera es tocable.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.rowClickable(onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = LocalSageType.current.titleCard, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = LocalSageType.current.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}
