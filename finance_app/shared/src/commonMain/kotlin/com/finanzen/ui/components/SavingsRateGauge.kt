package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.motionTween
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Medidor semicircular de la tasa de ahorro. Arco de fondo (surfaceVariant) + arco de valor
 * (verde ingreso) con barrido proporcional, más una marca radial fija en [goalPct]. El porcentaje
 * exacto va en el centro como número grande, así el color nunca es la única señal. Tocar la
 * tarjeta abre un diálogo para editar la meta.
 */
@Composable
fun SavingsRateGauge(savingsRate: Float, goalPct: Long, onGoalChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val finance = LocalFinanceColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.primary
    // El arco no puede pintar un déficit (no hay barrido negativo): se recorta a 0. El número sí
    // muestra la tasa real, incluida negativa — aplanarla a 0% escondería que el mes fue deficitario.
    val arcTarget = savingsRate.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = arcTarget, animationSpec = motionTween(600))
    val pctInt = (savingsRate.coerceAtMost(1f) * 100).toInt()
    val valueColor = if (pctInt < 0) finance.expense else finance.income
    val goalFraction = (goalPct / 100f).coerceIn(0f, 1f)
    var showGoalDialog by remember { mutableStateOf(false) }

    FinanceCard(
        modifier = modifier.semantics {
            contentDescription = "Tasa de ahorro: $pctInt por ciento. Meta $goalPct por ciento. Toca para editar la meta."
        },
        onClick = { showGoalDialog = true },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Canvas(Modifier.fillMaxWidth().aspectRatio(2f)) {
                        val strokeW = 26f
                        val cx = size.width / 2f
                        val cy = size.height - strokeW / 2f
                        val r = (min(size.width / 2f, size.height) - strokeW).coerceAtLeast(1f)
                        val topLeft = Offset(cx - r, cy - r)
                        val arcSize = Size(r * 2f, r * 2f)
                        drawArc(
                            color = trackColor,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = finance.income,
                            startAngle = 180f,
                            sweepAngle = 180f * animated,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                        // Marca de la meta (editable): tick radial que cruza el arco de lado a lado.
                        val goalRad = ((180f + goalFraction * 180f) * PI / 180f).toFloat()
                        val cosA = cos(goalRad)
                        val sinA = sin(goalRad)
                        val inner = r - strokeW / 2f - 2f
                        val outer = r + strokeW / 2f + 2f
                        drawLine(
                            color = markerColor,
                            start = Offset(cx + inner * cosA, cy + inner * sinA),
                            end = Offset(cx + outer * cosA, cy + outer * sinA),
                            strokeWidth = 3f,
                        )
                    }
                    AutoSizeText(
                        text = "$pctInt%",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                        color = valueColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "Tasa de ahorro",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Meta $goalPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Icon(Icons.Outlined.Remove, contentDescription = "Reducir meta")
                }
                Text(
                    "$goalPct%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                IconButton(onClick = { onChange((goalPct + 5).coerceAtMost(100)) }, enabled = goalPct < 100) {
                    Icon(Icons.Outlined.Add, contentDescription = "Aumentar meta")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}
