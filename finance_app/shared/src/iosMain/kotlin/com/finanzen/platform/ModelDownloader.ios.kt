package com.finanzen.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// ponytail: stub iOS. Conectar a URLSession background download tasks cuando lleguemos al track
// iOS del asistente.
actual class ModelDownloader {
    actual fun installedModelPath(): String? = null

    actual fun deleteModel() = Unit

    actual fun start() {
        println("[MODELDOWNLOAD-iOS stub] start() no implementado")
    }

    actual fun cancel() = Unit

    actual fun state(): Flow<DownloadState> = flowOf(DownloadState.Failed("Asistente IA no disponible todavía en iOS"))
}
