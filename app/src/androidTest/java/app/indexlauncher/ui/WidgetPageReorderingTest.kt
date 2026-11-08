package app.indexlauncher.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.data.PageSettingsRepository
import app.indexlauncher.data.WidgetPlacementRepository
import app.indexlauncher.model.PageCounts
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetPageReorderingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var saved: List<Int>? = null
    private var cancelled = false
    private var count = 2
    private val snapshots = (1..5).associateWith { ordinal ->
        WidgetPageSnapshot(Bitmap.createBitmap(180, 320, Bitmap.Config.ARGB_8888).apply {
            eraseColor(if (ordinal == 1) Color.BLUE else Color.GREEN)
        }, listOf(if (ordinal == 1) "Weather" else "Calendar"), ordinal == 1)
    }

    @Test fun overviewUsesFullWidthWithoutAHomeMarkerOrPreviewExplanation() {
        show()
        compose.onNodeWithTag("page-order-list").assertWidthIsEqualTo(360.dp)
        compose.onNodeWithText("Home").assertDoesNotExist()
        compose.onNodeWithText("Previews show the top of each screen.", substring = true).assertDoesNotExist()
        compose.onNodeWithText(compose.activity.getString(R.string.reorder_pages_hint)).assertIsDisplayed()
    }

    @Test fun arrowMovesDraftAndDoneSavesNearestHomeFirst() {
        show()
        compose.onNodeWithText("Weather").assertIsDisplayed()
        compose.onNodeWithText("Calendar").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.page_snapshot_more)).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.page_move_away, 1)).performClick()
        assertNull(saved)
        done()
        assertEquals(listOf(2, 1), saved)
    }

    @Test fun cancelAndBackDoNotSaveChanges() {
        show()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.page_move_away, 1)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.cancel)).performClick()
        assertTrue(cancelled)
        assertNull(saved)
    }

    @Test fun backCancelsOverviewDraft() {
        show()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.page_move_away, 1)).performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(cancelled)
        assertNull(saved)
    }

    @Test fun draftSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.page_move_away, 1)).performClick()
        restoration.emulateSavedInstanceStateRestore()
        done()
        assertEquals(listOf(2, 1), saved)
    }

    @Test fun dragReordersAndCancelledDragRestoresOriginalDraft() {
        show()
        drag(cancel = true)
        done()
        assertEquals(listOf(1, 2), saved)
        saved = null
        drag()
        done()
        assertEquals(listOf(2, 1), saved)
    }

    @Test fun holdingNearLeftEdgeReachesOffscreenScreens() {
        count = 5
        show()
        val start = compose.onNodeWithText(compose.activity.getString(R.string.widget_page, 1)).fetchSemanticsNode().boundsInRoot.center
        val viewport = compose.onNodeWithTag("page-order-list").fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput {
            down(start)
            advanceEventTime(650)
            moveBy(Offset(-1f, 0f))
        }
        compose.mainClock.advanceTimeBy(32)
        compose.onRoot().performTouchInput { moveTo(Offset(viewport.left + 12f, start.y), delayMillis = 200) }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        done()
        assertEquals(1, saved?.last())
    }

    @Test fun backDuringDragCancelsOnlyTheDrag() {
        show()
        val start = compose.onNodeWithText(compose.activity.getString(R.string.widget_page, 2)).fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithText(compose.activity.getString(R.string.widget_page, 1)).fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {
            down(start)
            advanceEventTime(650)
            for (step in 1..24) moveTo(start + (end - start + Offset(64f, 0f)) * (step / 24f), delayMillis = 16)
        }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onRoot().performTouchInput { up() }
        assertEquals(false, cancelled)
        done()
        assertEquals(listOf(1, 2), saved)
    }

    @Test fun accessibilityActionsUsePhysicalDirectionsInRtl() {
        show(rtl = true)
        val action = compose.onNodeWithText(compose.activity.getString(R.string.widget_page, 1))
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first { it.label == compose.activity.getString(R.string.page_move_away, 1) }
        compose.runOnIdle { assertTrue(action.action()) }
        done()
        assertEquals(listOf(2, 1), saved)
    }

    @Test fun savedOrderSurvivesRepositoryRecreationAndHiddenPageRestoration() {
        val prefs = compose.activity.getSharedPreferences("workspace_left", 0)
        val originalCount = prefs.getInt("left_pages", -1)
        val originalOrder = prefs.getString("page_order", null)
        val widgets = WidgetPlacementRepository(compose.activity)
        val originalWidgets = widgets.widgets.value
        try {
            prefs.edit().putInt("left_pages", 3).remove("page_order").commit()
            val pages = PageSettingsRepository(compose.activity)
            pages.reorder(listOf(3, 1, 2))
            assertEquals(listOf(3, 1, 2, 4, 5), PageSettingsRepository(compose.activity).counts.value.order)
            pages.setLeft(2)
            pages.reorder(listOf(1, 3))
            pages.setLeft(3)
            assertEquals(listOf(1, 3, 2), PageSettingsRepository(compose.activity).counts.value.visibleOrdinals)
            widgets.refresh()
            assertEquals(originalWidgets, widgets.widgets.value)
            pages.reorder(listOf(1, 3, 4)) // Cannot reorder a hidden screen into the visible set.
            assertEquals(listOf(1, 3, 2), pages.counts.value.visibleOrdinals)
        } finally {
            prefs.edit().apply {
                if (originalCount == -1) remove("left_pages") else putInt("left_pages", originalCount)
                if (originalOrder == null) remove("page_order") else putString("page_order", originalOrder)
            }.commit()
        }
    }

    private fun show(rtl: Boolean = false) = compose.setContent { Content(rtl) }

    @androidx.compose.runtime.Composable
    private fun Content(rtl: Boolean = false) {
        IndexLauncherTheme {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.width(360.dp).height(650.dp)) {
                    PageOrderingContent(PageCounts(count), snapshots, 1, { saved = it }, { cancelled = true })
                }
            }
        }
    }

    private fun done() = compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick()

    private fun drag(cancel: Boolean = false) {
        val start = compose.onNodeWithText(compose.activity.getString(R.string.widget_page, 2)).fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithText(compose.activity.getString(R.string.widget_page, 1)).fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {
            down(start)
            advanceEventTime(650)
            // The last card is partially clipped; cross its actual center, not just
            // the center of the visible semantics bounds.
            val destination = end + Offset(64f, 0f)
            for (step in 1..24) moveTo(start + (destination - start) * (step / 24f), delayMillis = 16)
            if (cancel) cancel() else up()
        }
        compose.waitForIdle()
    }
}
