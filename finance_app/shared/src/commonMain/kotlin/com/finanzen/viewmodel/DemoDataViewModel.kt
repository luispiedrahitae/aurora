package com.finanzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzen.data.TransactionRepository
import com.finanzen.data.loadDemoData
import com.finanzen.db.FinanzenDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * "Cargar datos de ejemplo": solo se ofrece mientras no haya ningún movimiento, para que el usuario
 * pueda probar Tendencias, Resumen y Presupuesto sin registrar meses de datos a mano.
 */
class DemoDataViewModel(private val db: FinanzenDb, txRepo: TransactionRepository) : ViewModel() {

    /** Verdadero mientras la BD no tenga movimientos (nulo hasta la primera lectura). */
    val canLoad: StateFlow<Boolean?> =
        txRepo.observeAll().map { it.isEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun load() {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                loadDemoData(db, Clock.System.todayIn(TimeZone.currentSystemDefault()))
            } finally {
                _loading.value = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
