package com.finanzen.platform

import android.content.Context
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.finanzen.db.FinanzenDb

actual val platformName: String = "Android ${android.os.Build.VERSION.RELEASE}"

actual class DriverFactory(private val context: Context) {
    actual fun create(): SqlDriver = AndroidSqliteDriver(FinanzenDb.Schema, context, "finanzen.db")
}

// ponytail: stub. Subir a WorkManager + NotificationManager en Phase 11/12 cuando lleguemos a producción Android.
actual class NotificationScheduler(@Suppress("unused") private val context: Context) {
    actual fun scheduleReminder(id: Long, title: String, body: String, atEpochDay: Long) {
        Log.d("FinanZen", "[NOTIF-Android stub] id=$id at=$atEpochDay '$title': $body")
    }

    actual fun cancel(id: Long) {
        Log.d("FinanZen", "[NOTIF-Android stub] cancel id=$id")
    }
}

// ponytail: stub. Migrar a MediaStore + PdfDocument cuando lleguemos a release Android.
actual class ReportExporter(@Suppress("unused") private val context: Context) {
    actual fun saveCsv(suggestedName: String, content: String): String {
        Log.d("FinanZen", "[REPORT-Android stub] saveCsv '$suggestedName' (${content.length} chars)")
        return "stub: pendiente integrar MediaStore"
    }

    actual fun savePdf(suggestedName: String, lines: List<String>): String {
        Log.d("FinanZen", "[REPORT-Android stub] savePdf '$suggestedName' (${lines.size} líneas)")
        return "stub: pendiente integrar PdfDocument + MediaStore"
    }
}

// ponytail: stub. Conectar a androidx.biometric.BiometricPrompt + FragmentActivity en release.
actual class BiometricAuth(@Suppress("unused") private val context: Context) {
    actual fun isAvailable(): Boolean = false
    actual fun authenticate(reason: String, onResult: (Boolean) -> Unit) {
        Log.d("FinanZen", "[BIO-Android stub] '$reason' → not configured")
        onResult(false)
    }
}

actual class BackupCrypto {
    actual fun encrypt(passphrase: String, plaintext: String): String = com.finanzen.security.AndroidAesGcm.encrypt(passphrase, plaintext)

    actual fun decrypt(passphrase: String, envelope: String): String? = com.finanzen.security.AndroidAesGcm.decrypt(passphrase, envelope)
}

// ponytail: en Android real, usar MediaStore.Downloads y ACTION_OPEN_DOCUMENT.
// V1 escribe al Files/Documents interno; el path se devuelve para que la UI lo muestre.
actual class BackupIO(private val context: Context) {
    actual fun writeBackup(filename: String, content: String): String = runCatching {
        val safe = filename.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val dir = java.io.File(context.filesDir, "backups").apply { mkdirs() }
        val file = java.io.File(dir, "${safe}_${System.currentTimeMillis()}.finzbkp")
        file.writeText(content, Charsets.UTF_8)
        file.absolutePath
    }.getOrElse { "error: ${it.message}" }

    actual fun readBackup(absolutePath: String): String? = runCatching {
        java.io.File(absolutePath).readText(Charsets.UTF_8)
    }.getOrNull()
}
