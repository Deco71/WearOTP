package com.decoapps.wearotp.shared.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

const val ITERATIONS = 6
const val HASH_LENGTH = 32 //bytes
const val PARALLELISM = 1
const val MEMORY_COST = 65536 //64 MB
const val SALT_LENGTH = CryptoManager.SALT_LENGTH

fun deriveKeyFromPassword(password: ByteBuffer,
                          salt: ByteBuffer,
                          iterations: Int = ITERATIONS,
                          hashLength: Int = HASH_LENGTH,
                          parallelism: Int = PARALLELISM,
                          memoryCost: Int = MEMORY_COST): SecretKey {

    val argon2Kt = Argon2Kt()

    val hashResult = argon2Kt.hash(
        mode = Argon2Mode.ARGON2_ID,
        password = password,
        tCostInIterations = iterations,
        mCostInKibibyte = memoryCost,
        salt = salt,
        hashLengthInBytes = hashLength,
        parallelism = parallelism
    )
    val hashArray = hashResult.rawHashAsByteArray()

    return SecretKeySpec(hashArray, CryptoManager.ALGORITHM)
}

fun generateSalt(): ByteArray {
    val salt = ByteArray(SALT_LENGTH)
    SecureRandom().nextBytes(salt)
    return salt
}