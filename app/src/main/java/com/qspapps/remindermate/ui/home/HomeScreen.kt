package com.qspapps.remindermate.ui.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.qspapps.remindermate.R
import com.qspapps.remindermate.ui.core.DatePickerDialog
import com.qspapps.remindermate.ui.core.ReminderInstanceItem
import com.qspapps.remindermate.ui.navigation.AppScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val hideCompletedRemindersTooltipState = rememberTooltipState()
    val showCompletedText = if (uiState.showCompleted) stringResource(id = R.string.hide_completed) else stringResource(id = R.string.show_completed)
    val selectDateTooltipState = rememberTooltipState()

    val title = if (uiState.selectedDate.isEqual(LocalDate.now())) {
        stringResource(id = R.string.today_reminders)
    } else {
        uiState.selectedDate.format(DateTimeFormatter.ofPattern(stringResource(id = R.string.home_date_format)))
    }

    val menuItems = listOf(
        AppScreen.AllReminders,
        AppScreen.OverdueReminders,
        AppScreen.Settings,
        AppScreen.About
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip {
                                Text(showCompletedText)
                            }
                        },
                        state = hideCompletedRemindersTooltipState,
                        content = {
                            IconButton(onClick = { viewModel.toggleShowCompleted() }) {
                                Icon(
                                    imageVector = if (uiState.showCompleted) Icons.Filled.Check else Icons.Filled.CheckCircle,
                                    contentDescription = showCompletedText
                                )
                            }
                        }
                    )
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(id = R.string.select_date))
                            }
                        },
                        state = selectDateTooltipState,
                        content = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = stringResource(id = R.string.select_date))
                            }
                        }
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("home_more_options")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            menuItems.forEach { screen ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = screen.displayName!!)) },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(screen.route)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            screen.icon!!,
                                            contentDescription = stringResource(id = screen.displayName!!)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(AppScreen.AddEditReminder.createRoute(0L)) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_reminder))
            }
        }
    ) {
        var swipeAmount = 0f
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .pointerInput(uiState.selectedDate) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeAmount = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeAmount += dragAmount
                        },
                        onDragEnd = {
                            val threshold = 100
                            if (swipeAmount < -threshold) {
                                viewModel.loadRemindersForDay(uiState.selectedDate.plusDays(1))
                            } else if (swipeAmount > threshold) {
                                viewModel.loadRemindersForDay(uiState.selectedDate.minusDays(1))
                            }
                        }
                    )
                }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.reminders) { reminderInstance ->
                        val isOverDue = !reminderInstance.isCompleted &&
                                currentTime.isAfter(reminderInstance.displayTime)
                        ReminderInstanceItem(
                            reminderInstance = reminderInstance,
                            actions = viewModel.getReminderActions(navController),
                            showDate = false,
                            isOverDue,
                            defaultTimes = uiState.defaultTimes
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = uiState.selectedDate,
            onDateSelected = { selectedDate ->
                viewModel.loadRemindersForDay(selectedDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
