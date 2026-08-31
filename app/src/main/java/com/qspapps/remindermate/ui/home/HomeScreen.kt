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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.qspapps.remindermate.R
import com.qspapps.remindermate.ui.core.DatePickerDialog
import com.qspapps.remindermate.ui.core.ReminderInstanceItem
import com.qspapps.remindermate.ui.navigation.AppScreen
import com.qspapps.remindermate.ui.navigation.homeMenuDestinations
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Horizontal drag distance, in pixels, that moves the view by one day. */
private const val DAY_SWIPE_THRESHOLD = 100f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val showCompletedText = stringResource(
        id = if (uiState.showCompleted) R.string.hide_completed else R.string.show_completed
    )

    // Built once per screen, not once per row: a fresh instance would defeat item skipping.
    val reminderActions = remember(navController) { viewModel.getReminderActions(navController) }

    val datePattern = stringResource(id = R.string.home_date_format)
    val title = if (uiState.selectedDate.isEqual(LocalDate.now())) {
        stringResource(id = R.string.today_reminders)
    } else {
        val formatter = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern) }
        uiState.selectedDate.format(formatter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                actions = {
                    TooltipIconButton(
                        tooltip = showCompletedText,
                        icon = if (uiState.showCompleted) Icons.Filled.Check else Icons.Filled.CheckCircle,
                        onClick = viewModel::toggleShowCompleted
                    )
                    TooltipIconButton(
                        tooltip = stringResource(id = R.string.select_date),
                        icon = Icons.Default.DateRange,
                        onClick = { showDatePicker = true }
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("home_more_options")
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(id = R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            homeMenuDestinations.forEach { destination ->
                                val label = stringResource(id = destination.displayName)
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(destination.screen.route)
                                    },
                                    leadingIcon = {
                                        Icon(destination.icon, contentDescription = label)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(AppScreen.AddEditReminder.createRoute(0L)) }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_reminder))
            }
        }
    ) { innerPadding ->
        var swipeAmount = 0f
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pointerInput(uiState.selectedDate) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeAmount = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeAmount += dragAmount
                        },
                        onDragEnd = {
                            val days = when {
                                swipeAmount < -DAY_SWIPE_THRESHOLD -> 1L
                                swipeAmount > DAY_SWIPE_THRESHOLD -> -1L
                                else -> return@detectHorizontalDragGestures
                            }
                            viewModel.loadRemindersForDay(uiState.selectedDate.plusDays(days))
                        }
                    )
                }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = uiState.reminders,
                        key = { it.reminderId to it.originalTime }
                    ) { reminderInstance ->
                        ReminderInstanceItem(
                            reminderInstance = reminderInstance,
                            actions = reminderActions,
                            showDate = false,
                            isOverdue = !reminderInstance.isCompleted &&
                                uiState.currentTime.isAfter(reminderInstance.displayTime),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    tooltip: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = tooltip)
        }
    }
}
