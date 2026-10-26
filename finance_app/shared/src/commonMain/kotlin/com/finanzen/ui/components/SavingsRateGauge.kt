package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.icons.LucideIcons
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.motionTween

/**
 * Medidor compacto de la tasa de ahorro (círculo de 64dp + texto al lado), igual que
 * `Sage.dc.html`: anillo de progreso (verde ingreso) sobre pista neutra, porcentaje en el centro.
 * Tocar la tarjeta abre un diálogo para editar la meta.
 */
@Composable
fun SavingsRateGauge(savingsRate: Float, goalPct: Long, onGoalChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val finance = LocalFinanceColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    // El anillo no puede pintar un déficit (no hay barrido negativo): se recorta a 0. El número sí
    // muestra la tasa real, incluida negativa — aplanarla a 0% escondería que el mes fue deficitario.
    val arcTarget = savingsRate.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = arcTarget, animationSpec = motionTween(600))
    val pctInt = (savingsRate.coerceAtMost(1f) * 100).toInt()
    val valueColor = if (pctInt < 0) finance.expense else finance.income
    val aboveGoal = savingsRate * 100f >= goalPct
    var showGoalDialog by remember { mutableStateOf(false) }

    FinanceCard(
        modifier = modifier.semantics {
            contentDescription = "Tasa de ahorro: $pctInt por ciento. Meta $goalPct por ciento. Toca para editar la meta."
        },
        onClick = { showGoalDialog = true },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            // Sage.dc.html: anillo conic-gradient de 84px con un círculo hueco de 64px encima (del
            // mismo tono que la tarjeta) — no un solo trazo de 64px total como antes, que quedaba
            // chico y sin el hueco central del diseño original.
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(84.dp)) {
                    val strokeW = 10.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                    )
                    drawArc(
                        color = finance.income,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                    )
                }
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$pctInt%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = valueColor,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "TASA DE AHORRO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (aboveGoal) "Por encima de tu meta de $goalPct%" else "Meta: $goalPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (aboveGoal) finance.income else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            InfoTooltip(
                "Es lo que ahorraste del mes: (ingresos − gastos) ÷ ingresos, en porcentaje. " +
                    "Un valor negativo significa que gastaste más de lo que ingresó. " +
                    "Sin ingresos registrados este mes, muestra 0%.",
                contentDescription = "Cómo se calcula la tasa de ahorro",
            )
        }
    }

    if (showGoalDialog) {
        SavingsGoalDialog(goalPct = goalPct, onChange = onGoalChange, onDismiss = { showGoalDialog = false })
    }
}

/** Editor de la meta de ahorro: pasos de 5 puntos porcentuales, persiste en cada toque. */
@Composable
private fun SavingsGoalDialog(goalPct: Long, onChange: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meta de ahorro") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onChange((goalPct - 5).coerceAtLeast(0)) }, enabled = goalPct > 0) {
                    Icon(LucideIcons.Minus, contentDescription = "Reducir meta")
                }
                Text(
                    "$goalPct%",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                IconButton(onClick = { onChange((goalPct + 5).coerceAtMost(100)) }, enabled = goalPct < 100) {
                    Icon(LucideIcons.Plus, contentDescription = "Aumentar meta")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}
