package com.finanzen.platform

/**
 * RAM total y almacenamiento libre del dispositivo, para decidir si el asistente IA on-device
 * puede activarse. No garantiza que la inferencia sea rápida, solo que no debería fallar por
 * falta de memoria o espacio.
 */
data class AssistantDeviceCheck(
    val meetsRequirements: Boolean,
    val totalRamBytes: Long,
    val availableStorageBytes: Long,
)

/**
 * Umbrales de partida para ofrecer el asistente: Gemma 3n E4B necesita del orden de unos GB de
 * RAM viva durante la inferencia, se deja margen sobre eso para el resto de Android + la app.
 * // ponytail: cifras de partida sin validar en un dispositivo real de gama baja — revisar antes
 * // de release (ver plan de implementación, pregunta abierta #3).
 */
const val ASSISTANT_MIN_RAM_BYTES: Long = 6L * 1024 * 1024 * 1024
const val ASSISTANT_MIN_FREE_STORAGE_BYTES: Long = 6L * 1024 * 1024 * 1024

/**
 * Chequeo en frío, sin permisos, de si el dispositivo cumple los requisitos del asistente IA
 * on-device (RAM total + almacenamiento libre en el destino del modelo).
 */
expect class AssistantDeviceChecker {
    fun check(): AssistantDeviceCheck
}
