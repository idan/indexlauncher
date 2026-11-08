package app.indexlauncher.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetEditBarTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var width by mutableStateOf(560.dp)
    private var fontScale by mutableStateOf(1f)
    private var reorders = 0

    @Test fun wideBarShowsReorderCaptionAndOnlyOneAction() {
        show()
        compose.onNodeWithText(compose.activity.getString(R.string.reorder)).assertIsDisplayed()
        assertSingleActions()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.reorder_widget_pages)).performClick()
        assertEquals(1, reorders)
    }

    @Test fun narrowBarFallsBackToIconAndRestoresCaptionWhenWidened() {
        width = 280.dp
        show()
        compose.onNodeWithText(compose.activity.getString(R.string.reorder)).assertDoesNotExist()
        assertSingleActions()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.reorder_widget_pages)).assertIsDisplayed()
        compose.runOnIdle { width = 560.dp }
        compose.onNodeWithText(compose.activity.getString(R.string.reorder)).assertIsDisplayed()
        assertSingleActions()
    }

    @Test fun largeFontsUseIconRatherThanForceTheCaptionOntoAnotherLine() {
        width = 400.dp
        fontScale = 2.5f
        show()
        compose.onNodeWithText(compose.activity.getString(R.string.reorder)).assertDoesNotExist()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.reorder_widget_pages)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.add_widget)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.done)).assertIsDisplayed()
        assertSingleActions()
    }

    @Test fun homeWidgetBarStillHasOnlyAddAndDone() {
        show(reorderable = false)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.reorder_widget_pages)).assertDoesNotExist()
        compose.onNodeWithText(compose.activity.getString(R.string.add_widget)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.done)).assertIsDisplayed()
    }

    private fun show(reorderable: Boolean = true) = compose.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            IndexLauncherTheme {
                Box(Modifier.width(width).height(240.dp)) {
                    WidgetEditBar(
                        onAddWidget = {}, onDone = {},
                        onReorderPages = if (reorderable) { { reorders++ } } else null,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    private fun assertSingleActions() {
        compose.onAllNodesWithText(compose.activity.getString(R.string.add_widget)).assertCountEquals(1)
        compose.onAllNodesWithText(compose.activity.getString(R.string.done)).assertCountEquals(1)
        compose.onAllNodesWithContentDescription(compose.activity.getString(R.string.reorder_widget_pages)).assertCountEquals(1)
    }
}
