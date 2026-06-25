package com.finanzen.platform

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.finanzen.db.FinanzenDb
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import java.util.concurrent.TimeUnit

actual val platformName: String = "Android ${android.os.Build.VERSION.RELEASE}"

actual class DriverFactory(private val context: Context) {
    actual fun create(): SqlDriver = AndroidSqliteDriver(FinanzenDb.Schema, context, "finanzen.db")
}

/**
 * Programa recordatorios con WorkManager (inexacto, evita SCHEDULE_EXACT_ALARM). El Worker
 * [ReminderWorker] publica la notificación en la fecha indicada (a las 9:00 locales). Cada
 * recordatorio usa un nombre único por id para poder reemplazarlo/cancelarlo.
 */
actual class NotificationScheduler(private val context: Context) {
    actual fun scheduleReminder(id: Long, title: String, body: String, atEpochDay: Long) {
        val triggerAt = LocalDate.fromEpochDays(atEpochDay.toInt())
            .atTime(LocalTime(REMINDER_HOUR, 0))
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val delayMs = triggerAt - System.currentTimeMillis()
        // No notificamos recordatorios cuya fecha ya pasó.
        if (delayMs <= 0) {
            cancel(id)
            return
        }
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_NOTIF_ID to id, KEY_TITLE to title, KEY_BODY to body))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
    }

    actual fun cancel(id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    private fun workName(id: Long): String = "finanzen_reminder_$id"

    private companion object {
        const val REMINDER_HOUR = 9
    }
}

/** Exporta reportes a la carpeta Descargas: CSV directo y PDF renderizado con PdfDocument. */
actual class ReportExporter(private val context: Context) {
    actual fun saveCsv(suggestedName: String, content: String): String = runCatching {
        writeToDownloads(context, ensureExtension(suggestedName, "csv"), "text/csv") { os ->
            os.write(content.toByteArray(Charsets.UTF_8))
        }
    }.getOrElse { "error: ${it.message}" }

    actual fun savePdf(suggestedName: String, lines: List<String>): String = runCatching {
        writeToDownloads(context, ensureExtension(suggestedName, "pdf"), "application/pdf") { os ->
            writePdf(os, lines)
        }
    }.getOrElse { "error: ${it.message}" }
}

actual class BackupCrypto {
    actual fun encrypt(passphrase: String, plaintext: String): String = com.finanzen.security.AndroidAesGcm.encrypt(passphrase, plaintext)

    actual fun decrypt(passphrase: String, envelope: String): String? = com.finanzen.security.AndroidAesGcm.decrypt(passphrase, envelope)
}

// Escribe el backup cifrado a Descargas vía MediaStore (sin permisos). La importación usa
// rememberBackupPicker (SAF), así que aquí solo necesitamos escribir.
actual class BackupIO(private val context: Context) {
    actual fun writeBackup(filename: String, content: String): String = runCatching {
        writeToDownloads(context, ensureExtension(filename, "finzbkp"), "application/octet-stream") { os ->
            os.write(content.toByteArray(Charsets.UTF_8))
        }
    }.getOrElse { "error: ${it.message}" }
}
