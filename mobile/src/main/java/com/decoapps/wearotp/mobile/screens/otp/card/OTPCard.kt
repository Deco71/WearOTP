package com.decoapps.wearotp.mobile.screens.otp.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.decoapps.wearotp.mobile.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.decoapps.wearotp.shared.data.OTPService
import com.decoapps.wearotp.shared.viewmodels.OTPCardViewModel
import com.decoapps.wearotp.shared.viewmodels.ProgressColorLevel

@Composable
fun OTPCard(
    service: OTPService,
    modifier: Modifier = Modifier,
    onDelete: ((OTPService) -> Unit)? = null,
    viewModel: OTPCardViewModel = viewModel(key = service.id, factory = OTPCardViewModel.factory(service))
) {
    val context = LocalContext.current
    val timeProgress by viewModel.timeProgress.collectAsState()
    val progressColorLevel by viewModel.progressColorLevel.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val deleteDialogServiceName by viewModel.deleteDialogServiceName.collectAsState()

    val token by viewModel.token.collectAsState()

    val animatedProgress = remember { Animatable(timeProgress) }
    val isFirstFrame = remember { mutableStateOf(true) }
    val lastAnimatedValue = remember { mutableFloatStateOf(timeProgress) }

    val timeSkip = ((100/(service.interval)) * 0.01).toFloat()
    val animationTime = 900

    LaunchedEffect(timeProgress) {
        if (isFirstFrame.value) {
            animatedProgress.snapTo(timeProgress)
            isFirstFrame.value = false
        } else if(lastAnimatedValue.floatValue < timeProgress) {
            animatedProgress.animateTo(
                targetValue = timeProgress,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing))
            animatedProgress.animateTo(
                targetValue = (timeProgress - timeSkip),
                animationSpec = tween(durationMillis = animationTime, easing = LinearEasing)
            )
        }
        else {
            animatedProgress.animateTo(
                targetValue = timeProgress - timeSkip,
                animationSpec = tween(durationMillis = animationTime, easing = LinearEasing)
            )
        }
        lastAnimatedValue.floatValue = timeProgress
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteDismissed() },
            title = { Text(stringResource(id = R.string.remove_service_title)) },
            text = { Text(stringResource(id = R.string.remove_service_confirm, deleteDialogServiceName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeleteConfirmed(service, onDelete) }) {
                    Text(stringResource(id = R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDeleteDismissed() }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Card(
        modifier = modifier.combinedClickable(
            onClick = { viewModel.onCardClick(context) },
            onLongClick = { viewModel.onCardLongClick() }
        ),
        shape = CardDefaults.shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round,
                    color = when (progressColorLevel) {
                        ProgressColorLevel.CRITICAL -> MaterialTheme.colorScheme.error
                        ProgressColorLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                        ProgressColorLevel.NORMAL -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = stringResource(id = R.string.service_icon_content_description),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = service.issuer ?: stringResource(id = R.string.unknown_service),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (service.accountName?.isEmpty() == false) {
                        Text(
                            text = ("(" + service.accountName + ")"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = viewModel.formatToken(token),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
