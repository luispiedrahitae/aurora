package com.finanzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.PillShape

/** Jerarquía de botones Sage: una sola acción primaria por pantalla. */
enum class SageButtonKind { Primary, Secondary, Ghost, Danger }

/** Alto de un botón normal (48) y de uno compacto (36). */
private val ButtonHeight = 48.dp
private val ButtonHeightSmall = 36.dp

/**
 * Botón Sage. [Primary] relleno con el acento, [Secondary] con borde fuerte sobre la superficie,
 * [Ghost] solo texto en el color de acento, [Danger] con borde y texto de error (acciones destructivas).
 * Radio 10, alto 48 (36 con [small]).
 */
@Composable
fun SageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: SageButtonKind = SageButtonKind.Primary,
    icon: Painter? = null,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    val sized = modifier.defaultMinSize(minHeight = if (small) ButtonHeightSmall else ButtonHeight)
    val padding = PaddingValues(horizontal = if (small) 12.dp else 20.dp)
    val content: @Composable RowScope.() -> Unit = { ButtonContent(text, icon) }
    when (kind) {
        SageButtonKind.Primary -> Button(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
            content = content,
        )

        SageButtonKind.Secondary, SageButtonKind.Danger -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            border = BorderStroke(1.dp, if (kind == SageButtonKind.Danger) scheme.error else scheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = scheme.surface,
                contentColor = if (kind == SageButtonKind.Danger) scheme.error else scheme.onSurface,
            ),
            content = content,
        )

        SageButtonKind.Ghost -> TextButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = scheme.onPrimaryContainer),
            content = content,
        )
    }
}

@Composable
private fun ButtonContent(text: String, icon: Painter?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = LocalSageType.current.label)
    }
}

/**
 * Botón flotante de captura rápida. Por defecto es un círculo de 56 dp con "+" y [label] como
 * descripción accesible; con [showLabel] muestra además el texto (variante extendida).
 */
@Composable
fun AddFab(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, showLabel: Boolean = false) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = PillShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
    ) {
        if (!showLabel) {
            Icon(LucideIcons.Plus, contentDescription = label, modifier = Modifier.padding(16.dp).size(24.dp))
            return@Surface
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(LucideIcons.Plus, null, modifier = Modifier.size(24.dp))
            Text(label, style = LocalSageType.current.label)
        }
    }
}

/**
 * Colores del interruptor: pista del acento encendido; apagado, pista hundida con borde y perilla en
 * tinta atenuada para que se vea a 3:1 sobre la tarjeta (WCAG 1.4.11).
 */
@Composable
fun sageSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
)
