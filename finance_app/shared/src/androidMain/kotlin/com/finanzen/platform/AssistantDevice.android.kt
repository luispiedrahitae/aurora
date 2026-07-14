package com.finanzen.platform

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs

/**
 * MediaPipe LLM Inference para modelos tipo Gemma tiene un piso práctico de Android 12 (API 31);
 * se combina con el umbral de RAM/almacenamiento de [ASSISTANT_MIN_RAM_BYTES].
 * // ponytail: piso de SDK de partida, a revisar contra la documentación vigente de MediaPipe al
 * // momento de implementar la Fase 2 real.
 */
actual class AssistantDeviceChecker(private val context: Context) {
    actual fun check(): AssistantDeviceCheck {
        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val availableStorage = StatFs(context.filesDir.path).availableBytes
        val sdkOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val meets = sdkOk && totalRam >= ASSISTANT_MIN_RAM_BYTES && availableStorage >= ASSISTANT_MIN_FREE_STORAGE_BYTES
        return AssistantDeviceCheck(meets, totalRam, availableStorage)
    }
}
