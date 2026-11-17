package com.finanzen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios Sage: 6 (insignias), 10 (botones, campos, chips), 16 (tarjetas, tablas, hojas). */
object SageRadius {
    val sm = 6.dp
    val md = 10.dp
    val lg = 16.dp
}

val FinanZenShapes = Shapes(
    extraSmall = RoundedCornerShape(SageRadius.sm),
    small = RoundedCornerShape(SageRadius.md),
    medium = RoundedCornerShape(SageRadius.md),
    large = RoundedCornerShape(SageRadius.lg),
    extraLarge = RoundedCornerShape(SageRadius.lg),
)

/** Forma pill: solo para el FAB, las pistas de progreso y los interruptores. */
val PillShape = RoundedCornerShape(percent = 50)
