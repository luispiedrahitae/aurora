package com.finanzen.platform

import androidx.compose.runtime.Composable

// ponytail: stub iOS. Conectar a UIDocumentPickerViewController en el track iOS.
@Composable
actual fun rememberBackupPicker(onPicked: (String?) -> Unit): () -> Unit = {
    println("[FILEPICKER-iOS stub] picker no implementado")
    onPicked(null)
}
