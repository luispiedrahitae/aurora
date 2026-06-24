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
        }
    }

    fun disableLock() {
        db.transaction {
            db.settingQueries.delete(KEY_ENABLED)
            db.settingQueries.delete(KEY_SALT)
            db.settingQueries.delete(KEY_PIN_HASH)
        }
    }

    fun verifyPin(pin: String): Boolean {
        val salt = read(KEY_SALT) ?: return false
        val expected = read(KEY_PIN_HASH) ?: return false
        return PinHasher.verify(pin, salt, expected)
    }

    private fun read(key: String): String? = db.settingQueries.get(key).executeAsOneOrNull()

    companion object {
        const val KEY_ENABLED = "lock.enabled"
        const val KEY_SALT = "lock.salt"
        const val KEY_PIN_HASH = "lock.pin_hash"
    }
}
