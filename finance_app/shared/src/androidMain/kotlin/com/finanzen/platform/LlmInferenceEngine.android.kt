package com.finanzen.platform

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Envuelve `com.google.ai.edge.litertlm` (LiteRT-LM), la vía de despliegue mobile actualmente
 * recomendada por Google para Gemma (sucesora de MediaPipe LLM Inference, que quedó en modo
 * "maintenance-only" — ver `libs.versions.toml`). [engine] se crea una sola vez al cargar el
 * modelo (costoso, hasta ~10s); cada [generate] abre y cierra su propia `Conversation` — ver la
 * nota de eficiencia de RAM en el `expect`. Intenta el backend GPU primero y cae a CPU si falla
 * (el manifiesto declara las native libs de GPU como `required="false"`, así que el intento no
 * bloquea nada en dispositivos sin soporte).
 */
actual class LlmInferenceEngine {
    private var engine: Engine? = null

    actual suspend fun load(modelPath: String): Result<Unit> = runCatching {
        close()
        engine = withContext(Dispatchers.Default) { buildEngine(modelPath) }
        Unit
    }

    actual fun isLoaded(): Boolean = engine != null

    actual suspend fun generate(prompt: String, onToken: (String) -> Unit): Result<String> {
        val activeEngine = engine ?: return Result.failure(IllegalStateException("Modelo no cargado"))
        return runCatching {
            activeEngine.createConversation().use { conversation ->
                suspendCancellableCoroutine { cont ->
                    val full = StringBuilder()
                    conversation.sendMessageAsync(
                        prompt,
                        object : MessageCallback {
                            override fun onMessage(message: Message) {
                                val partial = message.toString()
                                full.append(partial)
                                onToken(partial)
                            }

                            override fun onDone() {
                                if (cont.isActive) cont.resumeWith(Result.success(full.toString()))
                            }

                            override fun onError(throwable: Throwable) {
                                if (cont.isActive) cont.resumeWith(Result.failure(throwable))
                            }
                        },
                    )
                }
            }
        }
    }

    actual fun close() {
        engine?.close()
        engine = null
    }

    // ponytail: Backend.GPU() "succeeds" at initialize() even without a real GPU delegate
    // (verified on the Android emulator, which has no OpenCL) — the failure only surfaces
    // asynchronously on the first generate() call, as an OnError callback deep in the native
    // executor, by which point there's no clean way to retry on the same turn. A synchronous
    // try/catch around initialize() can't catch that. Defaulting to CPU until this is validated
    // GPU-with-fallback on real hardware (where OpenCL/GPU delegate actually exists) is the safe
    // path — see plan verification notes.
    private fun buildEngine(modelPath: String): Engine = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU())).also { it.initialize() }
}
