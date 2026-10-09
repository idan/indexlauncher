package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesScrollingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun scrollingFavoritesAlsoScrollsWidgetsOffscreen() {
        showHome(favoriteCount = 20)
        val unavailable = compose.activity.getString(R.string.widget_unavailable)
        compose.onNodeWithText(unavailable).assertIsDisplayed()
        val widgetTop = compose.onNodeWithText(unavailable).fetchSemanticsNode().boundsInRoot.top
        compose.onAllNodes(hasScrollAction()).assertCountEquals(1)

        compose.onNodeWithText("Favorite 0").performTouchInput {
            swipe(center, Offset(center.x, center.y - 120f), durationMillis = 500)
        }
        assertTrue(compose.onNodeWithText(unavailable).fetchSemanticsNode().boundsInRoot.top < widgetTop)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Favorite 19"))
        compose.onNodeWithText("Favorite 19").assertIsDisplayed()
        compose.onNodeWithText(unavailable).assertIsNotDisplayed()

        compose.onNode(hasScrollAction()).performScrollToNode(hasText(unavailable))
        compose.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test fun oversizedWidgetsDoNotTrapFavoritesInASeparateScrollRegion() {
        showHome(favoriteCount = 3, widgets = listOf(WidgetPlacement(-1, WidgetWidth.FULL, 2000)))
        compose.onAllNodes(hasScrollAction()).assertCountEquals(1)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Favorite 2"))
        compose.onNodeWithText("Favorite 2").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(
            hasText(compose.activity.getString(R.string.widget_unavailable)),
        )
        compose.onNodeWithText(compose.activity.getString(R.string.widget_unavailable)).assertIsDisplayed()
    }

    @Test fun shortHomeKeepsWidgetsAndFavoritesVisibleAndSearchStartsOnlyOnFavorites() {
        showHome(favoriteCount = 1)
        val unavailable = compose.activity.getString(R.string.widget_unavailable)
        val closeSearch = compose.activity.getString(R.string.close_search)
        compose.onNodeWithText(unavailable).assertIsDisplayed()
        compose.onNodeWithText("Favorite 0").assertIsDisplayed()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(1)

        compose.onNodeWithText(unavailable).performTouchInput {
            swipe(center, Offset(center.x, center.y - 300f), durationMillis = 500)
        }
        compose.onNodeWithContentDescription(closeSearch).assertDoesNotExist()

        compose.onNodeWithText("Favorite 0").performTouchInput {
            swipe(center, Offset(center.x, center.y - 300f), durationMillis = 500)
        }
        compose.onNodeWithContentDescription(closeSearch).assertIsDisplayed()
    }

    @Test fun emptyHomeWidgetEditorStillOffersAddAndDone() {
        showHome(favoriteCount = 1, widgets = emptyList())
        val editAction = compose.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first()
        compose.runOnIdle { editAction.action() }
        compose.onNodeWithText(compose.activity.getString(R.string.add_widget)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.add_widget)).assertDoesNotExist()
        compose.onNodeWithText("Favorite 0").assertIsDisplayed()
    }

    @Test fun backExitsEditingEvenAfterWidgetsScrollOffscreen() {
        showHome(favoriteCount = 20)
        val editAction = compose.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first()
        compose.runOnIdle { editAction.action() }
        compose.onNodeWithText(compose.activity.getString(R.string.done)).assertIsDisplayed()

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Favorite 19"))
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNode(hasScrollAction()).performScrollToNode(
            hasText(compose.activity.getString(R.string.widget_unavailable)),
        )
        compose.onNodeWithText(compose.activity.getString(R.string.done)).assertDoesNotExist()
    }

    @Test fun widgetsAndEmptyFavoritesStillShareOneList() {
        showHome(favoriteCount = 0, widgets = listOf(WidgetPlacement(-1, WidgetWidth.FULL, 2000)))
        compose.onAllNodes(hasScrollAction()).assertCountEquals(1)
        compose.onNode(hasScrollAction()).performScrollToNode(
            hasText(compose.activity.getString(R.string.no_apps)),
        )
        compose.onNodeWithText(compose.activity.getString(R.string.no_apps)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.widget_unavailable)).assertIsNotDisplayed()
    }

    private fun showHome(
        favoriteCount: Int,
        widgets: List<WidgetPlacement> = listOf(WidgetPlacement(-1, WidgetWidth.FULL)),
    ) {
        val apps = List(favoriteCount) { AppEntry("test.app$it", "Main", "Favorite $it") }
        val host = AppWidgetHost(compose.activity, 12345)
        val manager = AppWidgetManager.getInstance(compose.activity)
        compose.setContent {
            IndexLauncherTheme {
                LauncherScreen(
                    state = LauncherUiState(apps = apps, isLoading = false),
                    favoriteIds = apps.map { it.id },
                    homeWidgets = widgets,
                    widgetHost = host,
                    widgetManager = manager,
                    suppressRail = true,
                    onRailInteraction = {},
                    onScrimOpacityChange = {},
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
            }
        }
    }
}
