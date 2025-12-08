package com.qspapps.remindermate.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showRestoreDialog by remember { mutableStateOf<Uri?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                viewModel.backupReminders(uri)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                showRestoreDialog = uri
            }
        }
    }

    if (showRestoreDialog != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("Restore Reminders") },
            text = { Text("Do you want to delete all existing reminders before restoring?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog?.let { viewModel.restoreReminders(it, true) }
                    showRestoreDialog = null
                }) { Text("Delete and Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog?.let { viewModel.restoreReminders(it, false) }
                    showRestoreDialog = null
                }) { Text("Keep Existing") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Section 1: Data Management
            item {
                SettingsSectionTitle("Data Management")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup Data",
                    subtitle = "Save your data locally",
                    onClick = {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                            putExtra(Intent.EXTRA_TITLE, "reminders.json")
                        }
                        backupLauncher.launch(intent)
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.History,
                    title = "Restore Data",
                    subtitle = "Restore from a previous backup",
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                        }
                        restoreLauncher.launch(intent)
                    }
                )
            }
        }
    }
}

// --- Reusable Custom Composables ---

@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * A standard clickable settings item (Navigation or Action)
 */
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else null,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    )
}

/**
 * A settings item with a toggle switch
 */
@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        // Using Modifier.clickable on the row allows clicking anywhere to toggle
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}