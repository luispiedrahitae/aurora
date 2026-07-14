package com.finanzen.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val modelDir = File(System.getProperty("user.home"), ".finanzen/models").apply { mkdirs() }
private const val MODEL_FILENAME = "gemma-4-e4b-it.litertlm"
private const val MODEL_DOWNLOAD_URL =
    "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true"

/**
 * Desktop es preview de desarrollo — no hay motor de inferencia real (ver
 * `LlmInferenceEngine.desktop.kt`), pero sí hace una descarga real para poder previsualizar la UI
 * de progreso. Sin WorkManager en JVM desktop: un scope de app-lifetime alcanza, no hay pantalla
 * que sobrevivir ni proceso que reiniciar en un preview de un solo usuario.
 */
actual class ModelDownloader {
    private val modelFile = File(modelDir, MODEL_FILENAME)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateFlow = MutableStateFlow<DownloadState>(DownloadState.Idle)

    actual fun installedModelPath(): String? = modelFile.takeIf { it.exists() }?.absolutePath

    actual fun deleteModel() {
        modelFile.delete()
    }

    actual fun start() {
        if (stateFlow.value is DownloadState.InProgress) return
        scope.launch {
            stateFlow.value = DownloadState.InProgress(0L, 0L)
            runCatching {
                val tmp = File(modelDir, "$MODEL_FILENAME.tmp")
                val connection = URL(MODEL_DOWNLOAD_URL).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()
                val total = connection.contentLengthLong
                connection.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(65536)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            stateFlow.value = DownloadState.InProgress(downloaded, total)
                        }
                    }
                }
                if (!tmp.renameTo(modelFile)) error("No se pudo mover el archivo descargado")
            }.onSuccess {
                stateFlow.value = DownloadState.Completed
            }.onFailure {
                stateFlow.value = DownloadState.Failed(it.message ?: "Error de descarga")
            }
        }
    }

    actual fun cancel() {
        stateFlow.value = DownloadState.Idle
    }

    actual fun state(): Flow<DownloadState> = stateFlow.asStateFlow()
}
