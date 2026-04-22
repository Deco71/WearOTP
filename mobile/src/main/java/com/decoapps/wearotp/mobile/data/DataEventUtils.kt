package com.decoapps.wearotp.mobile.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.decoapps.wearotp.mobile.R
import com.decoapps.wearotp.shared.crypto.TokenFileManager
import com.decoapps.wearotp.shared.crypto.rsaEncrypt
import com.decoapps.wearotp.shared.data.OTPService
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

//We must do this everytime since the wear app might be uninstalled at any time, in that case we must wait for the new public key to be published
fun fetchWearPublicKey(context: Context, onResult: (PublicKey?) -> Unit) {
    Wearable.getDataClient(context)
        .dataItems
        .addOnSuccessListener { dataItemBuffer ->
            var publicKey: PublicKey? = null
            dataItemBuffer.forEach { dataItem ->
                if (dataItem.uri.path?.startsWith("/public-key") == true) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val publicKeyBase64 = dataMap.getString("publicKey")
                        if (publicKeyBase64 != null) {
                            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
                            val keyFactory = KeyFactory.getInstance("RSA")
                            publicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
                        }
                    } catch (e: Exception) {
                        Log.e("DataEventUtils", "Failed to parse wear public key: ${e.message}")
                    }
                }
            }
            dataItemBuffer.release()
            onResult(publicKey)
        }
        .addOnFailureListener {
            Log.e("DataEventUtils", "Failed to fetch wear public key: ${it.message}")
            onResult(null)
        }
}

fun sendToWearable(context: Context, service: OTPService) {
    fetchWearPublicKey(context) { publicKey ->
        if (publicKey == null) {
            Log.e("DataEventUtils", "Cannot send token: wearable public key not available. The wear app must be running and connected.")
            return@fetchWearPublicKey
        }

        try {
            val encoder = Base64.getEncoder()
            val putDataMapReq = PutDataMapRequest.create("/create-token/${service.id}").apply {
                // non-sensitive fields
                dataMap.putString("issuer", service.issuer ?: "")
                dataMap.putString("accountName", service.accountName ?: "")
                dataMap.putLong("timestamp", service.lastUpdate)

                // sensitive fields
                dataMap.putString("secret", encoder.encodeToString(rsaEncrypt(service.secret, publicKey)))
                dataMap.putString("algorithm", encoder.encodeToString(rsaEncrypt(service.algorithm, publicKey)))
                dataMap.putString("digits", encoder.encodeToString(rsaEncrypt(service.digits.toString(), publicKey)))
                dataMap.putString("interval", encoder.encodeToString(rsaEncrypt(service.interval.toString(), publicKey)))
            }
            val request = putDataMapReq.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request)
                .addOnSuccessListener {
                    Log.d("DataEventUtils", "Successfully sent encrypted token to wearable: ${service.id}")
                }
                .addOnFailureListener {
                    Log.e("DataEventUtils", "Failed to send token to wearable: ${it.message}")
                }
        } catch (e: Exception) {
            Log.e("DataEventUtils", "Error encrypting or sending token: ${e.message}")
        }
    }
}

fun removeToWearable(context: Context, tokenId: String) {
    try {
        val putDataMapReq = PutDataMapRequest.create("/delete-token/${tokenId}").apply {
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        val request = putDataMapReq.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d("DataEventUtils", "Successfully sent delete request to wearable for token: $tokenId")
            }
            .addOnFailureListener {
                Log.e("DataEventUtils", "Failed to send delete request to wearable for token $tokenId: ${it.message}")
            }
    } catch (e: Exception) {
        Log.e("DataEventUtils", "Error sending delete request to wearable: ${e.message}")
    }
}

fun syncData(context: Context) {
    val tokenFileManager = TokenFileManager()

    fetchWearPublicKey(context) { publicKey ->
        if (publicKey == null) {
            Log.e(
                "DataEventUtils",
                "Cannot sync: wearable public key not available."
            )
            Toast.makeText(context,
                context.getString(R.string.sync_failed_open_wear),
                Toast.LENGTH_SHORT).show()
            return@fetchWearPublicKey
        }
        val loadedServices = tokenFileManager.loadEncryptedTokens(TokenFileManager.getTokensDirectory(context.filesDir))
        try {
            val putDataMapReq = PutDataMapRequest.create("/sync").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putStringArray("allTokenIds", loadedServices.map { it.id }.toTypedArray())
            }
            val request = putDataMapReq.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request)
                .addOnSuccessListener {
                    Log.d("DataEventUtils", "Successfully sent sync list to wearable")
                    for (service in loadedServices) {
                        sendToWearable(context, service)
                    }
                    Toast.makeText(context,
                        context.getString(R.string.sync_successful),
                        Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.e("DataEventUtils", "Failed to send sync list to wearable: ${it.message}")
                    Toast.makeText(context,
                        context.getString(R.string.sync_failed_generic),
                        Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("DataEventUtils", "Error sending message: ${e.message}")
        }
    }
}
