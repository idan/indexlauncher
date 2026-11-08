package app.indexlauncher.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.MainActivity
import app.indexlauncher.ACTION_REORDER_WIDGET_PAGES
import app.indexlauncher.data.PageSettingsRepository
import app.indexlauncher.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Both system navigation modes deliver the same MAIN/HOME intent to the singleTask activity. */
@RunWith(AndroidJUnit4::class)
class HomeIntentTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeReturnsToFavoritesAndDoesNotReplayAfterRecreation() {
        repeat(2) {
            openIndex()
            sendMainIntent(Intent.CATEGORY_HOME)
            compose.onNodeWithText(compose.activity.getString(R.string.settings_tile_title)).assertDoesNotExist()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.search_apps)).assertDoesNotExist()
        }
        openIndex()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(compose.activity.getString(R.string.settings_tile_title)).assertIsDisplayed()
    }

    @Test fun launcherIntentAndOrdinaryResumePreserveIndex() {
        openIndex()
        sendMainIntent(Intent.CATEGORY_LAUNCHER)
        compose.onNodeWithText(compose.activity.getString(R.string.settings_tile_title)).assertIsDisplayed()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText(compose.activity.getString(R.string.settings_tile_title)).assertIsDisplayed()
    }

    @Test fun settingsOpensSharedOverviewDraftRestoresAndHomeCancels() {
        val prefs = compose.activity.getSharedPreferences("workspace_left", 0)
        val originalCount = prefs.getInt("left_pages", -1)
        val originalOrder = prefs.getString("page_order", null)
        try {
            prefs.edit().putInt("left_pages", 2).remove("page_order").commit()
            openIndex()
            compose.onNodeWithText(compose.activity.getString(R.string.settings_tile_title)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.reorder_widget_pages)).performScrollTo().performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.reorder_widget_pages)).assertIsDisplayed()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.page_move_away, 1)).performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick()
            assertEquals(listOf(2, 1), PageSettingsRepository(compose.activity).counts.value.visibleOrdinals)
            compose.runOnIdle {
                compose.activity.startActivity(Intent(compose.activity, MainActivity::class.java).setAction(ACTION_REORDER_WIDGET_PAGES))
            }
            compose.waitForIdle()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.page_move_toward, 1)).performClick()
            sendMainIntent(Intent.CATEGORY_HOME)
            compose.onNodeWithText(compose.activity.getString(R.string.reorder_widget_pages)).assertDoesNotExist()
            assertEquals(listOf(2, 1), PageSettingsRepository(compose.activity).counts.value.visibleOrdinals)
        } finally {
            prefs.edit().apply {
                if (originalCount == -1) remove("left_pages") else putInt("left_pages", originalCount)
                if (originalOrder == null) remove("page_order") else putString("page_order", originalOrder)
            }.commit()
        }
    }

    private fun openIndex() {
        val tools = compose.activity.getString(R.string.show_launcher_tools)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription(tools).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(tools).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.settings_tile_title)).assertIsDisplayed()
    }

    private fun sendMainIntent(category: String) {
        compose.runOnIdle {
            compose.activity.startActivity(
                Intent(compose.activity, MainActivity::class.java)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        compose.waitForIdle()
    }
}
