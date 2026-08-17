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
private const val TITLE_TEXT = 18f
private const val SECTION_TEXT = 13f
private const val ACCENT_COLOR = 0xFF0F766E.toInt() // teal, coherente con el acento de la app

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

/** Renderiza líneas estructuradas a un PDF multipágina y lo escribe en [os], con jerarquía visual por rol. */
internal fun writePdf(os: OutputStream, lines: List<ReportLine>) {
    val body = Paint().apply {
        textSize = BODY_TEXT
        color = Color.BLACK
        isAntiAlias = true
    }
    val bodyBold = Paint().apply {
        textSize = BODY_TEXT
        color = Color.BLACK
        isFakeBoldText = true
        isAntiAlias = true
    }
    val bodyRight = Paint(body).apply { textAlign = Paint.Align.RIGHT }
    val bodyBoldRight = Paint(bodyBold).apply { textAlign = Paint.Align.RIGHT }
    val title = Paint().apply {
        textSize = TITLE_TEXT
        color = ACCENT_COLOR
        isFakeBoldText = true
        isAntiAlias = true
    }
    val section = Paint().apply {
        textSize = SECTION_TEXT
        color = ACCENT_COLOR
        isFakeBoldText = true
        isAntiAlias = true
    }
    val divider = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    val doc = PdfDocument()
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
    var y = MARGIN + LINE_H

    // reserve: cuánto espacio extra debe quedar libre además de la línea actual — usado para que un
    // encabezado de sección nunca quede solo al final de una página, sin ninguna fila debajo.
    fun newPageIfNeeded(reserve: Float = 0f) {
        if (y + reserve > PAGE_H - MARGIN) {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            y = MARGIN + LINE_H
        }
    }

    try {
        lines.forEach { line ->
            newPageIfNeeded()
            when (line) {
                is ReportLine.Title -> {
                    page.canvas.drawText(line.text, MARGIN, y, title)
                    y += LINE_H * 1.5f
                }
                is ReportLine.Section -> {
                    y += LINE_H * 0.5f
                    // encabezado + separador + al menos una fila, para no dejarlo huérfano al final de página.
                    newPageIfNeeded(reserve = LINE_H * 2.3f)
                    page.canvas.drawText(line.text, MARGIN, y, section)
                    y += LINE_H * 0.3f
                    page.canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, divider)
                    y += LINE_H
                }
                is ReportLine.Row -> {
                    val (left, right) = if (line.emphasis) bodyBold to bodyBoldRight else body to bodyRight
                    page.canvas.drawText(line.label, MARGIN, y, left)
                    if (line.value.isNotEmpty()) {
                        page.canvas.drawText(line.value, PAGE_W - MARGIN, y, right)
                    }
                    y += LINE_H
                }
                ReportLine.Divider -> {
                    // El cierre del documento (total + leyenda opcional) va justo después de un
                    // divider — se reserva espacio para que ese bloque final no quede cortado o
                    // pegado al margen inferior de la página.
                    newPageIfNeeded(reserve = LINE_H * 3f)
                    page.canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, divider)
                    // Una línea completa de aire (no 0.5, como en el resto de dividers) porque acá
                    // la línea de texto que sigue va pegada al ascent de la fuente: con menos
                    // espacio, "Total de movimientos..." quedaba montado sobre la línea divisoria.
                    y += LINE_H
                }
                ReportLine.Blank -> y += LINE_H * 0.5f
            }
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
