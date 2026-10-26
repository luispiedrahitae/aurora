package com.finanzen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.components.FinanceCard
import com.finanzen.ui.components.InfoTooltip
import com.finanzen.ui.components.SectionHeader
import com.finanzen.ui.components.SubScreenHeader
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.AccentPreset
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.motionTween
import com.finanzen.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val accent by vm.accent.collectAsState()
    val reduceMotion by vm.reduceMotion.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title = "Apariencia", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Color de acento")
                    FinanceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Color de la app: ${accent.label}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg),
                                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg),
                            ) {
                                vm.accents.forEach { preset ->
                                    AccentSwatch(
                                        preset = preset,
                                        selected = preset == accent,
                                        onClick = { vm.setAccent(preset) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Animaciones")
                    FinanceCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Reducir animaciones", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = reduceMotion, onCheckedChange = vm::setReduceMotion)
                                InfoTooltip(
                                    "Sustituye las animaciones por cambios instantáneos.",
                                    contentDescription = "Qué hace reducir animaciones",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Muestra de acento: círculo con el color del preset, halo animado al seleccionar y check encima.
 * El check va en blanco — todos los primarios claros son lo bastante oscuros para 4.5:1 contra blanco.
 */
@Composable
private fun AccentSwatch(
    preset: AccentPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ring by animateDpAsState(if (selected) 2.dp else 0.dp, motionTween(180), label = "accentRing")
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(LocalSpacing.current.sm)
            .semantics { contentDescription = preset.label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clip(CircleShape).background(preset.swatch),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(motionTween(150)) + scaleIn(motionTween(150), initialScale = 0.6f),
                exit = fadeOut(motionTween(120)) + scaleOut(motionTween(120), targetScale = 0.6f),
            ) {
                Icon(LucideIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
