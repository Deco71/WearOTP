package com.decoapps.wearotp.wear.screens.card

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.decoapps.wearotp.wear.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.decoapps.wearotp.shared.data.OTPService
import com.decoapps.wearotp.shared.viewmodels.OTPCardViewModel

@Composable
fun OTPCard(
    service: OTPService,
    modifier: Modifier = Modifier,
    viewModel: OTPCardViewModel = viewModel(key = service.id, factory = OTPCardViewModel.factory(service))
) {
    val timeProgress by viewModel.timeProgress.collectAsState()
    val token by viewModel.token.collectAsState()
    val animatedProgress = remember { Animatable(timeProgress) }
    val isFirstFrame = remember { mutableStateOf(true) }
    val lastAnimatedValue = remember { mutableFloatStateOf(timeProgress) }

    val timeSkip = ((100/(service.interval)) * 0.01).toFloat()
    val animationTime = 900

    val totp = viewModel.formatToken(viewModel.formatToken(token))

    LaunchedEffect(timeProgress) {
        Log.d("OTPCard", "Time Progress: $timeProgress, Last Animated Value: ${lastAnimatedValue.floatValue}, Is First Frame: ${isFirstFrame.value}")
        if (isFirstFrame.value) {
            animatedProgress.snapTo(timeProgress)
            isFirstFrame.value = false
        } else if(lastAnimatedValue.floatValue < timeProgress) {
            animatedProgress.animateTo(
                targetValue = timeProgress,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing))
        }
        else {
            animatedProgress.animateTo(
                targetValue = timeProgress - timeSkip,
                animationSpec = tween(durationMillis = animationTime, easing = LinearEasing)
            )
        }
        lastAnimatedValue.floatValue = timeProgress
    }

    Card(
        onClick = { },
        modifier = modifier,
        enabled = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = stringResource(id = R.string.service_icon_content_description),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = service.issuer ?: stringResource(id = R.string.unknown_service),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = totp,
                    style = if (totp.length > 7) {MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )} else {MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )},
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
