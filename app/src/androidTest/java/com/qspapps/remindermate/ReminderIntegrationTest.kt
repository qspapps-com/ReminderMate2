package com.qspapps.remindermate

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import javax.inject.Inject

@HiltAndroidTest
class ReminderIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: ReminderRepository

    @Before
    fun init() {
        hiltRule.inject()
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
        // Find the checkbox within the reminder item with our title
        composeTestRule.onNodeWithTag("reminder_item_$title")
            .onChildren()
            .filterToOne(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .performClick()

        // 3. Verify it's marked as completed (Checkbox should be checked)
        composeTestRule.onNodeWithTag("reminder_item_$title")
            .onChildren()
            .filterToOne(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOn()
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.theme_setting_title))
            .performClick()

        // Verify Theme dialog is shown
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.choose_theme_dialog_title))
            .assertIsDisplayed()

        // Select Dark theme
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.theme_dark))
            .performClick()

        // Verify dialog is closed and theme text is updated (this assumes the UI updates immediately)
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
