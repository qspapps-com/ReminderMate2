
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.repository.Theme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showRestoreDialog by remember { mutableStateOf<Uri?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            title = { Text(stringResource(id = R.string.restore_reminders_dialog_title)) },
            text = { Text(stringResource(id = R.string.restore_reminders_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog?.let { viewModel.restoreReminders(it, true) }
                    showRestoreDialog = null
                }) { Text(stringResource(id = R.string.delete_and_restore_button)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog?.let { viewModel.restoreReminders(it, false) }
                    showRestoreDialog = null
                }) { Text(stringResource(id = R.string.keep_existing_button)) }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(id = R.string.clear_all_reminders_dialog_title)) },
            text = { Text(stringResource(id = R.string.clear_all_reminders_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllReminders()
                    showClearAllDialog = false
                }) { Text(stringResource(id = R.string.clear_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text(stringResource(id = R.string.cancel_button)) }
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.theme,
            onThemeSelected = { viewModel.updateTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button_content_description)
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
            item {
                SettingsSectionTitle(stringResource(id = R.string.appearance_section_title))
            }
            item {
                SettingsItem(
                    icon = if (uiState.theme == Theme.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = stringResource(id = R.string.theme_setting_title),
                    subtitle = stringResource(id = uiState.theme.toStringResource()),
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.VisibilityOff,
                    title = stringResource(id = R.string.hide_completed_reminders_setting_title),
                    checked = uiState.hideCompleted,
                    onCheckedChange = { viewModel.updateHideCompleted(it) }
                )
            }
            item { HorizontalDivider() }
            item {
                SettingsSectionTitle(stringResource(id = R.string.data_management_section_title))
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = stringResource(id = R.string.backup_data_setting_title),
                    subtitle = stringResource(id = R.string.backup_data_setting_subtitle),
                    onClick = {
                        val currentDateTime = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
                        val formattedDateTime = currentDateTime.format(formatter)
                        val fileName = "remindermate_${formattedDateTime}.json.gz"

                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/gzip"
                            putExtra(Intent.EXTRA_TITLE, fileName)
                        }
                        backupLauncher.launch(intent)
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.History,
                    title = stringResource(id = R.string.restore_data_setting_title),
                    subtitle = stringResource(id = R.string.restore_data_setting_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/gzip"
                        }
                        restoreLauncher.launch(intent)
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = stringResource(id = R.string.clear_all_reminders_setting_title),
                    subtitle = stringResource(id = R.string.clear_all_reminders_setting_subtitle),
                    onClick = { showClearAllDialog = true }
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: Theme,
    onThemeSelected: (Theme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.choose_theme_dialog_title)) },
        text = {
            Column {
                Theme.entries.forEach { theme ->
                    ListItem(
                        headlineContent = { Text(stringResource(id = theme.toStringResource())) },
                        modifier = Modifier.clickable { onThemeSelected(theme); onDismiss() },
                        leadingContent = {
                            Icon(
                                imageVector = when (theme) {
                                    Theme.LIGHT -> Icons.Default.LightMode
                                    Theme.DARK -> Icons.Default.DarkMode
                                    Theme.SYSTEM -> Icons.Default.DarkMode // Choose an appropriate icon
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel_button))
            }
        }
    )
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

@Composable
private fun Theme.toStringResource(): Int {
    return when (this) {
        Theme.LIGHT -> R.string.theme_light
        Theme.DARK -> R.string.theme_dark
        Theme.SYSTEM -> R.string.theme_system
    }
}
