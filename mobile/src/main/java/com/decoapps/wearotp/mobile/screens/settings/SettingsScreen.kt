package com.decoapps.wearotp.mobile.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.decoapps.wearotp.mobile.R
import com.decoapps.wearotp.mobile.data.PreferencesViewModel
import com.decoapps.wearotp.mobile.data.syncData
import com.decoapps.wearotp.mobile.drive.login
import com.decoapps.wearotp.mobile.theme.ColorMode
import kotlinx.coroutines.launch
import com.decoapps.wearotp.mobile.BuildConfig
import com.decoapps.wearotp.mobile.drive.DriveServiceHelper
import com.decoapps.wearotp.mobile.drive.DriveServiceHelper.Companion.getDriveService
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, authorizationLauncher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>) {
    val preferencesViewModel: PreferencesViewModel =
        viewModel(factory = PreferencesViewModel.Factory)
    val currentColorMode = preferencesViewModel.currentColorMode.collectAsState().value
    val uriHandler = LocalUriHandler.current
    var versionName : String? = null
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

    try {
        versionName = context.packageManager
            .getPackageInfo(context.packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                ThemeOption(
                    label = "System default",
                    description = "Follows your device's light/dark setting",
                    selected = currentColorMode == ColorMode.SYSTEM.toString(),
                    onClick = { preferencesViewModel.saveColorMode(ColorMode.SYSTEM) }
                )
                ThemeOption(
                    label = "Light",
                    description = "Always use light theme",
                    selected = currentColorMode == ColorMode.LIGHT.toString(),
                    onClick = { preferencesViewModel.saveColorMode(ColorMode.LIGHT) }
                )
                ThemeOption(
                    label = "Dark",
                    description = "Always use dark theme",
                    selected = currentColorMode == ColorMode.DARK.toString(),
                    onClick = { preferencesViewModel.saveColorMode(ColorMode.DARK) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ThemeOption(
                        label = "Dynamic colors",
                        description = "Use colors from your wallpaper (Android 12+)",
                        selected = currentColorMode == ColorMode.DYNAMIC.toString(),
                        onClick = { preferencesViewModel.saveColorMode(ColorMode.DYNAMIC) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Syncing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { syncData(context) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sync now",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Fix problems by syncing your data manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                val accessToken = login(webClientId, context, authorizationLauncher)
                                if (accessToken != null) {
                                    /*createDriveFile(
                                        context,
                                        accessToken,
                                        "This is a test file created by WatchOTP to verify Google Drive integration."
                                    )*/
                                    listAppDataFiles(accessToken)
                                }
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.github_invertocat_black_clearspace), // TODO Sostituire con icona Google Drive se disponibile
                        contentDescription = "Google Drive",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Accedi a Google Drive",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Effettua il login per sincronizzare con Google Drive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/Deco71/WatchOTP") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.github_invertocat_black_clearspace),
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "View on GitHub",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "We are open source! Check us on GitHub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()

                Spacer(modifier = Modifier.height(24.dp))

                if (versionName != null) {
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

suspend fun createDriveFile(context: Context, accessToken: String, content: String) {
    try {
        val driveService = getDriveService(accessToken)
        val helper = DriveServiceHelper(driveService)

        // Create a temporary file with the specified content TODO remove this
        val testFile = File(context.filesDir, "test_file.txt")

        testFile.writeText(content)
        helper.createTextFile(testFile)
    } catch (e: Exception) {
        Log.e("DRIVE_FILE_CREATION", "Failed during Drive file creation", e)
    }
}

suspend fun listAppDataFiles(accessToken: String) {
    try {
        val driveService = getDriveService(accessToken)
        val helper = DriveServiceHelper(driveService)
        val files = helper.listAppDataFiles()
        Log.d("DRIVE_FILE_LISTING", "Files in appDataFolder: ${files.joinToString { it.name }}")
    } catch (e: Exception) {
        Log.e("DRIVE_FILE_LISTING", "Failed during listing app data files", e)
    }
}

