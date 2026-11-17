package com.finanzen.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado Sage (4/8/12/16/20/24/32/48). Las pantallas usan estos pasos, nunca `dp` sueltos:
 * 16 de margen lateral, 24 entre secciones, 8 entre el título de sección y su contenido.
 */
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xlSummary: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
