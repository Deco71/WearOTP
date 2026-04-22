package com.decoapps.wearotp.wear.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.decoapps.wearotp.shared.crypto.TokenFileManager
import com.decoapps.wearotp.shared.crypto.rsaDecrypt
import com.decoapps.wearotp.shared.data.OTPService
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.io.File
import java.util.Base64
import com.decoapps.wearotp.shared.crypto.getRSAKeys

fun elaborateDataItem(dataItem: DataItem, tokensDir: File) : Long? {

    val tokenFileManager = TokenFileManager()
    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

    var last_sync : Long? = null

    when {
        dataItem.uri.path?.startsWith("/sync") == true -> {

            val ids = dataMap.getStringArray("allTokenIds")

            if (ids != null) {
                tokensDir.listFiles()?.forEach { file ->
                    if (!ids.contains(file.name)) {
                        tokenFileManager.deleteToken(tokensDir, file.name)
                    }
                }
            }

            last_sync = dataMap.getLong("timestamp")

        }

        dataItem.uri.path?.startsWith("/create-token") == true -> {

            val id = dataItem.uri.pathSegments.lastOrNull() ?: return last_sync

            val keyPair = getRSAKeys()
            val privateKey = keyPair?.private
            val secret: String
            val algorithm: String
            val digits: Int
            val interval: Int

            if (privateKey != null) {
                try {
                    val secretBytes = Base64.getDecoder().decode(dataMap.getString("secret")!!)
                    val algorithmBytes = Base64.getDecoder().decode(dataMap.getString("algorithm")!!)
                    val digitsBytes = Base64.getDecoder().decode(dataMap.getString("digits")!!)
                    val intervalBytes = Base64.getDecoder().decode(dataMap.getString("interval")!!)

                    secret = rsaDecrypt(secretBytes, privateKey)
                    algorithm = rsaDecrypt(algorithmBytes, privateKey)
                    digits = rsaDecrypt(digitsBytes, privateKey).toInt()
                    interval = rsaDecrypt(intervalBytes, privateKey).toInt()
                } catch (e: Exception) {
                    Log.e("DataEventUtils", "RSA decryption failed: ${e.message}")
                    return null
                }
            } else {
                Log.w("DataEventUtils", "Received token data but local RSA key unavailable, skipping.")
                return null
            }

            last_sync = dataMap.getLong("timestamp")

            val newService = OTPService(
                id = id,
                issuer = dataMap.getString("issuer"),
                accountName = dataMap.getString("accountName"),
                secret = secret,
                algorithm = algorithm,
                digits = digits,
                interval = interval,
                lastUpdate = last_sync
            )
            if (!tokenFileManager.saveEncryptedToken(tokensDir, newService)) return null
        }

        dataItem.uri.path?.startsWith("/delete-token") == true -> {

            val id = dataItem.uri.pathSegments.lastOrNull() ?: return null

            last_sync = dataMap.getLong("timestamp")

            tokenFileManager.deleteToken(tokensDir, id)
        }
    }

    return last_sync
}

fun publishPublicKey(context: Context, publicKeyBase64: String) {
    try {

        val putDataMapReq = PutDataMapRequest.create("/public-key").apply {
            dataMap.putString("publicKey", publicKeyBase64)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        val request = putDataMapReq.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d("DataEventUtils", "Public key published to Wearable Data Layer")
            }
            .addOnFailureListener {
                Log.e("DataEventUtils", "Failed to publish public key: ${it.message}")
            }

        val initialSyncPutDataMapReq = PutDataMapRequest.create("/initial-sync").apply {
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        val initialSyncRequest = initialSyncPutDataMapReq.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(initialSyncRequest)
            .addOnSuccessListener {
                Log.d("DataEventUtils", "Public key published to Wearable Data Layer")
            }
            .addOnFailureListener {
                Log.e("DataEventUtils", "Failed to publish public key: ${it.message}")
            }
    } catch (e: Exception) {
        Log.e("DataEventUtils", "Error publishing public key: ${e.message}")
    }
}

fun removePendingDataItem(context: Context, uri: Uri) {
    Wearable.getDataClient(context).deleteDataItems(uri)
        .addOnSuccessListener {
            Log.d("OTPViewModel", "Successfully removed data item with URI: $uri")
        }
        .addOnFailureListener {
            Log.e("OTPViewModel", "Failed to remove data item with URI: $uri, error: ${it.message}")
        }
}
