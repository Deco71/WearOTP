package com.decoapps.wearotp.mobile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.Modifier
import com.decoapps.wearotp.mobile.screens.NavigationStack
import com.decoapps.wearotp.mobile.screens.otp.OTPViewModel
import com.decoapps.wearotp.mobile.theme.AppTheme
import com.decoapps.wearotp.mobile.data.syncData
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import kotlin.getValue
class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private val otpViewModel: OTPViewModel by viewModels()
    private val dataClient by lazy { Wearable.getDataClient(this) }

    val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val authResult = Identity.getAuthorizationClient(this)
                .getAuthorizationResultFromIntent(result.data)

            authResult.accessToken?.let { token ->
                Log.d("DriveAuth", "Authorization successful, token: $token")
            } ?: run {
                Log.e("DriveAuth", "Authorization succeeded but no token received")
            }
        } else {
            Log.e("DriveAuth", "User cancelled or authorization failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            AppTheme() {
                NavigationStack(Modifier, authorizationLauncher)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(this)
        otpViewModel.loadTokensFromDirectory(this)
        Wearable.getDataClient(this)
            .dataItems
            .addOnSuccessListener { dataItemBuffer ->
                val uri = dataItemBuffer.firstOrNull { it.uri.path?.startsWith("/initial-sync") == true }?.uri
                dataItemBuffer.release()
                if (uri != null) handleInitialSync(uri)
            }
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            Log.d("WATCH_CONNECTION", "Received data change event: ${event.type} for URI: ${event.dataItem.uri}")
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path?.startsWith("/initial-sync") == true
            ) {
                handleInitialSync(event.dataItem.uri)
            }
        }
    }

    private fun handleInitialSync(uri: Uri) {
        Log.d("WATCH_CONNECTION", "Wear app requested initial sync — starting sync")
        syncData(this)
        Wearable.getDataClient(this).deleteDataItems(uri)
            .addOnSuccessListener {
                Log.d("OTPViewModel", "Successfully removed data item with URI: $uri")
            }
            .addOnFailureListener {
                Log.e("OTPViewModel", "Failed to remove data item with URI: $uri, error: ${it.message}")
            }

    }
}