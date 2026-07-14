package com.finanzen.platform

// ponytail: stub iOS. Conectar a NSProcessInfo.physicalMemory + NSFileManager cuando lleguemos
// al track iOS del asistente.
actual class AssistantDeviceChecker {
    actual fun check(): AssistantDeviceCheck {
        println("[ASSISTANT-iOS stub] chequeo de dispositivo no implementado")
        return AssistantDeviceCheck(meetsRequirements = false, totalRamBytes = 0, availableStorageBytes = 0)
    }
}
