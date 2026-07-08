package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Medidor semicircular de la tasa de ahorro. Arco de fondo (surfaceVariant) + arco de valor
 * (verde ingreso) con barrido proporcional, más una marca radial fija en la meta del 20%.
 * El porcentaje exacto va en el centro como número grande, así el color nunca es la única señal.
 */
@Composable
fun SavingsRateGauge(savingsRate: Float, modifier: Modifier = Modifier) {
    val finance = LocalFinanceColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurface
    val target = savingsRate.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(600))
    val pctInt = (target * 100).toInt()

    FinanceCard(
        modifier = modifier.semantics { contentDescription = "Tasa de ahorro: $pctInt por ciento. Meta 20 por ciento." },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                    // Marca de la meta fija (20%): tick radial que cruza el arco de lado a lado.
                    val goalRad = ((180f + 0.2f * 180f) * PI / 180f).toFloat()
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
                    modifier = Modifier.fillMaxWidth(0.5f).padding(bottom = 2.dp),
                    color = finance.income,
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
                "Meta 20%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
