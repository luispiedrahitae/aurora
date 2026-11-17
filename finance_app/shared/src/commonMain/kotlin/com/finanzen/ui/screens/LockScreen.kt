package com.finanzen.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finanzen.platform.rememberBiometricUnlock
import com.finanzen.ui.components.FieldError
import com.finanzen.ui.components.SageButton
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.viewmodel.SecurityViewModel

private const val PIN_MIN = 4
private const val PIN_MAX = 8

@Composable
fun LockScreen(vm: SecurityViewModel) {
    var entered by remember { mutableStateOf("") }
    val error by vm.attemptError.collectAsState()
    val isBusy by vm.isBusy.collectAsState()
    val biometric = rememberBiometricUnlock()

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(entered) {
        // Sin envío automático (el PIN mide de 4 a 8): el error se limpia desde el primer dígito nuevo.
        if (entered.isNotEmpty() && error != null) vm.clearError()
    }
    // Vibración al fallar: el error se nota aunque no se esté mirando el texto.
    LaunchedEffect(error) {
        if (error != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    // Ofrece biometría automáticamente al abrir la pantalla de bloqueo.
    LaunchedEffect(Unit) {
        if (biometric.available) biometric.authenticate { vm.unlockBiometric() }
    }

    LockLayout(
        entered = entered,
        status = {
            if (isBusy) {
                Text(
                    "Verificando…",
                    style = LocalSageType.current.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            } else {
                error?.let { FieldError(it) }
            }
        },
        onDigit = { d -> if (entered.length < PIN_MAX) entered += d },
        onBackspace = { entered = entered.dropLast(1) },
        onBiometric = if (biometric.available) ({ biometric.authenticate { vm.unlockBiometric() } }) else null,
        primaryLabel = "Entrar",
        primaryEnabled = entered.length >= PIN_MIN && !isBusy,
        onPrimary = {
            vm.unlock(entered)
            entered = ""
        },
    )
}

/**
 * Vista previa del bloqueo (Seguridad › "Ver el bloqueo"): mismo aspecto que [LockScreen] sin
 * verificar nada contra el PIN real, para que el usuario vea el flujo sin gastar un intento.
 */
@Composable
fun PinPreviewScreen(onCancel: () -> Unit) {
    var entered by remember { mutableStateOf("") }
    LockLayout(
        entered = entered,
        status = {
            Text("Vista previa: no se verifica nada.", style = LocalSageType.current.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onDigit = { d -> if (entered.length < PIN_MAX) entered += d },
        onBackspace = { entered = entered.dropLast(1) },
        onBiometric = null,
        primaryLabel = "Cerrar vista previa",
        primaryEnabled = true,
        onPrimary = onCancel,
    )
}

/** Estructura común: marca arriba, puntos del PIN, teclado de 3×4 y la acción principal. */
@Composable
private fun LockLayout(
    entered: String,
    status: @Composable () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?,
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LucideIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
            Text(
                "Sage",
                style = LocalSageType.current.wordmark,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Escribe tu PIN para entrar",
                style = LocalSageType.current.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            PinDots(entered.length, Modifier.padding(top = 32.dp))
            Box(Modifier.heightIn(min = 40.dp).padding(top = 12.dp), contentAlignment = Alignment.Center) { status() }
            Keypad(onDigit, onBackspace, onBiometric, Modifier.padding(top = 16.dp))
            SageButton(
                primaryLabel,
                onPrimary,
                Modifier.padding(top = 24.dp).widthIn(min = 248.dp),
                enabled = primaryEnabled,
            )
            Text(
                "Tus datos viven solo en este teléfono.",
                style = LocalSageType.current.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

/** Puntos del PIN: rellenos en tinta los escritos, solo contorno los que faltan (mínimo 4 visibles). */
@Composable
private fun PinDots(count: Int, modifier: Modifier = Modifier) {
    val shown = maxOf(PIN_MIN, count)
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = "$count dígitos escritos" },
    ) {
        repeat(shown) { i ->
            val filled = i < count
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .then(if (filled) Modifier.background(ink) else Modifier.border(2.dp, muted, CircleShape)),
            )
        }
    }
}

/** Teclado numérico de teclas circulares de 72 dp; abajo, huella (si hay) y borrar. */
@Composable
private fun Keypad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onBiometric: (() -> Unit)?, modifier: Modifier = Modifier) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                row.forEach { d -> KeyButton(onClick = { onDigit(d) }) { Text(d, style = LocalSageType.current.amountLg) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            if (onBiometric != null) {
                KeyButton(onClick = onBiometric, flat = true, description = "Usar huella o rostro") {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            } else {
                Spacer(Modifier.size(KEY_SIZE))
            }
            KeyButton(onClick = { onDigit("0") }) { Text("0", style = LocalSageType.current.amountLg) }
            KeyButton(onClick = onBackspace, flat = true, description = "Borrar dígito") {
                Icon(LucideIcons.Backspace, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun KeyButton(onClick: () -> Unit, flat: Boolean = false, description: String? = null, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.size(KEY_SIZE).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        shape = CircleShape,
        color = if (flat) Color.Transparent else scheme.surface,
        contentColor = if (flat) scheme.onSurfaceVariant else scheme.onSurface,
        border = if (flat) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private val KEY_SIZE = 72.dp
