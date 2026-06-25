package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import com.finanzen.data.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockState { Unlocked, Locked }

class SecurityViewModel(private val repo: SecurityRepository) : ViewModel() {

    private val _state = MutableStateFlow(if (repo.isLockEnabled()) LockState.Locked else LockState.Unlocked)
    val state: StateFlow<LockState> = _state.asStateFlow()

    private val _attemptError = MutableStateFlow<String?>(null)
    val attemptError: StateFlow<String?> = _attemptError.asStateFlow()

    fun lockEnabled(): Boolean = repo.isLockEnabled()

    fun unlock(pin: String) {
        if (repo.verifyPin(pin)) {
            _attemptError.value = null
            _state.value = LockState.Unlocked
        } else {
            _attemptError.value = "PIN incorrecto"
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
