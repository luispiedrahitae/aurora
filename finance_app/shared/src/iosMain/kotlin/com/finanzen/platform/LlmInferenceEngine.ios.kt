package com.finanzen.platform

// ponytail: stub iOS. Conectar a LiteRT-LM iOS cuando lleguemos al track iOS del asistente.
actual class LlmInferenceEngine {
    actual suspend fun load(modelPath: String): Result<Unit> {
        println("[LLM-iOS stub] load '$modelPath' no implementado")
        return Result.failure(UnsupportedOperationException("Asistente IA no disponible todavía en iOS"))
    }

    actual fun isLoaded(): Boolean = false

    actual suspend fun generate(prompt: String, onToken: (String) -> Unit): Result<String> = Result.failure(UnsupportedOperationException("Asistente IA no disponible todavía en iOS"))

    actual fun close() = Unit
}
