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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finanzen.platform.rememberBiometricUnlock
import com.finanzen.ui.theme.LocalFinanceColors
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text("Cauce", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Sigue el flujo de tus finanzas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  Usar biometría")
                }
            }
        }
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
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                "←" -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Borrar dígito")
                else -> Text(label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
