package com.finanzen.ui.theme

/**
 * `Modifier.blur` renderiza vía Skia en iOS/Desktop (Compose Multiplatform), pero en Android usa
 * `RenderEffect` y requiere API 31+; por debajo es un no-op silencioso. [glassSurface] usa esto para
 * caer a un tile translúcido sin blur en vez de un vidrio "roto" (opaco pero sin desenfoque).
 */
expect fun platformBlurSupported(): Boolean
