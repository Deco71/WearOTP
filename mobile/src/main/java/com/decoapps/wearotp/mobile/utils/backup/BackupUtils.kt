package com.decoapps.wearotp.mobile.utils.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.decoapps.wearotp.mobile.data.syncData
import com.decoapps.wearotp.shared.crypto.CryptoManager
import com.decoapps.wearotp.shared.crypto.TokenFileManager
import com.decoapps.wearotp.shared.data.OTPService
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.CipherOutputStream
import javax.crypto.CipherInputStream

fun createBackup(context: Context, uri: Uri, userKey: String) {
    val tokensDir = TokenFileManager.getTokensDirectory(context.filesDir)
    val tokenFileManager = TokenFileManager()
    val cryptoManager = CryptoManager()

    try {
        context.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { fos ->

                val encryptCipher = cryptoManager.getEncryptBackupCipher(fos, userKey)
                CipherOutputStream(fos, encryptCipher).use { cos ->
                    ZipOutputStream(cos).use { zos ->
                        tokenFileManager.loadEncryptedTokens(tokensDir).forEach { service ->
                            val jsonString = TokenFileManager.json.encodeToString(service)
                            val jsonBytes = jsonString.toByteArray()
                            Log.d("BackupUtils", "Adding file to zip: ${service.id}")
                            zos.putNextEntry(ZipEntry(service.id))
                            zos.write(jsonBytes)
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
        Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        Log.e("BackupUtils", "Error creating backup", e)
        Toast.makeText(context, "Failed to create backup", Toast.LENGTH_SHORT).show()
    }
}

fun restoreBackup(context: Context, uri: Uri, userKey: String) {
    val tokensDir = TokenFileManager.getTokensDirectory(context.filesDir)
    val tokenFileManager = TokenFileManager()
    val cryptoManager = CryptoManager()
    try {
        if (tokensDir.exists()) {
            tokensDir.listFiles()?.forEach { it.delete() }
        } else {
            tokensDir.mkdirs()
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val salt = ByteArray(CryptoManager.SALT_LENGTH)
            inputStream.read(salt)

            val nonce = ByteArray(CryptoManager.GCM_NONCE_SIZE)
            inputStream.read(nonce)

            val decryptCipher = cryptoManager.getDecryptBackupCipher(userKey, salt, nonce)

            Log.d("BackupUtils", "Starting to read backup zip")

            CipherInputStream(inputStream, decryptCipher).use { cis ->
                ZipInputStream(cis).use { zis ->
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
        }
        Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show()
        syncData(context)
    } catch (e: Exception) { // Catch generic exception as crypto errors can occur
        Log.e("BackupUtils", "Error restoring backup", e)
        Toast.makeText(context, "Failed to restore backup, check your password", Toast.LENGTH_SHORT).show()
    }
}
