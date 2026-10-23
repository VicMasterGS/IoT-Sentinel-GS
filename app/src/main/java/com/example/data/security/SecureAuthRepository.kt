package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class UserSession(
    val username: String,
    val sessionToken: String,
    val loginTimestamp: Long
)

sealed class AuthResult {
    data class Success(val userSession: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Secure authentication repository.
 * Stores user hashes (PBKDF2-HMAC-SHA256 + 32-byte salt) and session tokens
 * encrypted with AES-GCM via Android KeyStore at rest.
 * Plaintext passwords are never stored in memory or persistence.
 */
class SecureAuthRepository(context: Context) {
    private val authPrefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "secure_auth_prefs",
        Context.MODE_PRIVATE
    )
    private val sessionPrefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "secure_session",
        Context.MODE_PRIVATE
    )

    suspend fun register(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.length < 3) {
            return@withContext AuthResult.Error("El usuario debe tener al menos 3 caracteres")
        }
        if (password.length < 6) {
            return@withContext AuthResult.Error("La contraseña debe tener al menos 6 caracteres")
        }

        val existingRecord = authPrefs.getString("user_${cleanUser.lowercase()}", null)
        if (existingRecord != null) {
            return@withContext AuthResult.Error("El usuario ya existe")
        }

        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPassword(password.toCharArray(), salt)

        val recordJson = JSONObject().apply {
            put("username", cleanUser)
            put("salt", CryptoManager.toBase64(salt))
            put("hash", CryptoManager.toBase64(hash))
            put("createdAt", System.currentTimeMillis())
        }

        val encryptedPayload = CryptoManager.encryptAtRest(recordJson.toString())
        authPrefs.edit()
            .putString("user_${cleanUser.lowercase()}", encryptedPayload)
            .apply()

        // Create initial session
        val session = createSession(cleanUser)
        AuthResult.Success(session)
    }

    companion object {
        // Fixed dummy salt for constant-time rejection of non-existent users
        private val DUMMY_SALT = ByteArray(32) { (it xor 0x5A).toByte() }
    }

    suspend fun login(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val encryptedPayload = authPrefs.getString("user_${cleanUser.lowercase()}", null)
        if (encryptedPayload == null) {
            // Mitigate timing side-channels by performing PBKDF2 calculation even if user does not exist
            CryptoManager.hashPassword(password.toCharArray(), DUMMY_SALT)
            return@withContext AuthResult.Error("Credenciales inválidas")
        }

        // Support backward-compatible transparent migration for legacy unencrypted JSON records
        val decryptedJsonString = CryptoManager.decryptAtRest(encryptedPayload)
            ?: if (encryptedPayload.trimStart().startsWith("{")) encryptedPayload else null

        if (decryptedJsonString == null) {
            CryptoManager.hashPassword(password.toCharArray(), DUMMY_SALT)
            return@withContext AuthResult.Error("Credenciales inválidas")
        }

        try {
            val recordJson = JSONObject(decryptedJsonString)
            val salt = CryptoManager.fromBase64(recordJson.getString("salt"))
            val expectedHash = CryptoManager.fromBase64(recordJson.getString("hash"))

            val isValid = CryptoManager.verifyPassword(password.toCharArray(), salt, expectedHash)
            if (!isValid) {
                return@withContext AuthResult.Error("Credenciales inválidas")
            }

            // Transparently re-encrypt with Android KeyStore if legacy unencrypted payload was read
            if (!encryptedPayload.contains(":")) {
                val reEncrypted = CryptoManager.encryptAtRest(recordJson.toString())
                authPrefs.edit().putString("user_${cleanUser.lowercase()}", reEncrypted).apply()
            }

            val session = createSession(cleanUser)
            AuthResult.Success(session)
        } catch (e: Exception) {
            AuthResult.Error("Credenciales inválidas")
        }
    }

    private fun createSession(username: String): UserSession {
        val sessionToken = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val sessionJson = JSONObject().apply {
            put("username", username)
            put("sessionToken", sessionToken)
            put("timestamp", timestamp)
        }
        val encryptedSession = CryptoManager.encryptAtRest(sessionJson.toString())
        sessionPrefs.edit().putString("active_session", encryptedSession).apply()
        return UserSession(username, sessionToken, timestamp)
    }

    suspend fun getActiveSession(): UserSession? = withContext(Dispatchers.IO) {
        val encryptedSession = sessionPrefs.getString("active_session", null) ?: return@withContext null
        val decrypted = CryptoManager.decryptAtRest(encryptedSession) ?: return@withContext null
        return@withContext try {
            val json = JSONObject(decrypted)
            UserSession(
                username = json.getString("username"),
                sessionToken = json.getString("sessionToken"),
                loginTimestamp = json.getLong("timestamp")
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionPrefs.edit().remove("active_session").apply()
    }
}
