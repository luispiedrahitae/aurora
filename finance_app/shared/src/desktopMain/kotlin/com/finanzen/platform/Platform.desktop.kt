package com.finanzen.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.finanzen.db.FinanzenDb
import java.io.File
import java.util.Properties

actual val platformName: String =
    "Desktop / JVM ${System.getProperty("java.version")} (${System.getProperty("os.name")})"

actual class DriverFactory {
    actual fun create(): SqlDriver {
        val dir = File(System.getProperty("user.home"), ".finanzen").apply { mkdirs() }
        val dbFile = File(dir, "finanzen.db")
        val freshInstall = !dbFile.exists()
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            properties = Properties(),
        )
        if (freshInstall) {
            FinanzenDb.Schema.create(driver)
        }
        return driver
    }
}

// Desktop es preview de desarrollo — no se distribuye, así que el scheduler solo loguea.
actual class NotificationScheduler {
    actual fun scheduleReminder(id: Long, title: String, body: String, atEpochDay: Long) {
        println("[NOTIF-Desktop] id=$id at=$atEpochDay '$title': $body")
    }

    actual fun notifyNow(id: Long, title: String, body: String) {
        println("[NOTIF-Desktop] now id=$id '$title': $body")
    }

    actual fun cancel(id: Long) {
        println("[NOTIF-Desktop] cancel id=$id")
    }
}

/**
 * Desktop: escribe en ~/Downloads. PDF se materializa como .txt con prefijo "PDF STUB" —
 * el preview es para desarrollo y no se distribuye; la salida PDF real vive en Android/iOS.
 */
actual class ReportExporter {
    private val downloadsDir: File =
        File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }

    actual fun saveCsv(suggestedName: String, content: String): String = runCatching {
        val file = uniqueFile(suggestedName, ".csv")
        file.writeText(content, Charsets.UTF_8)
        file.absolutePath
    }.getOrElse { "error: ${it.message}" }

    actual fun savePdf(suggestedName: String, lines: List<ReportLine>): String = runCatching {
        // ponytail: stub Desktop — texto plano marcado. Real PDF solo en Android/iOS (release).
        val file = uniqueFile(suggestedName, ".pdf.txt")
        val header = "=== FinanZen PDF STUB (Desktop preview) ===\n"
        val body = lines.joinToString("\n") { line ->
            when (line) {
                is ReportLine.Title -> "\n${line.text}\n${"=".repeat(line.text.length)}"
                is ReportLine.Section -> "\n-- ${line.text} --"
                is ReportLine.Row -> {
                    val prefix = if (line.emphasis) "* " else "  "
                    if (line.value.isEmpty()) "$prefix${line.label}" else "$prefix${line.label.padEnd(42)}${line.value}"
                }
                ReportLine.Divider -> "-".repeat(40)
                ReportLine.Blank -> ""
            }
        }
        file.writeText(header + body, Charsets.UTF_8)
        file.absolutePath
    }.getOrElse { "error: ${it.message}" }

    private fun uniqueFile(suggestedName: String, ext: String): File {
        val safe = suggestedName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val ts = System.currentTimeMillis()
        return File(downloadsDir, "${safe}_$ts$ext")
    }
}

actual class BackupCrypto {
    actual fun encrypt(passphrase: String, plaintext: String): String = com.finanzen.security.JvmAesGcm.encrypt(passphrase, plaintext)

    actual fun decrypt(passphrase: String, envelope: String): String? = com.finanzen.security.JvmAesGcm.decrypt(passphrase, envelope)
}

actual class BackupIO {
    private val dir: File = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }

    actual fun writeBackup(filename: String, content: String): String = runCatching {
        val safe = filename.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val file = File(dir, "${safe}_${System.currentTimeMillis()}.finzbkp")
        file.writeText(content, Charsets.UTF_8)
        file.absolutePath
    }.getOrElse { "error: ${it.message}" }
}
