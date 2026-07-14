package com.finanzen.data

import com.finanzen.platform.AssistantDeviceCheck
import com.finanzen.platform.AssistantDeviceChecker
import com.finanzen.platform.DownloadState
import com.finanzen.platform.LlmInferenceEngine
import com.finanzen.platform.ModelDownloader
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio delgado: solo ciclo de vida del motor de inferencia y de la descarga del modelo. La
 * agregación de datos financieros y el system prompt viven en `AssistantViewModel` (lógica pura,
 * no una preocupación de la capa de datos — mismo criterio que el resto de los repositorios/
 * ViewModels de la app).
 */
class AssistantRepository(
    private val engine: LlmInferenceEngine,
    private val downloader: ModelDownloader,
    private val deviceChecker: AssistantDeviceChecker,
) {
    fun deviceCheck(): AssistantDeviceCheck = deviceChecker.check()

    fun modelPath(): String? = downloader.installedModelPath()

    fun startDownload() = downloader.start()

    fun downloadState(): Flow<DownloadState> = downloader.state()

    fun deleteModel() = downloader.deleteModel()

    suspend fun loadModel(): Result<Unit> {
        val path = downloader.installedModelPath() ?: return Result.failure(IllegalStateException("Modelo no descargado"))
        return engine.load(path)
    }

    suspend fun ask(prompt: String, onToken: (String) -> Unit): Result<String> = engine.generate(prompt, onToken)

    fun release() = engine.close()
}
