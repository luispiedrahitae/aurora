package com.finanzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing

/**
 * Header unificado de las 4 pestañas principales. Aporta el inset de status bar (única fuente del
 * tope — ver AppNav, que solo aplica el inset inferior) + título expresivo grande, con slots
 * opcionales para subtítulo y una acción a la derecha. Reemplaza la mezcla previa de "pantallas con
 * TopAppBar" vs "pantallas sin header", que hacía que las pestañas no se sintieran la misma app.
 */
@Composable
fun MainTabHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = spacing.lg, end = spacing.sm, top = spacing.sm, bottom = spacing.xs)
            // Barra superior Sage: 56 dp de alto mínimo, igual en las cinco pestañas tenga o no acción.
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = LocalSageType.current.titleScreen,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = LocalSageType.current.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (action != null) action()
    }
}

/**
 * Header de las 12 sub-pantallas de "Más" (con navegación hacia atrás): mismo tratamiento visual
 * expresivo que [MainTabHeader] — mismo inset de status bar, misma tipografía de título — más una
 * flecha "volver" a la izquierda. Sibling deliberado en vez de un parámetro opcional en
 * MainTabHeader: mantiene MainTabHeader intacto para las 4 pestañas + MoreScreen, y distingue
 * explícitamente "raíz de pestaña" de "sub-pantalla" en cada call site.
 */
@Composable
fun SubScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = spacing.xs, end = spacing.sm, top = spacing.sm, bottom = spacing.xs)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(LucideIcons.ArrowLeft, contentDescription = "Volver")
        }
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = LocalSageType.current.titleTopBar,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (action != null) action()
    }
}

/**
 * Header de las hojas modales (`ModalBottomSheet`) de alta/edición: título + botón "Cerrar" (X) a
 * la derecha, mismo tratamiento tipográfico expresivo que [MainTabHeader]/[SubScreenHeader]. Antes
 * copiado a mano en cada formulario (Movimiento, Cuenta, Presupuesto, Retiro de inversión) — un
 * cambio en uno no se replicaba a los demás sin querer.
 */
@Composable
fun SheetHeader(title: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = LocalSageType.current.titleTopBar,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onDismiss) {
            Icon(LucideIcons.X, contentDescription = "Cerrar")
        }
    }
}
