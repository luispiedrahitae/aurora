package com.finanzen.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Material del único tile de vidrio permitido por pantalla (DESIGN.md, "The One Glass Tile Rule").
 * No hay backdrop-blur real del contenido detrás (Compose sin dependencias externas no lo soporta
 * de forma multiplataforma) — el efecto es translucidez + borde + realce superior, con un desenfoque
 * decorativo sutil donde la plataforma lo soporta bien ([platformBlurSupported]).
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    colors: FinanceColors = LocalFinanceColors.current,
): Modifier {
    val blurred = remember { platformBlurSupported() }
    return this
        .clip(shape)
        .then(if (blurred) Modifier.blur(0.5.dp) else Modifier)
        .background(colors.glassSurface, shape)
        .border(1.dp, colors.glassBorder, shape)
}
