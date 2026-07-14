package com.finanzen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Escala ajustada para precisión "Swiss Modernism 2.0" (vs. los 20–36dp anteriores, demasiado suaves
 * para una cuadrícula precisa). Tiles bento pequeños usan `small`/`medium`, el tile hero de vidrio
 * usa `large`/`extraLarge`. Ver DESIGN.md sección "Bento Tiles".
 */
val FinanZenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Forma pill para chips y botones de acción. */
val PillShape = RoundedCornerShape(percent = 50)
