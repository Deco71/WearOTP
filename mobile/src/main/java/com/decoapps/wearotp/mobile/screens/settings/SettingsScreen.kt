package com.decoapps.wearotp.mobile.screens.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import com.decoapps.wearotp.mobile.utils.backup.createBackup
import com.decoapps.wearotp.mobile.utils.backup.restoreBackup
import com.decoapps.wearotp.mobile.data.syncData
import com.decoapps.wearotp.mobile.theme.ColorMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val preferencesViewModel: PreferencesViewModel =
        viewModel(factory = PreferencesViewModel.Factory)
    val currentColorMode = preferencesViewModel.currentColorMode.collectAsState().value
    val uriHandler = LocalUriHandler.current
    var versionName : String? = null
    val context = LocalContext.current

    // Stato per dialog inserimento chiave
    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Launcher per creare backup
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let {
                val key = keyInput
                createBackup(context, it, key)
            }
            keyInput = ""
        }
    )

    // Launcher per ripristinare backup
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val key = keyInput
                restoreBackup(context, it, key)
            }
            keyInput = ""
        }
    )

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
                HorizontalDivider()

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Backup",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val currentDateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                            val formattedDateTime = currentDateTime.format(formatter)
                            val backupFileName = "wear-otp-backup_$formattedDateTime.zip"
                            pendingAction = {
                                createDocumentLauncher.launch(backupFileName)
                            }
                            showKeyDialog = true
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Backup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Create backup",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Save your accounts to a file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pendingAction = {
                                openDocumentLauncher.launch(arrayOf("application/zip"))
                            }
                            showKeyDialog = true
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Restore backup",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Restore your accounts from a file.",
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

                // Dialog inserimento chiave
                if (showKeyDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showKeyDialog = false
                            keyInput = ""
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showKeyDialog = false
                                    pendingAction?.invoke()
                                }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showKeyDialog = false
                                    keyInput = ""
                                }
                            ) {
                                Text("Annulla")
                            }
                        },
                        title = { Text("Inserisci chiave") },
                        text = {
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it },
                                label = { Text("Chiave di backup") },
                                singleLine = true
                            )
                        }
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