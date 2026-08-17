package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class LockState { Unlocked, Locked }

class SecurityViewModel(private val repo: SecurityRepository) : ViewModel() {

    private val _state = MutableStateFlow(if (repo.isLockEnabled()) LockState.Locked else LockState.Unlocked)
    val state: StateFlow<LockState> = _state.asStateFlow()

    private val _attemptError = MutableStateFlow<String?>(null)
    val attemptError: StateFlow<String?> = _attemptError.asStateFlow()

    /** true mientras se hashea/verifica el PIN en segundo plano (200k iteraciones SHA-256). */
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun lockEnabled(): Boolean = repo.isLockEnabled()

    fun unlock(pin: String) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val lockedUntil = repo.lockedUntilMs()
                if (now < lockedUntil) {
                    val remainingSec = (lockedUntil - now + 999) / 1000
                    _attemptError.value = "Demasiados intentos. Espera ${remainingSec}s."
                    return@launch
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
            } finally {
                _isBusy.value = false
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

    fun setupPin(pin: String, onResult: (Boolean) -> Unit) {
        if (pin.length !in 4..8 || !pin.all { it.isDigit() }) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repo.enableLockWithPin(pin)
                _state.value = LockState.Unlocked
                onResult(true)
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun disableLock() {
        repo.disableLock()
        _state.value = LockState.Unlocked
    }
}
