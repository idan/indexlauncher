package app.indexlauncher.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.PageCounts
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomePagerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var expanded by mutableStateOf(false)
    private var count by mutableIntStateOf(2)
    private var rtl by mutableStateOf(false)

    @Test fun rightwardRowDragPagesLeftAndLeftwardWidgetDragReturnsToApps() {
        showHome()
        compose.onNodeWithText("App state 0").assertIsDisplayed()
        compose.onNodeWithTag("app-row").performTouchInput { swipeRight() }
        compose.onNodeWithText("Widget 1").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.add_favorite)).assertDoesNotExist()
        compose.onNodeWithTag("widget-1").performTouchInput { swipeLeft() }
        compose.onNodeWithText("App state 0").assertIsDisplayed()
    }

    @Test fun leftwardRowDragRevealsStarWithoutPagingAndRightwardDragClosesIt() {
        showHome()
        val star = compose.activity.getString(R.string.add_favorite)
        compose.onNodeWithTag("app-row").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription(star).assertIsDisplayed()
        compose.onNodeWithText("Widget 1").assertDoesNotExist()
        compose.onNodeWithTag("app-row").performTouchInput { swipeRight() }
        compose.onNodeWithContentDescription(star).assertDoesNotExist()
        compose.onNodeWithText("App state 0").assertIsDisplayed()
    }

    @Test fun unfoldingPinsSameAppStateAndFoldingRemembersLeftWidgetSelection() {
        requireWideWindow()
        showHome()
        compose.onNodeWithText("Increment").performClick()
        compose.onNodeWithTag("query").performTextInput("preserved")
        compose.runOnIdle { expanded = true }
        compose.onNodeWithText("App state 1").assertIsDisplayed()
        compose.onNodeWithText("preserved").assertIsDisplayed()
        compose.onNodeWithText("Widget 1").assertIsDisplayed()
        val appBounds = compose.onNodeWithText("App state 1").fetchSemanticsNode().boundsInRoot
        val widgetBounds = compose.onNodeWithText("Widget 1").fetchSemanticsNode().boundsInRoot
        assertTrue(appBounds.left > widgetBounds.right)
        compose.onNodeWithTag("widget-1").performTouchInput { swipeRight() }
        compose.onNodeWithText("Widget 2").assertIsDisplayed()
        compose.onNodeWithText("App state 1").assertIsDisplayed()
        compose.runOnIdle { expanded = false }
        compose.onNodeWithText("App state 1").assertIsDisplayed()
        compose.onNodeWithText("preserved").assertIsDisplayed()
        compose.runOnIdle { expanded = true }
        compose.onNodeWithText("Widget 2").assertIsDisplayed()
    }

    @Test fun changingCountPreservesLogicalPageAndClampsHiddenPages() {
        showHome()
        compose.onNodeWithTag("app-row").performTouchInput { swipeRight() }
        compose.onNodeWithTag("widget-1").performTouchInput { swipeRight() }
        compose.onNodeWithText("Widget 2").assertIsDisplayed()
        compose.runOnIdle { count = 3 }
        compose.onNodeWithText("Widget 2").assertIsDisplayed()
        compose.runOnIdle { count = 1 }
        compose.onNodeWithText("Widget 1").assertIsDisplayed()
        compose.runOnIdle { count = 0 }
        compose.onNodeWithText("App state 0").assertIsDisplayed()
    }

    @Test fun emptyWideWorkspaceDoesNotCreateAWidgetPage() {
        requireWideWindow()
        expanded = true
        count = 0
        showHome()
        compose.onNodeWithText(compose.activity.getString(R.string.foldable_no_widget_pages)).assertIsDisplayed()
        compose.onNodeWithText("App state 0").assertIsDisplayed()
    }

    @Test fun appStateRestoresAcrossRecreationAndThenMovesBetweenPanes() {
        requireWideWindow()
        expanded = true
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TestHome() }
        compose.onNodeWithText("Increment").performClick()
        compose.onNodeWithTag("query").performTextInput("restored")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("App state 1").assertIsDisplayed()
        compose.onNodeWithText("restored").assertIsDisplayed()
        compose.runOnIdle { expanded = false }
        compose.onNodeWithText("App state 1").assertIsDisplayed()
        compose.onNodeWithText("restored").assertIsDisplayed()
    }

    @Test fun physicalRowAndPagingDirectionsStayTheSameInRtl() {
        rtl = true
        showHome()
        compose.onNodeWithTag("app-row").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.add_favorite)).assertIsDisplayed()
        compose.onNodeWithTag("app-row").performTouchInput { swipeRight() }
        compose.onNodeWithTag("app-row").performTouchInput { swipeRight() }
        compose.onNodeWithText("Widget 1").assertIsDisplayed()
    }

    @Test fun widgetPageLocalStateSurvivesPaneChangesAndPageCountChanges() {
        requireWideWindow()
        showHome()
        compose.onNodeWithTag("app-row").performTouchInput { swipeRight() }
        compose.onNodeWithText("Edit widget 1").performClick()
        compose.onNodeWithText("Widget state 1").assertIsDisplayed()
        compose.runOnIdle { expanded = true }
        compose.onNodeWithText("Widget 1").assertIsDisplayed()
        compose.onNodeWithText("Widget state 1").assertIsDisplayed()
        compose.runOnIdle { count = 3 }
        compose.onNodeWithText("Widget 1").assertIsDisplayed()
        compose.onNodeWithText("Widget state 1").assertIsDisplayed()
    }

    private fun requireWideWindow() {
        assumeTrue("Two-pane tests need an unfolded/wide emulator", compose.activity.resources.configuration.screenWidthDp >= 760)
    }

    private fun showHome() = compose.setContent { TestHome() }

    @Composable
    private fun TestHome() {
        IndexLauncherTheme {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.width(if (expanded) 760.dp else 360.dp).height(650.dp)) {
                    HomePager(
                        counts = PageCounts(count),
                        appPage = { _, _, _, _ ->
                            var value by rememberSaveable { mutableIntStateOf(0) }
                            var query by rememberSaveable { mutableStateOf("") }
                            var open by rememberSaveable { mutableStateOf(false) }
                            Column(Modifier.fillMaxSize()) {
                                Text("App state $value")
                                Button(onClick = { value++ }) { Text("Increment") }
                                OutlinedTextField(query, { query = it }, modifier = Modifier.testTag("query"))
                                Box(Modifier.testTag("app-row")) {
                                    AppRow(
                                        app = AppEntry("test.app", "Main", "Test app"),
                                        catalogVersion = 0,
                                        iconShape = IconShape.DeviceDefault,
                                        loadIcon = { _, _, _ -> null },
                                        isFavorite = false,
                                        showLabelShadow = false,
                                        isOpen = open,
                                        onReveal = { open = it != null },
                                        onToggleFavorite = {},
                                        onClick = {},
                                    )
                                }
                            }
                        },
                        widgetPage = { page, _ ->
                            var edits by rememberSaveable { mutableIntStateOf(0) }
                            Column(Modifier.fillMaxSize().testTag("widget-${page.ordinal}")) {
                                Text("Widget ${page.ordinal}")
                                Text("Widget state $edits")
                                Button(onClick = { edits++ }) { Text("Edit widget ${page.ordinal}") }
                            }
                        },
                    )
                }
            }
        }
    }
}
