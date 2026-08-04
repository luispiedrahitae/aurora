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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.finanzen.ui.theme.AccentPreset
import com.finanzen.ui.theme.motionTween
import com.finanzen.viewmodel.SettingsViewModel
import com.finanzen.viewmodel.ThemeMode
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinInject(),
) {
    val theme by vm.theme.collectAsState()
    val accent by vm.accent.collectAsState()
    val dynamicColor by vm.dynamicColor.collectAsState()
    val reduceMotion by vm.reduceMotion.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apariencia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader("Tema") }
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Column {
                        ThemeOption("Sistema", ThemeMode.SYSTEM, theme, vm::setTheme)
                        ThemeOption("Claro", ThemeMode.LIGHT, theme, vm::setTheme)
                        ThemeOption("Oscuro", ThemeMode.DARK, theme, vm::setTheme)
                    }
                }
            }

            item { SectionHeader("Color de acento") }
            item {
                val accentActive = !dynamicColor
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (accentActive) {
                                "Color de la app: ${accent.label}."
                            } else {
                                "El color dinámico está activo. Desactívalo para elegir un acento."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().alpha(if (accentActive) 1f else 0.45f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            vm.accents.forEach { preset ->
                                AccentSwatch(
                                    preset = preset,
                                    selected = preset == accent,
                                    enabled = accentActive,
                                    onClick = { vm.setAccent(preset) },
                                )
                            }
                        }
                    }
                }
            }

            if (vm.dynamicSupported) {
                item { SectionHeader("Modo") }
                item {
                    FinanceCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Dinámico", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = dynamicColor, onCheckedChange = vm::setDynamicColor)
                                InfoTooltip(
                                    "Usa los colores de tu fondo de pantalla para teñir la app.",
                                    contentDescription = "Qué es el color dinámico",
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Animaciones") }
            item {
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

/**
 * Muestra de acento: círculo con el color del preset, halo animado al seleccionar y check encima.
 * El check va en blanco — todos los primarios claros son lo bastante oscuros para 4.5:1 contra blanco.
 */
@Composable
private fun AccentSwatch(
    preset: AccentPreset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val ring by animateDpAsState(if (selected) 2.dp else 0.dp, motionTween(180), label = "accentRing")
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(6.dp)
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
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    mode: ThemeMode,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected == mode, onClick = { onSelect(mode) })
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}
