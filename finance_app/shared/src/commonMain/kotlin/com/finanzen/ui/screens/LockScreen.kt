package com.finanzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finanzen.platform.rememberBiometricUnlock
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.FinanZenShapes
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.displayFontFamily
import com.finanzen.viewmodel.SecurityViewModel

private const val PIN_MAX = 8

@Composable
fun LockScreen(vm: SecurityViewModel) {
    var entered by remember { mutableStateOf("") }
    val error by vm.attemptError.collectAsState()
    val isBusy by vm.isBusy.collectAsState()
    val finance = LocalFinanceColors.current
    val biometric = rememberBiometricUnlock()

    LaunchedEffect(entered) {
        if (entered.length in 4..PIN_MAX) {
            // No auto-submit; el usuario presiona "OK" en el keypad. Limpio el error al teclear.
            if (error != null) vm.clearError()
        }
    }

    // Ofrece biometría automáticamente al abrir la pantalla de bloqueo.
    LaunchedEffect(Unit) {
        if (biometric.available) biometric.authenticate { vm.unlockBiometric() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xl),
        ) {
            LockIconBadge()
            Text("Sage", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = displayFontFamily()))
            Text(
                "Introduce tu PIN (4–$PIN_MAX dígitos)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PinDots(entered.length, max = PIN_MAX)
            if (isBusy) {
                Text(
                    "Verificando…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            } else {
                error?.let {
                    Text(
                        it,
                        color = finance.expense,
                        style = MaterialTheme.typography.bodyMedium,
                        // liveRegion: el lector de pantalla anuncia el error al fallar el PIN.
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Keypad(
                onDigit = { d -> if (entered.length < PIN_MAX) entered += d },
                onBackspace = { entered = entered.dropLast(1) },
                onSubmit = {
                    if (entered.length >= 4 && !isBusy) {
                        vm.unlock(entered)
                        entered = ""
                    }
                },
            )
            if (biometric.available) {
                TextButton(onClick = { biometric.authenticate { vm.unlockBiometric() } }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
                    ) {
                        // Sin glifo de huella en el set Lucide (39 iconos) — único uso Material
                        // intencional, ver LucideIcons.kt.
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Usar biometría")
                    }
                }
            }
        }
    }
}

/**
 * Vista previa del bloqueo (Apariencia > Seguridad > "Vista previa del bloqueo"): mismo look que
 * [LockScreen] pero sin verificar nada contra el PIN real — el dígito tecleado solo llena los
 * puntos de progreso y se reinicia solo, para que el usuario vea el flujo sin arriesgar un intento
 * fallido real.
 */
@Composable
fun PinPreviewScreen(onCancel: () -> Unit) {
    var entered by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xl),
        ) {
            LockIconBadge()
            Text(
                "Introduce tu PIN (4–$PIN_MAX dígitos)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PinDots(entered.length, max = PIN_MAX)
            Keypad(
                onDigit = { d -> if (entered.length < PIN_MAX) entered += d },
                onBackspace = { entered = entered.dropLast(1) },
                onSubmit = { entered = "" },
            )
            TextButton(onClick = onCancel) { Text("Cancelar vista previa") }
        }
    }
}

@Composable
private fun LockIconBadge() {
    Box(
        modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            LucideIcons.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun PinDots(count: Int, max: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "$count de $max dígitos introducidos" },
    ) {
        repeat(max) { i ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < count) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun Keypad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("←", "0", "OK"),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(0.85f),
    ) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (cell in row) {
                    KeypadCell(
                        label = cell,
                        modifier = Modifier.weight(1f).aspectRatio(1.4f),
                        onClick = {
                            when (cell) {
                                "←" -> onBackspace()
                                "OK" -> onSubmit()
                                else -> onDigit(cell)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadCell(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(FinanZenShapes.small),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                "←" -> Icon(LucideIcons.Backspace, contentDescription = "Borrar dígito")
                else -> Text(label, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
