package com.finanzen.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal const val MODEL_DOWNLOAD_URL =
    "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true"
internal const val MODEL_FILENAME = "gemma-4-e4b-it.litertlm"
internal const val KEY_PROGRESS_DOWNLOADED = "downloaded"
internal const val KEY_PROGRESS_TOTAL = "total"

private const val ASSISTANT_DOWNLOAD_CHANNEL_ID = "finanzen_assistant_download"
private const val ASSISTANT_DOWNLOAD_NOTIF_ID = 9001
private const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
private const val PROGRESS_STEP_BYTES = 2L * 1024 * 1024

internal fun modelDir(context: Context): File = File(context.filesDir, "models").apply { mkdirs() }
internal fun modelFile(context: Context): File = File(modelDir(context), MODEL_FILENAME)

private fun ensureAssistantDownloadChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val mgr = context.getSystemService(NotificationManager::class.java) ?: return
    if (mgr.getNotificationChannel(ASSISTANT_DOWNLOAD_CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        ASSISTANT_DOWNLOAD_CHANNEL_ID,
        "Descarga del asistente IA",
        NotificationManager.IMPORTANCE_LOW,
    ).apply { description = "Progreso de la descarga del modelo del asistente IA." }
    mgr.createNotificationChannel(channel)
}

/**
 * Descarga en streaming el modelo del asistente a `filesDir/models/<archivo>.tmp`, reportando
 * progreso vía [setProgress]/notificación foreground, y solo promueve el `.tmp` al archivo final
 * si el conteo de bytes coincide con `Content-Length`. Sin resume por rango HTTP — cualquier
 * reinicio (retry de WorkManager, worker matado a mitad de descarga) borra el `.tmp` y vuelve a
 * descargar completo; complejidad de resume no justificada para una app personal de un usuario.
 */
class ModelDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ensureAssistantDownloadChannel(applicationContext)
        setForeground(foregroundInfo(0, 0))
        val tmp = File(modelDir(applicationContext), "$MODEL_FILENAME.tmp")
        return try {
            withContext(Dispatchers.IO) {
                val connection = URL(MODEL_DOWNLOAD_URL).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                val total = connection.contentLengthLong
                connection.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                        var downloaded = 0L
                        var lastReported = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (downloaded - lastReported > PROGRESS_STEP_BYTES) {
                                lastReported = downloaded
                                setProgress(workDataOf(KEY_PROGRESS_DOWNLOADED to downloaded, KEY_PROGRESS_TOTAL to total))
                                setForeground(foregroundInfo(downloaded, total))
                            }
                        }
                        if (total > 0 && downloaded != total) error("Descarga incompleta: $downloaded de $total bytes")
                    }
                }
            }
            val dest = modelFile(applicationContext)
            if (!tmp.renameTo(dest)) error("No se pudo mover el archivo descargado a su ubicación final")
            Result.success()
        } catch (t: Throwable) {
            tmp.delete()
            Result.failure(workDataOf(KEY_ERROR to (t.message ?: "Error de descarga")))
        }
    }

    private fun foregroundInfo(downloaded: Long, total: Long): ForegroundInfo {
        val progressPct = if (total > 0) ((downloaded * 100) / total).toInt() else 0
        val notification = NotificationCompat.Builder(applicationContext, ASSISTANT_DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando modelo del asistente")
            .setProgress(100, progressPct, total <= 0)
            .setOngoing(true)
            .build()
        return ForegroundInfo(ASSISTANT_DOWNLOAD_NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_ERROR = "error"
        const val WORK_NAME = "assistant_model_download"
    }
}
