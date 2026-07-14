package com.finanzen.platform

// Desktop es preview de desarrollo — no se distribuye y el motor de inferencia real solo existe
// en Android, así que no gateamos por RAM/almacenamiento aquí.
actual class AssistantDeviceChecker {
    actual fun check(): AssistantDeviceCheck = AssistantDeviceCheck(
        meetsRequirements = true,
        totalRamBytes = ASSISTANT_MIN_RAM_BYTES,
        availableStorageBytes = ASSISTANT_MIN_FREE_STORAGE_BYTES,
    )
}
