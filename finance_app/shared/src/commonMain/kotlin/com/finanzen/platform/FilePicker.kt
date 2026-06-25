package com.finanzen.platform

import androidx.compose.runtime.Composable

/**
 * Devuelve un disparador que abre el selector de archivos del SO y entrega el contenido del
 * archivo elegido (texto) a [onPicked], o null si el usuario cancela / falla la lectura.
 * - Android: SAF (ACTION_OPEN_DOCUMENT)
 * - Desktop: java.awt.FileDialog
 * - iOS: stub (UIDocumentPicker, pendiente track iOS)
 */
@Composable
expect fun rememberBackupPicker(onPicked: (String?) -> Unit): () -> Unit
