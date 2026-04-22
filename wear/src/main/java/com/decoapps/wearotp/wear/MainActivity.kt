package com.decoapps.wearotp.wear

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.decoapps.wearotp.shared.crypto.TokenFileManager
import com.decoapps.wearotp.shared.crypto.getRSAKeys
import com.decoapps.wearotp.wear.data.PreferencesViewModel
import com.decoapps.wearotp.wear.data.elaborateDataItem
import com.decoapps.wearotp.wear.data.publishPublicKey
import com.decoapps.wearotp.wear.data.removePendingDataItem
import com.decoapps.wearotp.wear.screens.home.OTPList
import com.decoapps.wearotp.wear.screens.home.OTPViewModel
import com.decoapps.wearotp.wear.theme.AppTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import java.util.Base64

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private val otpViewModel: OTPViewModel by viewModels()

    private val dataClient by lazy { Wearable.getDataClient(this) }

    private val preferencesViewModel: PreferencesViewModel by viewModels { PreferencesViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val keysReady = mutableStateOf(false)

        lifecycleScope.launch {
            getRSAKeys() { generatedKeyPair ->
                val encoder = Base64.getEncoder()
                val publicKeyBase64 = encoder.encodeToString(generatedKeyPair.public.encoded)

                // Publish public key to dataStore to make it available for the mobile app
                publishPublicKey(this@MainActivity, publicKeyBase64)
                keysReady.value = true
            }
        }

        setContent {
            AppTheme() {
                val ready by keysReady
                if (!ready) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        OTPList(
                            Modifier
                        )
                    }
                }
            }
        }
    }

    private fun processDataItems(items: Iterable<DataItem>) {
        var uriToRemove: List<Uri> = emptyList()
        var lastSync: Long? = null

        val sortedItems = items
            .map { it to DataMapItem.fromDataItem(it).dataMap.getLong("timestamp", 0L) }
            .sortedBy { it.second }
            .map { it.first }

        sortedItems.forEach { dataItem ->
            val successfulSyncTime = elaborateDataItem(dataItem, TokenFileManager.getTokensDirectory(filesDir))
            if (successfulSyncTime != null && dataItem.uri.path?.startsWith("/public-key") == false) {
                uriToRemove = uriToRemove + dataItem.uri
            }
            if (successfulSyncTime != null) {
                if (lastSync == null || successfulSyncTime > lastSync) {
                    lastSync = successfulSyncTime
                }
            }
        }

        Log.d("WATCH_CONNECTION", "Elaborated Uris to remove: ${uriToRemove.joinToString(", ")}")
        for (uri in uriToRemove) {
            removePendingDataItem(this@MainActivity, uri)
        }

        otpViewModel.loadTokensFromDirectory(this@MainActivity)

        val currentLastSync = preferencesViewModel.currentLastSync.value
        if (lastSync != null && (currentLastSync == null || lastSync > currentLastSync)) preferencesViewModel.saveLastSync(lastSync)
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(this)

        lifecycleScope.launch {
            Wearable.getDataClient(this@MainActivity)
                .dataItems
                .addOnSuccessListener { dataItemBuffer ->
                    lifecycleScope.launch {
                        processDataItems(dataItemBuffer)
                        dataItemBuffer.release()
                    }
                }
                .addOnFailureListener {
                    Log.e("WATCH_CONNECTION", "Failed to query data items: ${it.message}")
                }
        }
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("WATCH_CONNECTION", "Querying data changes")

        // Collect all events before async operation
        val events = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .map { it.dataItem.freeze() }
        dataEvents.release()

        lifecycleScope.launch {
            processDataItems(events)
        }
    }
}