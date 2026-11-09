package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.PageCounts
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.ui.theme.IndexLauncherTheme
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlphabetRailScrubbingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var expanded by mutableStateOf(false)
    private var launches = 0
    private var drops = 0
    private var settingsOpens = 0
    private val feedback = mutableListOf<HapticFeedbackType>()
    private val density get() = compose.activity.resources.displayMetrics.density
    private val left get() = compose.onNodeWithTag("alphabet-rail-left")
    private val right get() = compose.onNodeWithTag("alphabet-rail-right")
    private val bubble get() = compose.onNodeWithTag("alphabet-rail-bubble")

    @Test fun leftScrubsUseRightTrackAndTickOnlyWhenTargetChanges() {
        showHome()
        val leftBounds = left.fetchSemanticsNode().boundsInRoot
        val aY = letter('A').fetchSemanticsNode().boundsInRoot.center.y - leftBounds.top
        val bY = letter('B').fetchSemanticsNode().boundsInRoot.center.y - leftBounds.top
        val cY = letter('C').fetchSemanticsNode().boundsInRoot.center.y - leftBounds.top
        left.performTouchInput { down(Offset(center.x, aY)) }
        assertBubbleAt(leftBounds.top + aY)
        val originalX = bubble.fetchSemanticsNode().boundsInRoot.center.x
        left.performTouchInput {
            moveTo(Offset(center.x + 4f, aY + 1f))
            moveTo(Offset(center.x, bY))
        }
        compose.onNodeWithText("Bravo 0").assertIsDisplayed()
        compose.onNodeWithText("Alpha 0").assertIsNotDisplayed()
        assertBubbleAt(leftBounds.top + bY)
        assertEquals(originalX, bubble.fetchSemanticsNode().boundsInRoot.center.x, 1f)
        left.performTouchInput {
            moveTo(Offset(center.x, cY))
            moveTo(Offset(center.x, bY))
            up()
        }
        bubble.assertDoesNotExist()
        assertEquals(List(4) { HapticFeedbackType.SegmentFrequentTick }, feedback)
        assertEquals(0, launches)
    }

    @Test fun leftTapBookendsNavigateWithoutOpeningSettingsAndHaveNoDuplicateLetterActions() {
        showHome()
        compose.onAllNodesWithContentDescription(compose.activity.getString(R.string.jump_to_letter, "B"))
            .assertCountEquals(1)
        tapLeft(compose.activity.getString(R.string.show_launcher_tools))
        compose.onNodeWithText(compose.activity.getString(R.string.settings_tile_title)).assertIsDisplayed()
        assertEquals(0, settingsOpens)
        tapLeft(compose.activity.getString(R.string.show_favorites))
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.search_apps)).assertDoesNotExist()
        compose.onNodeWithText("Alpha 0").assertIsDisplayed()
        tapLeft(compose.activity.getString(R.string.jump_to_letter, "#"))
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.search_apps)).assertIsDisplayed()
        assertEquals(List(3) { HapticFeedbackType.SegmentFrequentTick }, feedback)
    }

    @Test fun appRowsReserveGuttersWhileHomeWidgetsExtendUnderBothRails() {
        showHome(widgets = true)
        val leftBounds = left.fetchSemanticsNode().boundsInRoot
        val row = compose.onNodeWithText("Alpha 0").fetchSemanticsNode().boundsInRoot
        assertEquals(36f * density, leftBounds.width, 1f)
        assertEquals(48f * density, right.fetchSemanticsNode().boundsInRoot.width, 1f)
        assertEquals(leftBounds.right, row.left, 1f)
        val label = compose.onNodeWithText("Alpha 0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // Compose rounds each dimension separately: gutter + row padding + icon + icon/label gap.
        val expectedLabelLeft = leftBounds.left + listOf(36, 24, 40, 16).sumOf { (it * density).roundToInt() }
        assertEquals(expectedLabelLeft, label.left, 1f)
        val widget = compose.onNodeWithText(compose.activity.getString(R.string.widget_unavailable))
            .onParent().fetchSemanticsNode().boundsInRoot
        assertEquals(leftBounds.left + 20f * density, widget.left, 1f)
        assertTrue(widget.left < leftBounds.right)
        assertTrue(widget.right > right.fetchSemanticsNode().boundsInRoot.left)
        assertEquals(right.fetchSemanticsNode().boundsInRoot.right - 20f * density, widget.right, 1f)
        tapLeft(compose.activity.getString(R.string.jump_to_letter, "B"))
        assertEquals(leftBounds.right, compose.onNodeWithText("Bravo 0").fetchSemanticsNode().boundsInRoot.left, 1f)
    }

    @Test fun widgetEdgeTouchesScrubBothRailsButInteriorTouchesDoNot() {
        showHome(widgets = true)
        val root = compose.onRoot()
        val origin = root.fetchSemanticsNode().boundsInRoot.topLeft
        val widget = compose.onNodeWithText(compose.activity.getString(R.string.widget_unavailable))
            .onParent().fetchSemanticsNode().boundsInRoot
        val leftBounds = left.fetchSemanticsNode().boundsInRoot
        val rightBounds = right.fetchSemanticsNode().boundsInRoot

        root.performTouchInput { click(widget.center - origin) }
        assertTrue(feedback.isEmpty())
        bubble.assertDoesNotExist()
        val edgePoints = listOf(
            Offset((widget.left + leftBounds.right) / 2f, widget.center.y),
            Offset((rightBounds.left + widget.right) / 2f, widget.center.y),
        )
        edgePoints.forEachIndexed { index, point ->
            // The point is inside the widget's measured bounds, but the rail wins hit testing.
            assertTrue(widget.contains(point))
            root.performTouchInput { down(point - origin) }
            bubble.assertIsDisplayed()
            assertEquals(List(index + 1) { HapticFeedbackType.SegmentFrequentTick }, feedback)
            root.performTouchInput { up() }
            bubble.assertDoesNotExist()
            compose.onNodeWithText(compose.activity.getString(R.string.widget_unavailable)).assertIsDisplayed()
        }
        assertEquals(0, launches)
        assertEquals(0, drops)
    }

    @Test fun gutterLongPressAndDragOntoAppsCannotLaunchOrReorder() {
        showHome()
        val leftBounds = left.fetchSemanticsNode().boundsInRoot
        val row = compose.onNodeWithText("Alpha 0").fetchSemanticsNode().boundsInRoot
        left.performTouchInput {
            down(Offset(center.x, row.center.y - leftBounds.top))
            advanceEventTime(650)
            moveTo(Offset(row.center.x - leftBounds.left, row.center.y - leftBounds.top - 40f))
            up()
        }
        assertEquals(0, launches)
        assertEquals(0, drops)
        assertTrue(feedback.all { it == HapticFeedbackType.SegmentFrequentTick })
        compose.onNodeWithText("Widget page").assertDoesNotExist()
    }

    @Test fun pagingFromAppRowsStillWorksAndLeftScrubReactivatesAfterReturning() {
        showHome()
        compose.onNodeWithText("Alpha 0").performTouchInput {
            swipe(center, center + Offset(220f * density, 0f), durationMillis = 500)
        }
        compose.onNodeWithText("Widget page").assertIsDisplayed()
        left.assertIsNotDisplayed()
        compose.onNodeWithTag("widget-page").performTouchInput {
            swipe(Offset(width * 0.85f, center.y), Offset(width * 0.1f, center.y), durationMillis = 500)
        }
        left.assertIsDisplayed()
        tapLeft(compose.activity.getString(R.string.jump_to_letter, "B"))
        compose.onNodeWithText("Bravo 0").assertIsDisplayed()
        assertEquals(0, launches)
        assertEquals(0, drops)
    }

    @Test fun searchHasNeitherRailNorAdditionalLeftInset() {
        showHome()
        tapLeft(compose.activity.getString(R.string.jump_to_letter, "A"))
        val safeLeft = left.fetchSemanticsNode().boundsInRoot.left
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.search_apps)).performClick()
        left.assertDoesNotExist()
        right.assertDoesNotExist()
        bubble.assertDoesNotExist()
        assertEquals(safeLeft, compose.onNodeWithText("Alpha 0").fetchSemanticsNode().boundsInRoot.left, 1f)
    }

    @Test fun rtlKeepsInvisibleGutterOnPhysicalLeftAndBubbleBesidePhysicalRightRail() {
        showHome(rtl = true)
        val leftBounds = left.fetchSemanticsNode().boundsInRoot
        val rightBounds = right.fetchSemanticsNode().boundsInRoot
        assertTrue(leftBounds.right < rightBounds.left)
        val row = compose.onNodeWithText("Alpha 0").fetchSemanticsNode().boundsInRoot
        assertEquals(leftBounds.right, row.left, 1f)
        assertEquals(rightBounds.left, row.right, 1f)
        val y = letter('B').fetchSemanticsNode().boundsInRoot.center.y
        left.performTouchInput { down(Offset(center.x, y - leftBounds.top)) }
        assertBubbleAt(y)
        left.performTouchInput { up() }
    }

    @Test fun twoPaneHomeRetainsOriginalAppAndWidgetInsets() {
        assumeTrue("Two-pane layout needs a wide emulator", compose.activity.resources.configuration.screenWidthDp >= 760)
        expanded = true
        showHome(widgets = true)
        left.assertDoesNotExist()
        val row = compose.onNodeWithText("Alpha 0").fetchSemanticsNode().boundsInRoot
        val label = compose.onNodeWithText("Alpha 0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(80f * density, label.left - row.left, 1f)
        assertEquals((380f - 48f) * density, row.width, 1f)
        val widget = compose.onNodeWithText(compose.activity.getString(R.string.widget_unavailable))
            .onParent().fetchSemanticsNode().boundsInRoot
        assertEquals(row.left + 20f * density, widget.left, 1f)
        assertEquals(right.fetchSemanticsNode().boundsInRoot.right - 20f * density, widget.right, 1f)
    }

    @Test fun unfoldingCancelsLeftScrubRemovesGutterAndPreservesAppMode() {
        assumeTrue("Pane transitions need a wide emulator", compose.activity.resources.configuration.screenWidthDp >= 760)
        showHome()
        val bounds = left.fetchSemanticsNode().boundsInRoot
        val y = letter('B').fetchSemanticsNode().boundsInRoot.center.y
        left.performTouchInput { down(Offset(center.x, y - bounds.top)) }
        bubble.assertIsDisplayed()
        compose.runOnIdle { expanded = true }
        left.assertDoesNotExist()
        bubble.assertDoesNotExist()
        compose.onNodeWithText("Widget page").assertIsDisplayed()
        compose.onNodeWithText("Bravo 0").assertIsDisplayed()
        assertEquals((380f - 48f) * density, compose.onNodeWithText("Bravo 0").fetchSemanticsNode().boundsInRoot.width, 1f)
        // Release the canceled pointer through the surviving rail, then fold back.
        right.performTouchInput { up() }
        compose.runOnIdle { expanded = false }
        left.assertIsDisplayed()
        compose.onNodeWithText("Bravo 0").assertIsDisplayed()
        assertEquals((360f - 36f - 48f) * density, compose.onNodeWithText("Bravo 0").fetchSemanticsNode().boundsInRoot.width, 1f)
    }

    private fun assertBubbleAt(y: Float) {
        val bounds = bubble.fetchSemanticsNode().boundsInRoot
        assertEquals(y, bounds.center.y, 1f)
        assertEquals(8f * density, right.fetchSemanticsNode().boundsInRoot.left - bounds.right, 1f)
    }

    private fun letter(letter: Char) = compose.onNodeWithContentDescription(
        compose.activity.getString(R.string.jump_to_letter, letter.toString()),
    )

    private fun tapLeft(description: String) {
        val y = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center.y
        val top = left.fetchSemanticsNode().boundsInRoot.top
        left.performTouchInput { click(Offset(center.x, y - top)) }
    }

    private fun showHome(widgets: Boolean = false, rtl: Boolean = false) {
        val apps = listOf("Alpha", "Bravo", "Charlie").flatMap { name ->
            List(8) { AppEntry("test.${name.lowercase()}$it", "Main", "$name $it") }
        }
        val host = AppWidgetHost(compose.activity, 12345)
        val manager = AppWidgetManager.getInstance(compose.activity)
        val haptics = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                feedback += hapticFeedbackType
            }
        }
        compose.setContent {
            CompositionLocalProvider(
                LocalOverscrollFactory provides null,
                LocalHapticFeedback provides haptics,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                IndexLauncherTheme {
                    Box(Modifier.width(if (expanded) 760.dp else 360.dp).height(650.dp)) {
                        HomePager(
                            counts = PageCounts(1),
                            widgetPage = { _, _ ->
                                Box(Modifier.fillMaxSize().testTag("widget-page")) { Text("Widget page") }
                            },
                            appPage = { suppressRail, active, leftScrubbingEnabled, onRailInteraction, onScrimOpacityChange ->
                                LauncherScreen(
                                    state = LauncherUiState(apps = apps, isLoading = false),
                                    favoriteIds = apps.filter { it.label.endsWith(" 0") }.map { it.id },
                                    homeWidgets = if (widgets) listOf(WidgetPlacement(-1, WidgetWidth.FULL)) else emptyList(),
                                    widgetHost = host,
                                    widgetManager = manager,
                                    suppressRail = suppressRail,
                                    active = active,
                                    leftScrubbingEnabled = leftScrubbingEnabled,
                                    onRailInteraction = onRailInteraction,
                                    onScrimOpacityChange = onScrimOpacityChange,
                                    onToggleFavorite = {},
                                    onReorderFavorites = { drops++ },
                                    onAppClick = { launches++ },
                                    onOpenSettings = { settingsOpens++ },
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
                        )
                    }
                }
            }
        }
    }
}
