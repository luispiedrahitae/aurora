package com.finanzen.security

/** SHA-256 hex. Implementación por plataforma (java.security en JVM/Android, CommonCrypto en iOS). */
expect fun sha256Hex(input: String): String

/** Bytes aleatorios criptográficamente seguros (SecureRandom en JVM/Android, SecRandomCopyBytes en iOS). */
expect fun secureRandomBytes(size: Int): ByteArray

/**
 * Encadena [iterations] hashes SHA-256 partiendo de `sha256Hex(seed)`, donde cada vuelta re-hashea la
 * representación hexadecimal ASCII de la vuelta anterior (mismo resultado que llamar a [sha256Hex] en
 * bucle). Implementación por plataforma para reutilizar el motor de hash en vez de recrearlo en cada
 * una de las iteraciones — ver comentario en [PinHasher].
 */
expect fun sha256HexIterated(seed: String, iterations: Int): String

/**
 * Hash de PIN: SHA-256 iterado con sal aleatoria segura.
 * ponytail: el techo de seguridad es la entropía del PIN (4–8 dígitos), no el KDF — ningún número de
 * iteraciones resiste un ataque offline a 10^4–10^8 combinaciones. Las iteraciones solo encarecen cada
 * intento; la sal segura evita rainbow tables entre instalaciones. La biometría/FDE del SO es la defensa real.
 */
object PinHasher {
    private const val ITERATIONS = 200_000

    fun hash(pin: String, salt: String): String = sha256HexIterated(salt + pin, ITERATIONS)

    fun verify(pin: String, salt: String, expectedHash: String): Boolean = hash(pin, salt) == expectedHash

    /** Genera una sal de 32 bytes en hex con RNG criptográfico. */
    fun newSalt(): String = secureRandomBytes(32).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
