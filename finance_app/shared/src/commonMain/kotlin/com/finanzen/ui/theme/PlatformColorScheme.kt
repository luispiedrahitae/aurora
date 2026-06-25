package com.finanzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Color dinámico (Material You). Solo Android 12+ devuelve un scheme del wallpaper; iOS/Desktop
 * devuelven null y [FinanZenTheme] cae al teal refinado. Es @Composable porque la API de Android
 * necesita el LocalContext.
 */
@Composable
expect fun platformColorScheme(darkTheme: Boolean): ColorScheme?

/** true solo donde el color dinámico tiene efecto (Android 12+). Decide si mostrar el toggle. */
expect fun dynamicColorSupported(): Boolean
