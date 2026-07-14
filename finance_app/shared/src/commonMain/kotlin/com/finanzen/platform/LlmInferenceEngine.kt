package com.finanzen.platform

/**
 * Motor de inferencia del asistente (LiteRT-LM en Android). Sin historial
 * de turnos en el motor mismo — cada [generate] crea y cierra su propia sesión con el prompt
 * completo ya ensamblado (system prompt + contexto financiero + pregunta, ver
 * `AssistantViewModel`), para acotar el crecimiento del KV-cache en un modelo de este tamaño.
 * Llamar [close] al salir de la pantalla del asistente o al pasar la app a segundo plano.
 */
expect class LlmInferenceEngine {
    suspend fun load(modelPath: String): Result<Unit>
    fun isLoaded(): Boolean
    suspend fun generate(prompt: String, onToken: (String) -> Unit): Result<String>
    fun close()
}
