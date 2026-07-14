package com.finanzen.platform

import kotlinx.coroutines.flow.Flow

sealed interface DownloadState {
    data object Idle : DownloadState
    data class InProgress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState
    data object Completed : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * Descarga el .litertlm de Gemma 4 E4B (`litert-community/gemma-4-E4B-it-litert-lm` en
 * HuggingFace — Apache 2.0, sin gate de licencia, a diferencia de Gemma 3n) al almacenamiento
 * privado de la app. En Android corre vía WorkManager: sobrevive navegación fuera de la pantalla
 * del asistente y muerte de proceso (ver `ModelDownloadWorker`). [start] es idempotente (no
 * relanza si ya hay una descarga en curso o completa).
 */
expect class ModelDownloader {
    fun installedModelPath(): String?
    fun deleteModel()
    fun start()
    fun cancel()
    fun state(): Flow<DownloadState>
}
