package com.decoapps.wearotp.shared.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
private val OAEP_SPEC = OAEPParameterSpec(
    "SHA-256",
    "MGF1",
    MGF1ParameterSpec.SHA1,
    PSource.PSpecified.DEFAULT
)

fun getRSAKeys(onKeyPairGenerated: ((KeyPair) -> Unit)? = null): KeyPair? {
    try {
        val alias = "rsa_alias"
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            if (entry != null) {
                Log.d("RSAUtils", "RSA key pair already exists, retrieving from KeyStore")
                return KeyPair(entry.certificate.publicKey, entry.privateKey)
            }
        }

        if (onKeyPairGenerated == null) {
            Log.e("RSAUtils", "RSA key pair not found in keystore!!!")
            return null
        }

        Log.d("RSAUtils", "Generating new RSA key pair")
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(2048)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setIsStrongBoxBacked(true)
            .build()

        keyPairGenerator.initialize(spec)
        val keyPair = keyPairGenerator.generateKeyPair()
        onKeyPairGenerated(keyPair)
        return keyPair
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun rsaEncrypt(data: String, key: PublicKey): ByteArray {
    val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key, OAEP_SPEC)
    return cipher.doFinal(data.toByteArray(Charsets.UTF_8))
}

fun rsaDecrypt(data: ByteArray, key: PrivateKey): String {
    val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, OAEP_SPEC)
    return String(cipher.doFinal(data), Charsets.UTF_8)
}
