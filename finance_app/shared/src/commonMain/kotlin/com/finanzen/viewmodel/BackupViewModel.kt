package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.BackupSerializer
import com.finanzen.db.FinanzenDb
import com.finanzen.platform.BackupCrypto
import com.finanzen.platform.BackupIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class BackupViewModel(
    private val db: FinanzenDb,
    private val crypto: BackupCrypto,
    private val io: BackupIO,
) : ViewModel() {

    private val _status = MutableStateFlow<ExportStatus?>(null)
    val status: StateFlow<ExportStatus?> = _status.asStateFlow()

    fun export(passphrase: String) {
        if (passphrase.length < 8) {
            _status.value = ExportStatus("La passphrase debe tener al menos 8 caracteres", isError = true)
            return
        }
        val snapshot = BackupSerializer.snapshotOf(db, Clock.System.now().toEpochMilliseconds())
        val plaintext = BackupSerializer.toJson(snapshot)
        val envelope = crypto.encrypt(passphrase, plaintext)
        if (envelope.isEmpty()) {
            _status.value = ExportStatus("Cifrado no disponible en esta plataforma (stub)", isError = true)
            return
        }
        val path = io.writeBackup("finanzen-backup", envelope)
        _status.value = ExportStatus(path, isError = path.startsWith("error") || path.startsWith("stub"))
    }

    fun import(passphrase: String, absolutePath: String) {
        if (absolutePath.isBlank()) {
            _status.value = ExportStatus("Indica la ruta absoluta del archivo .finzbkp", isError = true)
            return
        }
        val envelope = io.readBackup(absolutePath)
        if (envelope.isNullOrEmpty()) {
            _status.value = ExportStatus("No pude leer el archivo en esa ruta", isError = true)
            return
        }
        val plaintext = crypto.decrypt(passphrase, envelope)
        if (plaintext == null) {
            _status.value = ExportStatus("Passphrase incorrecta o archivo corrupto", isError = true)
            return
        }
        val snapshot = runCatching { BackupSerializer.fromJson(plaintext) }.getOrNull()
        if (snapshot == null) {
            _status.value = ExportStatus("Formato no reconocido (¿backup de otra versión?)", isError = true)
            return
        }
        BackupSerializer.restore(db, snapshot)
        _status.value = ExportStatus(
            "Restaurado: ${snapshot.transactions.size} transacciones, ${snapshot.accounts.size} cuentas, ${snapshot.cards.size} tarjetas.",
            isError = false,
        )
    }

    fun clearStatus() {
        _status.value = null
    }
}
