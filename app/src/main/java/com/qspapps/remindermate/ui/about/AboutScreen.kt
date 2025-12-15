package com.qspapps.remindermate.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.qspapps.remindermate.BuildConfig
import com.qspapps.remindermate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.about)) },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Reminder Mate 2.0",)
            Text(text = "Version: ${BuildConfig.VERSION_NAME}",)
            Text(text = "Made in India with love by Naveen Belkale.")
            Text(text = "\nReminder Mate is a simple productivity app to help users manage reminders. It allows users to add reminders at specific time and get reminded through notifications. It provides an easy way to snooze reminders or mark completed through notifications itself. ")
            Text(text = "\nPlease leave a review in Play Store if you have any suggestions.")
            Text(text = "\nWebsite: https://www.qspapps.com/")
        }
    }
}
