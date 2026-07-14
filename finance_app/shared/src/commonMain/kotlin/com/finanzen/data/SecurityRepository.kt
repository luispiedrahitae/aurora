package com.finanzen.data

import com.finanzen.db.FinanzenDb
import com.finanzen.security.PinHasher

class SecurityRepository(private val db: FinanzenDb) {

    fun isLockEnabled(): Boolean = read(KEY_ENABLED) == "true"

    fun hasPin(): Boolean = read(KEY_PIN_HASH) != null

    fun enableLockWithPin(pin: String) {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        db.transaction {
            db.settingQueries.put(KEY_ENABLED, "true")
            db.settingQueries.put(KEY_SALT, salt)
            db.settingQueries.put(KEY_PIN_HASH, hash)
            clearAttempts()
        }
    }

    fun disableLock() {
        db.transaction {
            db.settingQueries.delete(KEY_ENABLED)
            db.settingQueries.delete(KEY_SALT)
            db.settingQueries.delete(KEY_PIN_HASH)
            clearAttempts()
        }
    }

    fun lockedUntilMs(): Long = read(KEY_LOCKED_UNTIL)?.toLongOrNull() ?: 0L

    /**
     * Verifica el PIN aplicando bloqueo por intentos fallidos: cada [MAX_ATTEMPTS] intentos
     * incorrectos seguidos disparan una espera que se duplica en cada ciclo (backoff exponencial),
     * hasta un techo de [MAX_LOCKOUT_SECONDS]. [nowMs] es inyectable para tests.
     */
    fun verifyPin(pin: String, nowMs: Long): Boolean {
        if (nowMs < lockedUntilMs()) return false
        val salt = read(KEY_SALT) ?: return false
        val expected = read(KEY_PIN_HASH) ?: return false
        val ok = PinHasher.verify(pin, salt, expected)
        if (ok) {
            clearAttempts()
        } else {
            registerFailedAttempt(nowMs)
        }
        return ok
    }

    private fun registerFailedAttempt(nowMs: Long) {
        val failCount = (read(KEY_FAIL_COUNT)?.toIntOrNull() ?: 0) + 1
        if (failCount < MAX_ATTEMPTS) {
            db.settingQueries.put(KEY_FAIL_COUNT, failCount.toString())
            return
        }
        val stage = (read(KEY_LOCKOUT_STAGE)?.toIntOrNull() ?: 0) + 1
        val waitSeconds = (BASE_LOCKOUT_SECONDS * (1L shl (stage - 1).coerceAtMost(10))).coerceAtMost(MAX_LOCKOUT_SECONDS)
        db.transaction {
            db.settingQueries.put(KEY_FAIL_COUNT, "0")
            db.settingQueries.put(KEY_LOCKOUT_STAGE, stage.toString())
            db.settingQueries.put(KEY_LOCKED_UNTIL, (nowMs + waitSeconds * 1000).toString())
        }
    }

    private fun clearAttempts() {
        db.settingQueries.delete(KEY_FAIL_COUNT)
        db.settingQueries.delete(KEY_LOCKOUT_STAGE)
        db.settingQueries.delete(KEY_LOCKED_UNTIL)
    }

    private fun read(key: String): String? = db.settingQueries.get(key).executeAsOneOrNull()

    companion object {
        const val KEY_ENABLED = "lock.enabled"
        const val KEY_SALT = "lock.salt"
        const val KEY_PIN_HASH = "lock.pin_hash"
        const val KEY_FAIL_COUNT = "lock.fail_count"
        const val KEY_LOCKOUT_STAGE = "lock.lockout_stage"
        const val KEY_LOCKED_UNTIL = "lock.locked_until_ms"
        const val MAX_ATTEMPTS = 5
        const val BASE_LOCKOUT_SECONDS = 30L
        const val MAX_LOCKOUT_SECONDS = 30L * 60
    }
}
