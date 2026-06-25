package com.finanzen.platform

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

// A4 (carta US) en puntos. Márgenes y alto de línea conservadores para que el texto respire.
private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 40f
private const val LINE_H = 18f
private const val BODY_TEXT = 12f
private const val TITLE_TEXT = 16f

/**
 * Escribe un archivo a la carpeta Descargas pública. API 29+ usa MediaStore (sin permisos);
 * en API 26–28 cae al dir externo de la app (también sin permisos). Devuelve una ruta legible.
 */
internal fun writeToDownloads(
    context: Context,
    displayName: String,
    mime: String,
    write: (OutputStream) -> Unit,
): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo en Descargas")
        resolver.openOutputStream(uri).use { os -> write(requireNotNull(os)) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return "Guardado en Descargas/$displayName"
    }
    // ponytail: <29 sin WRITE_EXTERNAL_STORAGE → dir externo de la app. Suficiente; la mayoría es 29+.
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    val file = File(dir, displayName)
    file.outputStream().use(write)
    return "Guardado en ${file.absolutePath}"
}

/** Renderiza líneas de texto a un PDF multipágina y lo escribe en [os]. La primera línea va como título. */
internal fun writePdf(os: OutputStream, lines: List<String>) {
    val body = Paint().apply {
        textSize = BODY_TEXT
        color = Color.BLACK
        isAntiAlias = true
    }
    val title = Paint().apply {
        textSize = TITLE_TEXT
        color = Color.BLACK
        isFakeBoldText = true
        isAntiAlias = true
    }

    val doc = PdfDocument()
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
    var y = MARGIN + LINE_H
    try {
        lines.forEachIndexed { index, line ->
            if (y > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                y = MARGIN + LINE_H
            }
            page.canvas.drawText(line, MARGIN, y, if (index == 0) title else body)
            y += LINE_H
        }
        doc.finishPage(page)
        doc.writeTo(os)
    } finally {
        doc.close()
    }
}

/** Garantiza la extensión y sanea el nombre para el sistema de archivos. */
internal fun ensureExtension(name: String, ext: String): String {
    val safe = name.replace(Regex("[^A-Za-z0-9_.-]"), "_")
    return if (safe.endsWith(".$ext", ignoreCase = true)) safe else "$safe.$ext"
}
