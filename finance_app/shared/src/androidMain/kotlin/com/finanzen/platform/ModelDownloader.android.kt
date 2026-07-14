package com.finanzen.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

actual class ModelDownloader(private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)

    actual fun installedModelPath(): String? = modelFile(context).takeIf { it.exists() }?.absolutePath

    actual fun deleteModel() {
        modelFile(context).delete()
    }

    actual fun start() {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .build()
        workManager.enqueueUniqueWork(ModelDownloadWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    actual fun cancel() {
        workManager.cancelUniqueWork(ModelDownloadWorker.WORK_NAME)
        File(modelDir(context), "$MODEL_FILENAME.tmp").delete()
    }

    actual fun state(): Flow<DownloadState> = workManager.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME).map { infos ->
        val info = infos.firstOrNull() ?: return@map DownloadState.Idle
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.InProgress(0L, 0L)
            WorkInfo.State.RUNNING -> DownloadState.InProgress(
                bytesDownloaded = info.progress.getLong(KEY_PROGRESS_DOWNLOADED, 0L),
                totalBytes = info.progress.getLong(KEY_PROGRESS_TOTAL, 0L),
            )
            WorkInfo.State.SUCCEEDED -> DownloadState.Completed
            WorkInfo.State.FAILED -> DownloadState.Failed(
                info.outputData.getString(ModelDownloadWorker.KEY_ERROR) ?: "Error de descarga",
            )
            WorkInfo.State.CANCELLED -> DownloadState.Idle
        }
    }
}
