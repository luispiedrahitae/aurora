package com.finanzen.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Preferencia "Reducir animaciones" (Ajustes › Apariencia), provista por [FinanZenTheme]. Cuando
 * está activa, las animaciones decorativas colapsan a un cambio instantáneo ([snap]).
 *
 * ponytail: flag manual en Ajustes; leer la preferencia del SO (ANIMATOR_DURATION_SCALE en Android,
 * isReduceMotionEnabled en iOS) exigiría un expect/actual nuevo — añadirlo si algún usuario lo pide.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** `tween(ms)` normal, o [snap] si el usuario pidió reducir animaciones. */
@Composable
fun <T> motionTween(ms: Int, delayMs: Int = 0): FiniteAnimationSpec<T> = if (LocalReduceMotion.current) snap() else tween(ms, delayMillis = delayMs)
