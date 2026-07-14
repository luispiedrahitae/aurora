package com.finanzen.platform

// ponytail: stub Desktop. El preview de desarrollo no distribuye ni necesita inferencia real —
// el asistente IA solo corre de verdad en Android (LiteRT-LM).
actual class LlmInferenceEngine {
    actual suspend fun load(modelPath: String): Result<Unit> {
        println("[LLM-Desktop stub] load '$modelPath' no implementado")
        return Result.failure(UnsupportedOperationException("Asistente IA no disponible en el preview de Desktop"))
    }

    actual fun isLoaded(): Boolean = false

    actual suspend fun generate(prompt: String, onToken: (String) -> Unit): Result<String> = Result.failure(UnsupportedOperationException("Asistente IA no disponible en el preview de Desktop"))

    actual fun close() = Unit
}
