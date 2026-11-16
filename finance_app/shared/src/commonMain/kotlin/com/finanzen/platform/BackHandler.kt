package com.finanzen.platform

import androidx.compose.runtime.Composable

/**
 * Intercepta el "atrás" del sistema mientras [enabled]. Compose Multiplatform 1.7.3 no trae
 * `BackHandler` en common; en Android delega al de `androidx.activity`.
 *
 * ponytail: no-op en desktop/iOS (sin gesto de atrás del sistema que interceptar); migrar a
 * `androidx.compose.ui.backhandler.BackHandler` al subir a CMP 1.8+.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
