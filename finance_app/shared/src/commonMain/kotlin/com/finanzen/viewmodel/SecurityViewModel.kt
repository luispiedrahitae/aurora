package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

enum class LockState { Unlocked, Locked }

class SecurityViewModel(private val repo: SecurityRepository) : ViewModel() {

    private val _state = MutableStateFlow(if (repo.isLockEnabled()) LockState.Locked else LockState.Unlocked)
    val state: StateFlow<LockState> = _state.asStateFlow()

    private val _attemptError = MutableStateFlow<String?>(null)
    val attemptError: StateFlow<String?> = _attemptError.asStateFlow()

    fun lockEnabled(): Boolean = repo.isLockEnabled()

    fun unlock(pin: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val lockedUntil = repo.lockedUntilMs()
        if (now < lockedUntil) {
            val remainingSec = (lockedUntil - now + 999) / 1000
            _attemptError.value = "Demasiados intentos. Espera ${remainingSec}s."
            return
        }
        if (repo.verifyPin(pin, now)) {
            _attemptError.value = null
            _state.value = LockState.Unlocked
        } else {
            val newLockedUntil = repo.lockedUntilMs()
            _attemptError.value = if (newLockedUntil > now) {
                "Demasiados intentos. Espera ${(newLockedUntil - now + 999) / 1000}s."
            } else {
                "PIN incorrecto"
            }
        }
    }

    /** El SO ya verificó la identidad vía biometría; el PIN sigue siendo la credencial de respaldo. */
    fun unlockBiometric() {
        _attemptError.value = null
        _state.value = LockState.Unlocked
    }

    fun clearError() {
        _attemptError.value = null
    }

    /** Llamado al iniciar la app desde el shell (Main/MainActivity). Re-bloquea si está habilitado. */
    fun lockOnAppStart() {
        if (repo.isLockEnabled()) _state.value = LockState.Locked
    }

    fun setupPin(pin: String): Boolean {
        if (pin.length !in 4..8 || !pin.all { it.isDigit() }) return false
        repo.enableLockWithPin(pin)
        _state.value = LockState.Unlocked
        return true
    }

    fun disableLock() {
        repo.disableLock()
        _state.value = LockState.Unlocked
    }
}
