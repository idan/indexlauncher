package app.indexlauncher.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.MainActivity
import app.indexlauncher.R
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
