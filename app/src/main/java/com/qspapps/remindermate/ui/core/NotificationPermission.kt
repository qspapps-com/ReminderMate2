package com.qspapps.remindermate.ui.core

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.qspapps.remindermate.R

/**
 * Requests POST_NOTIFICATIONS once per process, showing the rationale dialog when Android says
 * one is warranted. A no-op below API 33, where the permission is granted at install time.
 */
@Composable
fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val deniedMessage = stringResource(id = R.string.notifications_disabled_toast)
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, deniedMessage, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val activity = context as? Activity
        when {
            granted -> Unit
            activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
            ) == true -> showRationale = true

            else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(id = R.string.permission_required_title)) },
            text = { Text(stringResource(id = R.string.notification_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(id = R.string.permission_accept)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    Toast.makeText(context, deniedMessage, Toast.LENGTH_LONG).show()
                }) { Text(stringResource(id = R.string.permission_decline)) }
            }
        )
    }
}
