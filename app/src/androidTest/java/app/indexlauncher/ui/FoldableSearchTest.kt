package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.PageCounts
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoldableSearchTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realSearchQueryAndResultScrollSurvivePaneChangesAndRecreation() {
        assumeTrue(compose.activity.resources.configuration.screenWidthDp >= 760)
        val closeSearch = compose.activity.getString(R.string.close_search)
        val expanded = mutableStateOf(false)
        val apps = List(40) { AppEntry("test.app$it", "Main", "Favorite $it") }
        val host = AppWidgetHost(compose.activity, 12345)
        val manager = AppWidgetManager.getInstance(compose.activity)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            IndexLauncherTheme {
                Box(Modifier.width(if (expanded.value) 760.dp else 360.dp).height(650.dp)) {
                    HomePager(
                        counts = PageCounts(1),
                        appPage = { suppressRail, active, onRailInteraction, onScrimOpacityChange ->
                            LauncherScreen(
                                state = LauncherUiState(apps = apps, isLoading = false),
                                favoriteIds = apps.map { it.id },
                                homeWidgets = emptyList(),
                                widgetHost = host,
                                widgetManager = manager,
                                suppressRail = suppressRail,
                                active = active,
                                onRailInteraction = onRailInteraction,
                                onScrimOpacityChange = onScrimOpacityChange,
                                onToggleFavorite = {},
                                onAppClick = {},
                                onOpenSettings = {},
                                onLoadIcon = { _, _, _ -> null },
                                iconShape = IconShape.DeviceDefault,
                                onRetry = {},
                                onAddHomeWidget = {},
                                onRemoveHomeWidget = {},
                                onMoveHomeWidget = { _, _ -> },
                                onHomeWidgetWidthChange = { _, _ -> },
                                onHomeWidgetHeightChange = { _, _ -> },
                            )
                        },
                        widgetPage = { _, _ -> Text("Left widgets") },
                    )
                }
            }
        }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.jump_to_letter, "F")).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.search_apps)).performClick()
        compose.onNodeWithContentDescription(closeSearch).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("Favorite")
        compose.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText("Favorite 25"))
        compose.onNodeWithText("Favorite 25").assertIsDisplayed()
        compose.runOnIdle { expanded.value = true }
        compose.onNodeWithText("Left widgets").assertIsDisplayed()
        compose.onNodeWithText("Favorite").assertIsDisplayed()
        compose.onNodeWithText("Favorite 25").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Favorite").assertIsDisplayed()
        compose.onNodeWithText("Favorite 25").assertIsDisplayed()
        compose.runOnIdle { expanded.value = false }
        compose.onNodeWithText("Favorite").assertIsDisplayed()
        compose.onNodeWithText("Favorite 25").assertIsDisplayed()
    }
}
