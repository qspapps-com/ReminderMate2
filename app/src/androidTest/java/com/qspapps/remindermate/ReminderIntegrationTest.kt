package com.qspapps.remindermate

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.rule.GrantPermissionRule
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.utils.DateTimeUtils
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import javax.inject.Inject

@HiltAndroidTest
class ReminderIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Add this rule to automatically grant notification permission
    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: ReminderRepository

    @Before
    fun init() {
        hiltRule.inject()
        // Clear database before each test to prevent state leakage
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun createAndEditReminderTest() {
        val title = "Test Reminder"
        val description = "Test Description"
        val updatedTitle = "Updated Test Reminder"
        val moreOptions = composeTestRule.activity.getString(R.string.more_options)

        // 1. Create a new reminder
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_reminder))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_label))
            .performTextInput(title)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.description_label))
            .performTextInput(description)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.save_button))
            .performClick()

        // Verify it exists on home screen
        composeTestRule.onNodeWithText(title).assertIsDisplayed()

        // 2. Update existing reminder
        composeTestRule.onNodeWithTag("reminder_item_$title")
            .onChildren()
            .filterToOne(hasContentDescription(moreOptions))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.update_menu_item))
            .performClick()

        // Clear and update title
        composeTestRule.onNodeWithText(title)
            .performTextReplacement(updatedTitle)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.save_button))
            .performClick()

        // Verify updated title exists
        composeTestRule.onNodeWithText(updatedTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
    }

    @Test
    fun createReminderAndSnoozeTest() {
        val title = "Snooze Test"
        val moreOptions = composeTestRule.activity.getString(R.string.more_options)

        // 1. Create a new reminder
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_reminder))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_label))
            .performTextInput(title)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.save_button))
            .performClick()

        // 2. Snooze for 15 mins
        composeTestRule.onNodeWithTag("reminder_item_$title")
            .onChildren()
            .filterToOne(hasContentDescription(moreOptions))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.snooze_15_minutes_menu_item))
            .performClick()

        // 3. Verify that the displayed time is updated
        // Wait for potential UI updates and verify the snoozed time
        val snoozedTime = LocalDateTime.now().plusMinutes(15)
        val expectedTime = DateTimeUtils.formatTime(snoozedTime)

        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag("reminder_item_$title")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Example of an alternative that usually works on merged trees:
        composeTestRule.onNodeWithTag("reminder_item_$title")
            .assertTextContains(expectedTime)
    }

    @Test
    fun createWeeklyRecurringReminderAndVerifyStorageTest() {
        val title = "Weekly Task"
        val description = "Every week on this day"
        val interval = "2"
        val count = "5"

        // 1. Create a new weekly reminder
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_reminder))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_label))
            .performTextInput(title)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.description_label))
            .performTextInput(description)

        // Open Repeats dropdown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.repeats_label))
            .performClick()

        // Select Weekly
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.repeat_option_weekly))
            .performClick()

        // Set Interval
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.every_label))
            .performTextReplacement(interval)

        // Set Count
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.number_of_times_label))
            .performTextInput(count)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.save_button))
            .performClick()

        // 2. Verify it appears today
        composeTestRule.onNodeWithText(title).assertIsDisplayed()

        // 3. Swipe to tomorrow
        composeTestRule.onNode(hasScrollAction()).performTouchInput {
            swipeLeft()
        }

        // 4. Verify it does NOT appear tomorrow
        composeTestRule.onNodeWithText(title).assertDoesNotExist()

        // 5. Verify repository storage: Check all fields are populated correctly
        runBlocking {
            val allReminders = repository.getAllReminders().first()
            val savedReminder = allReminders.find { it.title == title }
            assertNotNull("Reminder should be saved in repository", savedReminder)
            assertEquals("Title mismatch", title, savedReminder?.title)
            assertEquals("Description mismatch", description, savedReminder?.description)
            assertNotNull(savedReminder?.recurrence)
            assertEquals(Frequency.WEEKLY, savedReminder?.recurrence?.frequency)
            assertEquals("Interval mismatch", interval.toInt(), savedReminder?.recurrence?.interval)
            assertEquals("Count mismatch", count.toInt(), savedReminder?.recurrence?.count)
            // Verify startDateTime is roughly now (ignoring seconds/nanos)
            val now = LocalDateTime.now()
            assertEquals("Start date mismatch", now.toLocalDate(), savedReminder?.startDateTime?.toLocalDate())
        }
    }

    @Test
    fun createReminderAndMarkCompletedTest() {
        val title = "Complete Me"

        // 1. Create a new reminder
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_reminder))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_label))
            .performTextInput(title)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.save_button))
            .performClick()

        // 2. Mark as completed using the checkbox in the list item
        composeTestRule.onNodeWithTag("reminder_item_$title")
            .onChildren()
            .filterToOne(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .performClick()

        // 3. Verify it's marked as completed
        composeTestRule.onNodeWithTag("reminder_item_$title")
            .onChildren()
            .filterToOne(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOn()
    }

    @Test
    fun createDailyRecurringReminderTest() {
        val title = "Daily Reminder"

        // 1. Create a new daily reminder
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_reminder))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_label))
            .performTextInput(title)

        // Open Repeats dropdown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.repeats_label))
            .performClick()

        // Select Daily
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.repeat_option_daily))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.save_button))
            .performClick()

        // 2. Verify it appears today
        composeTestRule.onNodeWithText(title).assertIsDisplayed()

        // 3. Swipe to tomorrow
        composeTestRule.onNode(hasScrollAction()).performTouchInput {
            swipeLeft()
        }

        // 4. Verify it appears tomorrow
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun createReminderAndCheckInAllRemindersTest() {
        val title = "All Reminders Test"

        // 1. Create a new reminder
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_reminder))
            .performClick()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_label))
            .performTextInput(title)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.save_button))
            .performClick()

        // 2. Navigate to All Reminders
        openNavigationMenu()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.all_reminders_title))
            .performClick()

        // 3. Verify it exists on All Reminders screen
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun checkOverdueReminderTest() {
        val overdueTitle = "Overdue Task"

        // 1. Insert an overdue reminder directly into the repository
        runBlocking {
            repository.insert(
                Reminder(
                    title = overdueTitle,
                    description = "Should be in overdue screen",
                    startDateTime = LocalDateTime.now().minusDays(1)
                )
            )
        }

        // 2. Navigate to Overdue Reminders
        openNavigationMenu()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.overdue_reminders_title))
            .performClick()

        // 3. Verify it exists on Overdue Reminders screen
        composeTestRule.onNodeWithText(overdueTitle).assertIsDisplayed()
    }

    @Test
    fun navigateToAllRemindersAndBackTest() {
        openNavigationMenu()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.all_reminders_title))
            .performClick()

        // Verify All Reminders screen is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.all_reminders_screen_title))
            .assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.back_button_content_description))
            .performClick()

        // Verify Home screen is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.today_reminders))
            .assertIsDisplayed()
    }

    @Test
    fun navigateToOverdueRemindersAndBackTest() {
        openNavigationMenu()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.overdue_reminders_title))
            .performClick()

        // Verify Overdue Reminders screen is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.overdue_reminders_screen_title))
            .assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.back_button_content_description))
            .performClick()

        // Verify Home screen is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.today_reminders))
            .assertIsDisplayed()
    }

    @Test
    fun navigateToSettingsAndToggleThemeTest() {
        openNavigationMenu()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.settings_title))
            .performClick()

        // Verify Settings screen is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.settings_screen_title))
            .assertIsDisplayed()

        // Click on Theme setting
        composeTestRule.onNodeWithTag("setting_theme").performClick()

        // Verify Theme dialog is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.choose_theme_dialog_title))
            .assertIsDisplayed()

        // Select Dark theme
        composeTestRule.onNodeWithTag("theme_option_DARK").performClick()

        // Verify dialog is closed and theme text is updated
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.theme_dark))
            .assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.back_button_content_description))
            .performClick()
    }

    @Test
    fun navigateToAboutScreenTest() {
        openNavigationMenu()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.about))
            .performClick()

        // Verify About screen is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.about))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Reminder Mate 2.0").assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.back_button_content_description))
            .performClick()
    }

    private fun openNavigationMenu() {
        composeTestRule.onNodeWithTag("home_more_options")
            .performClick()
    }
}
