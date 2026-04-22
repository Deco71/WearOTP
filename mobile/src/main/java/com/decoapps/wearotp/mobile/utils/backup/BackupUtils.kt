package com.decoapps.wearotp.mobile.utils.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.decoapps.wearotp.mobile.R
import com.decoapps.wearotp.mobile.data.syncData
import com.decoapps.wearotp.shared.crypto.CryptoManager
import com.decoapps.wearotp.shared.crypto.TokenFileManager
import com.decoapps.wearotp.shared.data.OTPService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun createBackup(context: Context, uri: Uri, userKey: String) {
    val tokensDir = TokenFileManager.getTokensDirectory(context.filesDir)
    val tokenFileManager = TokenFileManager()
    val cryptoManager = CryptoManager()

    try {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            tokenFileManager.loadEncryptedTokens(tokensDir).forEach { service ->
                val jsonString = TokenFileManager.json.encodeToString(service)
                val jsonBytes = jsonString.toByteArray()
                Log.d("BackupUtils", "Adding file to zip: ${service.id}")
                zos.putNextEntry(ZipEntry(service.id))
                zos.write(jsonBytes)
                zos.closeEntry()
            }
        }
        val cleartextZipBytes = baos.toByteArray()

        context.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { fos ->
                val encryptCipher = cryptoManager.getEncryptBackupCipher(fos, userKey)
                val ciphertextBytes = encryptCipher.doFinal(cleartextZipBytes)
                fos.write(ciphertextBytes)
            }
        }
        Toast.makeText(context, context.getString(R.string.backup_created_successfully), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("BackupUtils", "Error creating backup", e)
        Toast.makeText(context, context.getString(R.string.backup_creation_failed), Toast.LENGTH_SHORT).show()
    }
}

fun restoreBackup(context: Context, uri: Uri, userKey: String) {
    val tokensDir = TokenFileManager.getTokensDirectory(context.filesDir)
    val tokenFileManager = TokenFileManager()
    val cryptoManager = CryptoManager()
    try {

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val salt = ByteArray(CryptoManager.SALT_LENGTH)
            if (inputStream.read(salt) != CryptoManager.SALT_LENGTH) throw Exception("Invalid backup file: missing salt")

            val nonce = ByteArray(CryptoManager.GCM_NONCE_SIZE)
            if (inputStream.read(nonce) != CryptoManager.GCM_NONCE_SIZE) throw Exception("Invalid backup file: missing nonce")

            val decryptCipher = cryptoManager.getDecryptBackupCipher(userKey, salt, nonce)

            Log.d("BackupUtils", "Starting to read backup zip")
            val ciphertextBytes = inputStream.readBytes()
            
            // Decrypt all file in memory using doFinal() to prevent AEAD Tag Swallowing
            val cleartextZipBytes = decryptCipher.doFinal(ciphertextBytes)

            if (tokensDir.exists()) {
                tokensDir.listFiles()?.forEach { it.delete() }
            } else {
                tokensDir.mkdirs()
            }

            ZipInputStream(ByteArrayInputStream(cleartextZipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val jsonBytes = zis.readBytes()
                    val jsonString = String(jsonBytes)
                    val service = TokenFileManager.json.decodeFromString<OTPService>(jsonString)
                    tokenFileManager.saveEncryptedToken(tokensDir, service)
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        Toast.makeText(context, context.getString(R.string.backup_restored_successfully), Toast.LENGTH_SHORT).show()
        syncData(context)
    } catch (e: Exception) {
        Log.e("BackupUtils", "Error restoring backup", e)
        Toast.makeText(context, context.getString(R.string.backup_restore_failed), Toast.LENGTH_SHORT).show()
    }
}
