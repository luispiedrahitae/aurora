package com.finanzen.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.finanzen.db.FinanzenDb
import platform.UIKit.UIDevice

actual val platformName: String =
    "iOS ${UIDevice.currentDevice.systemVersion}"

actual class DriverFactory {
    actual fun create(): SqlDriver = NativeSqliteDriver(FinanzenDb.Schema, "finanzen.db")
}

// ponytail: stub. Subir a UNUserNotificationCenter cuando lleguemos a release iOS.
actual class NotificationScheduler {
    actual fun scheduleReminder(id: Long, title: String, body: String, atEpochDay: Long) {
        println("[NOTIF-iOS stub] id=$id at=$atEpochDay '$title': $body")
    }

    actual fun notifyNow(id: Long, title: String, body: String) {
        println("[NOTIF-iOS stub] now id=$id '$title': $body")
    }

    actual fun cancel(id: Long) {
        println("[NOTIF-iOS stub] cancel id=$id")
    }
}

// ponytail: stub. Migrar a NSFileManager + UIActivityViewController + PDFKit cuando lleguemos a release iOS.
actual class ReportExporter {
    actual fun saveCsv(suggestedName: String, content: String): String {
        println("[REPORT-iOS stub] saveCsv '$suggestedName' (${content.length} chars)")
        return "stub: no disponible en esta plataforma todavía"
    }

    actual fun savePdf(suggestedName: String, lines: List<String>): String {
        println("[REPORT-iOS stub] savePdf '$suggestedName' (${lines.size} líneas)")
        return "stub: no disponible en esta plataforma todavía"
    }
}

// ponytail: stub iOS. Conectar a CryptoKit / Security framework (AES.GCM.seal/open) cuando lleguemos a release iOS.
actual class BackupCrypto {
    actual fun encrypt(passphrase: String, plaintext: String): String {
        println("[BACKUP-iOS stub] encrypt no implementado — usar Android o Desktop para crear backups")
        return ""
    }
    actual fun decrypt(passphrase: String, envelope: String): String? {
        println("[BACKUP-iOS stub] decrypt no implementado")
        return null
    }
}

// ponytail: stub iOS. Conectar a NSFileManager + UIDocumentPicker.
actual class BackupIO {
    actual fun writeBackup(filename: String, content: String): String {
        println("[BACKUPIO-iOS stub] writeBackup '$filename' (${content.length} chars)")
        return "stub: backup no disponible en esta plataforma todavía"
    }
}
