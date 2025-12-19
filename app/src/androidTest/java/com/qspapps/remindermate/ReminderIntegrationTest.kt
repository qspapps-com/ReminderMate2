package com.qspapps.remindermate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ReminderIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

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
