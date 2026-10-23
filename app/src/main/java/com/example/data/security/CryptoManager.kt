package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Manages cryptographic operations backed by Android KeyStore.
 * - PBKDF2-HMAC-SHA256 (65,536 iterations, 256-bit key) with SecureRandom salt.
 * - AES-GCM-256 for at-rest credential encryption in Android KeyStore.
 * - Constant-time comparison using MessageDigest.isEqual.
 * - Graceful JVM fallback for local unit tests when AndroidKeyStore is absent.
 */
object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "IoTSentinelMasterKey"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_KEY_LENGTH = 256
    private const val SALT_LENGTH = 32

    private var fallbackKey: SecretKey? = null

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    } catch (_: Exception) {
        null
    }

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        val ks = keyStore
        if (ks != null) {
            try {
                if (!ks.containsAlias(KEY_ALIAS)) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEYSTORE
                    )
                    val spec = KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build()

                    keyGenerator.init(spec)
                    keyGenerator.generateKey()
                }
            } catch (_: Exception) {
                initFallbackKey()
            }
        } else {
            initFallbackKey()
        }
    }

    private fun initFallbackKey() {
        if (fallbackKey == null) {
            val kg = KeyGenerator.getInstance("AES")
            kg.init(256)
            fallbackKey = kg.generateKey()
        }
    }

    private fun getMasterKey(): SecretKey {
        val ks = keyStore
        return if (ks != null) {
            try {
                if (ks.containsAlias(KEY_ALIAS)) {
                    (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
                } else {
                    fallbackKey ?: run { initFallbackKey(); fallbackKey!! }
                }
            } catch (_: Exception) {
                fallbackKey ?: run { initFallbackKey(); fallbackKey!! }
            }
        } else {
            fallbackKey ?: run { initFallbackKey(); fallbackKey!! }
        }
    }

    /**
     * Encrypts plaintext using AES-GCM-256 and the master key.
     * Returns Base64-encoded string in format: "IV:CIPHERTEXT"
     */
    fun encryptAtRest(plainText: String): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return "$ivBase64:$cipherBase64"
    }

    /**
     * Decrypts an "IV:CIPHERTEXT" string using the master key.
     */
    fun decryptAtRest(encryptedCombined: String): String? {
        return try {
            val parts = encryptedCombined.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates a cryptographically secure random 32-byte salt.
     */
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Hashes password using PBKDF2WithHmacSHA256 with 65,536 iterations.
     */
    fun hashPassword(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    /**
     * Secure constant-time comparison of two byte arrays to resist timing attacks.
     */
    fun verifyPassword(password: CharArray, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val calculatedHash = hashPassword(password, salt)
        return MessageDigest.isEqual(calculatedHash, expectedHash)
    }

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun fromBase64(base64: String): ByteArray = Base64.decode(base64, Base64.NO_WRAP)
}
