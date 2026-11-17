package com.finanzen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finanzen.ui.theme.LocalFinanceColors
import com.finanzen.ui.theme.LocalSageType
import com.finanzen.ui.theme.LocalSpacing
import com.finanzen.ui.theme.SageMotion
import com.finanzen.ui.theme.motionTween

/** Estado de una barra de presupuesto o cupo: normal (acento), cerca del límite (aviso) o excedido (gasto). */
enum class ProgressTone { Normal, Warning, Over }

/** Color de relleno de cada [ProgressTone]. */
@Composable
fun progressColor(tone: ProgressTone): Color = when (tone) {
    ProgressTone.Normal -> MaterialTheme.colorScheme.primary
    ProgressTone.Warning -> LocalFinanceColors.current.warning
    ProgressTone.Over -> LocalFinanceColors.current.expense
}

/** Barra de progreso Sage: pista hundida de 8 dp y relleno en píldora. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val clamped = fraction.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = clamped, animationSpec = motionTween(SageMotion.SLOW))
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(8.dp).progressSemantics(clamped)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        if (animated > 0f) {
            drawRoundRect(color = color, size = Size(size.width * animated, size.height), cornerRadius = radius)
        }
    }
}

/** Fila de categoría: nombre y monto arriba, la [ProgressBar] estándar debajo. */
@Composable
fun CategoryProgressRow(
    name: String,
    amountMinor: Long,
    currency: String,
    pct: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = LocalSageType.current.bodySmall)
            MoneyText(amountMinor, currency, style = LocalSageType.current.amountSm)
        }
        ProgressBar(pct)
    }
}
