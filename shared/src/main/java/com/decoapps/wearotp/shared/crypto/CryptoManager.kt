package com.decoapps.wearotp.shared.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    private val keystoreAlias = "secret"

    private val keystore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun newCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)

    private fun getEncryptCipher(key: SecretKey = getKey()): Cipher {
        return newCipher().also { it.init(Cipher.ENCRYPT_MODE, key) }
    }

    private fun getDecryptCipherForNonce(nonce: ByteArray, key: SecretKey = getKey()): Cipher {
        return newCipher().also {
            it.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        }
    }

    private fun getKey(alias: String = keystoreAlias): SecretKey {
        return (keystore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: createKey(alias).also { key ->
                keystore.setEntry(alias, KeyStore.SecretKeyEntry(key), null)
            }
    }

    private fun createKey(alias: String) : SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
        return try {
            keyGenerator.init(createAesKeySpec(alias = alias, strongBoxBacked = true))
            keyGenerator.generateKey()
        } catch (_: Exception) {
            keyGenerator.init(createAesKeySpec(alias = alias, strongBoxBacked = false))
            keyGenerator.generateKey()
        }
    }

    private fun createAesKeySpec(alias: String, strongBoxBacked: Boolean): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setIsStrongBoxBacked(strongBoxBacked)
            .setRandomizedEncryptionRequired(true)
            .build()
    }

    fun getEncryptBackupCipher(fos: FileOutputStream, userKey: String) : Cipher {
        val salt = generateSalt()
        fos.write(salt)

        val userKeyBuffer = ByteBuffer.allocateDirect(userKey.toByteArray().size).put(userKey.toByteArray()).flip() as ByteBuffer
        val saltBuffer = ByteBuffer.allocateDirect(salt.size).put(salt).flip() as ByteBuffer
        val key = deriveKeyFromPassword(userKeyBuffer, saltBuffer)
        val encryptCipher = getEncryptCipher(key)
        val nonce = encryptCipher.iv

        fos.write(nonce)

        return encryptCipher
    }

    fun getDecryptBackupCipher(userKey: String, salt: ByteArray, nonce: ByteArray): Cipher {
        val saltBuffer = ByteBuffer.allocateDirect(salt.size).put(salt).flip() as ByteBuffer
        val userKeyBuffer = ByteBuffer.allocateDirect(userKey.toByteArray().size).put(userKey.toByteArray()).flip() as ByteBuffer
        val key = deriveKeyFromPassword(userKeyBuffer, saltBuffer)
        return getDecryptCipherForNonce(nonce, key)
    }


    fun encrypt(bytes: ByteArray, outputStream: OutputStream): ByteArray {
        val encryptCipher = getEncryptCipher()
        val encryptedBytes = encryptCipher.doFinal(bytes)
        val nonce = encryptCipher.iv

        outputStream.use {
            it.write(nonce)

            it.write(ByteBuffer.allocate(4).putInt(encryptedBytes.size).array())
            it.write(encryptedBytes)
        }
        return encryptedBytes
    }

    fun decrypt(inputStream: InputStream): ByteArray {
        return inputStream.use {
            val nonce = ByteArray(GCM_NONCE_SIZE)
            it.read(nonce)

            val encryptedSizeBytes = ByteArray(4)
            it.read(encryptedSizeBytes)
            val encryptedBytesSize = ByteBuffer.wrap(encryptedSizeBytes).int
            val encryptedBytes = ByteArray(encryptedBytesSize)
            it.read(encryptedBytes)

            getDecryptCipherForNonce(nonce).doFinal(encryptedBytes)
        }
    }

    companion object {
        const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        const val GCM_NONCE_SIZE = 12   // 96-bit nonce standard for GCM
        private const val GCM_TAG_LENGTH = 128 // 128-bit authentication tag
        const val SALT_LENGTH = 16
    }
}
