package com.decoapps.wearotp.wear.screens.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.ScrollIndicatorColors
import androidx.wear.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.decoapps.wearotp.wear.R
import com.decoapps.wearotp.shared.utils.formatLastSync
import com.decoapps.wearotp.wear.data.PreferencesViewModel
import com.decoapps.wearotp.wear.screens.card.OTPCard
@Composable
fun OTPList(modifier: Modifier) {
    val otpViewModel: OTPViewModel =
        viewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)

    val preferencesViewModel : PreferencesViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity,
        factory = PreferencesViewModel.Factory
    )

    val lastSync by preferencesViewModel.currentLastSync.collectAsStateWithLifecycle()
    val otpServices by otpViewModel.otpServices.collectAsStateWithLifecycle()
    val sortedOtpServices = otpServices.sortedBy { it.lastUpdate }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        otpViewModel.loadTokensFromDirectory(context)
    }

    val listState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollIndicator = {
            ScrollIndicator(
                state = listState,
                colors = ScrollIndicatorColors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd),
            )
        },
    ) { padding ->
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            state = listState,
            flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = listState),
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (otpServices.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.no_totp_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    )
                }

                if (lastSync == null) {
                    item {
                                Text(
                                text = stringResource(id = R.string.open_mobile_once),
                                style = MaterialTheme.typography.bodyExtraSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                            )
                    }
                }
            } else {
                items(sortedOtpServices, key = { it.id }) { service ->
                    OTPCard(
                        service = service,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
            if (lastSync != null) {
                item {
                    Text(
                        text = stringResource(id = R.string.last_sync_format, formatLastSync(lastSync, context)),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }

}