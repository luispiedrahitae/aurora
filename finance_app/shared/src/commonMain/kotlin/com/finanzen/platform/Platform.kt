package com.finanzen.platform

import app.cash.sqldelight.db.SqlDriver

expect val platformName: String

expect class DriverFactory {
    fun create(): SqlDriver
}

/**
 * Programa un recordatorio. Stub en todas las plataformas por ahora.
 * - Android: WorkManager + NotificationManager (TODO Phase 11/12)
 * - iOS: UNUserNotificationCenter (TODO)
 * - Desktop: solo log a consola (preview de desarrollo, no se distribuye)
 */
expect class NotificationScheduler {
    fun scheduleReminder(id: Long, title: String, body: String, atEpochDay: Long)
    fun cancel(id: Long)

    /** Publica una notificación inmediata (p.ej. presupuesto alcanzado). */
    fun notifyNow(id: Long, title: String, body: String)
}

/**
 * Una línea de contenido de reporte con su rol visual, para que cada plataforma le dé jerarquía
 * (tamaño/negrita/separadores) al renderizarla sin tener que adivinar el rol a partir del texto.
 */
sealed interface ReportLine {
    data class Title(val text: String) : ReportLine
    data class Section(val text: String) : ReportLine

    /** [value] vacío = línea de texto corrido; no vacío = fila de dos columnas (label a la
     * izquierda, value pegado al margen derecho — para montos/porcentajes). */
    data class Row(val label: String, val value: String = "", val emphasis: Boolean = false) : ReportLine
    data object Divider : ReportLine
    data object Blank : ReportLine
}

/**
 * Escribe el contenido de un reporte al disco/store del SO. Devuelve la ruta o un mensaje de error.
 * - Desktop: ~/Downloads
 * - Android: TODO usar MediaStore (Phase de release Android)
 * - iOS: TODO usar UIActivityViewController + share sheet
 */
expect class ReportExporter {
    fun saveCsv(suggestedName: String, content: String): String
    fun savePdf(suggestedName: String, lines: List<ReportLine>): String
}

/**
 * AES-GCM con clave derivada con PBKDF2-HMAC-SHA256 (600k iteraciones, 256 bits).
 * El envelope devuelto por [encrypt] es un JSON self-describing (salt + iv + ciphertext b64) y
 * puede ser leído por cualquier plataforma con la misma passphrase.
 */
expect class BackupCrypto {
    /** Devuelve el envelope JSON con todo lo necesario para descifrar (sal, IV, ciphertext). */
    fun encrypt(passphrase: String, plaintext: String): String

    /** Devuelve el texto descifrado o null si la passphrase es incorrecta / archivo manipulado. */
    fun decrypt(passphrase: String, envelope: String): String?
}

/**
 * Escribe archivos de backup en la plataforma (Android: MediaStore/Descargas; Desktop: ~/Downloads).
 * La lectura para importar se hace con [rememberBackupPicker], que entrega el contenido directamente.
 */
expect class BackupIO {
    fun writeBackup(filename: String, content: String): String
}
