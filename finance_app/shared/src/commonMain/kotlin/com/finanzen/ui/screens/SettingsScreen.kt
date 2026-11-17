package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.GroupCard
import com.finanzen.ui.components.ScreenColumn
import com.finanzen.ui.components.SegmentedControl
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.components.SwitchRow
import com.finanzen.ui.components.TitledSection
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.AccentPreset
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.ThemeMode
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

/** Colores por fila del selector de acento. */
private const val SWATCHES_PER_ROW = 5

/** Apariencia: tema claro/oscuro, color de acento y animaciones. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val accent by vm.accent.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val reduceMotion by vm.reduceMotion.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Apariencia", onBack = onBack)
        ScreenColumn {
            TitledSection("Tema") {
                SegmentedControl(
                    options = ThemeMode.entries.toList(),
                    selected = themeMode,
                    onSelect = vm::setThemeMode,
                    label = { it.label },
                )
                Text(
                    "Automático sigue el modo claro u oscuro del teléfono.",
                    style = LocalSageType.current.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TitledSection("Color de acento") {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Botones, selección y barras de progreso. Ahora: ${accent.label}.",
                            style = LocalSageType.current.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Filas de igual alto (nombre a dos renglones) para que el espacio entre colores no cambie.
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            vm.accents.chunked(SWATCHES_PER_ROW).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { preset ->
                                        AccentSwatch(preset, preset == accent, { vm.setAccent(preset) }, Modifier.weight(1f))
                                    }
                                    repeat(SWATCHES_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
            TitledSection("Animaciones") {
                GroupCard {
                    SwitchRow(
                        title = "Reducir animaciones",
                        subtitle = "Sustituye las animaciones por cambios instantáneos.",
                        checked = reduceMotion,
                        onCheckedChange = vm::setReduceMotion,
                    )
                }
            }
        }
    }
}

/**
 * Muestra de acento: círculo de 44 dp con el color del preset y su nombre debajo (reservando dos
 * renglones). El elegido lleva check blanco y un anillo en tinta separado por el color de fondo.
 */
@Composable
private fun AccentSwatch(preset: AccentPreset, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .then(if (selected) Modifier.border(2.dp, scheme.onSurface, CircleShape) else Modifier)
                .padding(4.dp)
                .clip(CircleShape)
                .background(preset.swatch),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(LucideIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(
            preset.label,
            style = LocalSageType.current.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().height(32.dp),
        )
    }
}
