package com.finanzen.platform

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberBackupPicker(onPicked: (String?) -> Unit): () -> Unit = {
    val dialog = FileDialog(null as Frame?, "Elegir backup .finzbkp", FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory
    val name = dialog.file
    val content = if (dir != null && name != null) {
        runCatching { File(dir, name).readText(Charsets.UTF_8) }.getOrNull()
    } else {
        null
    }
    onPicked(content)
}
