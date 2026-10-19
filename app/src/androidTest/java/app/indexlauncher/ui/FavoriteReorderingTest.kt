package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.data.FavoritesRepository
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.HomePage
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.ItemSpacing
import app.indexlauncher.model.PageCounts
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteReorderingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var ids by mutableStateOf(emptyList<String>())
    private var launches = 0
    private var drops = 0

    @Test fun longPressDragMovesDownThenUpWithoutLaunchingOrPaging() {
        val apps = showHome()
        drag("Favorite 0", "Favorite 2")
        assertEquals(listOf(apps[1].id, apps[2].id, apps[0].id, apps[3].id), ids)
        assertAbove("Favorite 2", "Favorite 0")
        drag("Favorite 0", "Favorite 1")
        assertEquals(apps.map { it.id }, ids)
        assertAbove("Favorite 0", "Favorite 1")
        assertEquals(0, launches)
        assertEquals(2, drops)
        compose.onNodeWithText("Widget page").assertDoesNotExist()
    }

    @Test fun canceledDragAndLongPressWithoutMovingDoNotSaveOrLaunch() {
        val apps = showHome()
        drag("Favorite 0", "Favorite 2", cancel = true)
        assertEquals(apps.map { it.id }, ids)
        assertAbove("Favorite 0", "Favorite 1")
        compose.onNodeWithText("Favorite 0").performTouchInput {
            down(center)
            advanceEventTime(650)
            up()
        }
        assertEquals(0, launches)
        assertEquals(0, drops)
    }

    @Test fun normalTapAndLeftSwipeStillWork() {
        showHome()
        compose.onNodeWithText("Favorite 0").performTouchInput { click() }
        assertEquals(1, launches)
        compose.onNodeWithText("Favorite 0").performTouchInput {
            swipe(center, Offset(0f, center.y), durationMillis = 400)
        }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.remove_favorite)).performClick()
        assertEquals(3, ids.size)
        assertEquals(0, drops)
    }

    @Test fun compactRtlRowsAndWidgetHeaderDoNotChangeReorderIndices() {
        val apps = showHome(widgets = true, rtl = true, spacing = ItemSpacing.fromPadding(ItemSpacing.MIN_PADDING_DP))
        drag("Favorite 3", "Favorite 1")
        assertEquals(listOf(apps[0].id, apps[3].id, apps[1].id, apps[2].id), ids)
        assertAbove("Favorite 3", "Favorite 1")
        compose.onNodeWithText(compose.activity.getString(R.string.widget_unavailable)).assertExists()
    }

    @Test fun accessibilityCanMoveFavoritesWithoutDragging() {
        val apps = showHome()
        val action = compose.onNodeWithText("Favorite 0").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .first { it.label == compose.activity.getString(R.string.move_favorite_down) }
        compose.runOnIdle { assertTrue(action.action()) }
        assertEquals(listOf(apps[1].id, apps[0].id, apps[2].id, apps[3].id), ids)
        assertAbove("Favorite 1", "Favorite 0")
        assertEquals(0, launches)
    }

    @Test fun movingTheFirstVisibleFavoriteKeepsTheViewportFixed() {
        val apps = showHome(count = 40, spacing = ItemSpacing.fromPadding(ItemSpacing.MAX_PADDING_DP))
        val initialTop = compose.onNodeWithText("Favorite 0").fetchSemanticsNode().boundsInRoot.top
        drag("Favorite 0", "Favorite 2")
        assertEquals(2, ids.indexOf(apps[0].id))
        val newTop = compose.onNodeWithText("Favorite 1").fetchSemanticsNode().boundsInRoot.top
        val viewportTop = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot.top
        // The top padding may be consumed, but the viewport must not follow the dragged key.
        assertTrue(newTop in viewportTop..initialTop)
    }

    @Test fun backCancelsAnActivePreview() {
        val apps = showHome()
        val start = compose.onNodeWithText("Favorite 0").fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithText("Favorite 2").fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {
            down(start)
            advanceEventTime(650)
            for (step in 1..24) moveTo(start + (end - start + Offset(80f, 4f)) * (step / 24f), delayMillis = 16)
        }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onRoot().performTouchInput { up() }
        assertEquals(apps.map { it.id }, ids)
        assertEquals(0, drops)
        assertEquals(0, launches)
        assertAbove("Favorite 0", "Favorite 1")
    }

    @Test fun holdingAtTopEdgeScrollsToEarlierOffscreenFavorites() {
        val apps = showHome(count = 40)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Favorite 39"))
        val start = compose.onNodeWithText("Favorite 39").fetchSemanticsNode().boundsInRoot.center
        val viewport = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput {
            down(start)
            advanceEventTime(650)
            moveBy(Offset(0f, -1f))
        }
        compose.mainClock.advanceTimeBy(32)
        compose.onRoot().performTouchInput {
            moveTo(Offset(start.x, viewport.top + 12f), delayMillis = 200)
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertTrue("Favorite should move before the initially visible rows", ids.indexOf(apps[39].id) < 24)
        assertEquals(1, drops)
        assertEquals(0, launches)
    }

    @Test fun holdingAtBottomEdgeScrollsToOffscreenFavorites() {
        val apps = showHome(count = 40)
        val start = compose.onNodeWithText("Favorite 0").fetchSemanticsNode().boundsInRoot.center
        val viewport = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput {
            down(start)
            advanceEventTime(650)
            // Start with a small movement so neither normal scroll nor row swipe claims the drag.
            moveBy(Offset(0f, 1f))
        }
        compose.mainClock.advanceTimeBy(32)
        compose.onRoot().performTouchInput {
            moveTo(Offset(start.x, viewport.bottom - 12f), delayMillis = 200)
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertTrue("Favorite should move past the initially visible rows", ids.indexOf(apps[0].id) > 15)
        assertEquals(1, drops)
        assertEquals(0, launches)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Favorite 0"))
    }

    @Test fun savedOrderSurvivesRepositoryRecreationIncludingUnavailableWorkFavorites() {
        val context = compose.activity
        val prefs = context.getSharedPreferences("favorites", 0)
        val original = prefs.getString("ordered_ids", null)
        try {
            prefs.edit().remove("ordered_ids").commit()
            val repository = FavoritesRepository(context)
            val saved = listOf("personal/Main", "profile:42:work/Main", "other/Main")
            saved.forEach(repository::toggle)
            repository.reorder(listOf(saved[2], saved[0]))
            assertEquals(listOf(saved[2], saved[1], saved[0]), FavoritesRepository(context).ids.value)
        } finally {
            prefs.edit().putString("ordered_ids", original).commit()
        }
    }

    private fun drag(from: String, to: String, cancel: Boolean = false) {
        val start = compose.onNodeWithText(from).fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithText(to).fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {
            down(start)
            advanceEventTime(650)
            // Cross the destination's center to insert on the far side of it.
            val destination = end + Offset(0f, if (end.y > start.y) 4f else -4f)
            for (step in 1..24) moveTo(start + (destination - start) * (step / 24f), delayMillis = 16)
            if (cancel) cancel() else up()
        }
        compose.waitForIdle()
    }

    private fun assertAbove(first: String, second: String) {
        assertTrue(compose.onNodeWithText(first).fetchSemanticsNode().boundsInRoot.top <
            compose.onNodeWithText(second).fetchSemanticsNode().boundsInRoot.top)
    }

    private fun showHome(
        count: Int = 4,
        widgets: Boolean = false,
        rtl: Boolean = false,
        spacing: ItemSpacing = ItemSpacing.Default,
    ): List<AppEntry> {
        val apps = List(count) { AppEntry("test.app$it", "Main", "Favorite $it") }
        ids = apps.map { it.id }
        val host = AppWidgetHost(compose.activity, 12345)
        val manager = AppWidgetManager.getInstance(compose.activity)
        compose.setContent {
            CompositionLocalProvider(
                LocalOverscrollFactory provides null,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                IndexLauncherTheme {
                    HomePager(
                        counts = PageCounts(left = 1),
                        widgetPage = { _: HomePage.Widgets, _ -> androidx.compose.material3.Text("Widget page") },
                        appPage = { suppressRail, active, onRailInteraction, onScrimOpacityChange ->
                            LauncherScreen(
                                state = LauncherUiState(apps = apps, isLoading = false),
                                favoriteIds = ids,
                                homeWidgets = if (widgets) listOf(WidgetPlacement(-1, WidgetWidth.FULL)) else emptyList(),
                                widgetHost = host,
                                widgetManager = manager,
                                suppressRail = suppressRail,
                                active = active,
                                onRailInteraction = onRailInteraction,
                                onScrimOpacityChange = onScrimOpacityChange,
                                onToggleFavorite = { id -> ids = ids.filterNot { it == id } },
                                onReorderFavorites = { ids = it; drops++ },
                                onAppClick = { launches++ },
                                onOpenSettings = {},
                                onLoadIcon = { _, _, _ -> null },
                                iconShape = IconShape.DeviceDefault,
                                itemSpacing = spacing,
                                onRetry = {},
                                onAddHomeWidget = {},
                                onRemoveHomeWidget = {},
                                onMoveHomeWidget = { _, _ -> },
                                onHomeWidgetWidthChange = { _, _ -> },
                                onHomeWidgetHeightChange = { _, _ -> },
                            )
                        },
                    )
                }
            }
        }
        compose.waitForIdle()
        return apps
    }
}
