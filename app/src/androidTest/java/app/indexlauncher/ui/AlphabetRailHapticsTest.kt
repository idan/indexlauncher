package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlphabetRailHapticsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val feedback = mutableListOf<HapticFeedbackType>()

    @Test fun scrubbingTicksOncePerNewTargetAndAgainWhenReturningToALetter() {
        showHome()
        val a = letter('A')
        val aCenter = a.fetchSemanticsNode().boundsInRoot.center
        val toB = letter('B').fetchSemanticsNode().boundsInRoot.center - aCenter
        val toC = letter('C').fetchSemanticsNode().boundsInRoot.center - aCenter

        a.performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(0f, 1f))
            moveTo(start + Offset(0f, -1f))
            moveTo(start + toB)
            moveTo(start + toB + Offset(0f, 1f))
            moveTo(start + toC)
            moveTo(start + toB)
            up()
        }

        compose.runOnIdle {
            // A, B, C, B: jitter within the same slot does not add feedback.
            assertEquals(List(4) { HapticFeedbackType.SegmentFrequentTick }, feedback)
        }
    }

    @Test fun railBookendsAndHashAlsoTickOnlyOnEntry() {
        showHome()
        val a = letter('A')
        val aCenter = a.fetchSemanticsNode().boundsInRoot.center
        val toFavorites = compose.onNodeWithContentDescription(
            compose.activity.getString(R.string.show_favorites),
        ).fetchSemanticsNode().boundsInRoot.center - aCenter
        val toHash = letter('#').fetchSemanticsNode().boundsInRoot.center - aCenter
        val toSettings = compose.onNodeWithContentDescription(
            compose.activity.getString(R.string.show_launcher_tools),
        ).fetchSemanticsNode().boundsInRoot.center - aCenter

        a.performTouchInput {
            val start = center
            down(start)
            moveTo(start + toFavorites)
            moveTo(start + toFavorites + Offset(0f, -100f))
            moveTo(start + toHash)
            moveTo(start + toSettings)
            moveTo(start + toSettings + Offset(0f, 1f))
            up()
        }

        compose.runOnIdle {
            assertEquals(List(4) { HapticFeedbackType.SegmentFrequentTick }, feedback)
        }
    }

    @Test fun separateTouchesEachTickButAccessibilityClicksDoNotInvokeScrubHaptics() {
        showHome()
        letter('A').performTouchInput { click() }
        letter('A').performTouchInput { click() }
        letter('B').performSemanticsAction(SemanticsActions.OnClick) { it() }

        compose.runOnIdle {
            assertEquals(List(2) { HapticFeedbackType.SegmentFrequentTick }, feedback)
        }
    }

    private fun letter(letter: Char) = compose.onNodeWithContentDescription(
        compose.activity.getString(R.string.jump_to_letter, letter.toString()),
    )

    private fun showHome() {
        val apps = listOf("Alpha", "Bravo", "Charlie").map {
            AppEntry("test.${it.lowercase()}", "Main", it)
        }
        val host = AppWidgetHost(compose.activity, 12345)
        val manager = AppWidgetManager.getInstance(compose.activity)
        val haptics = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                feedback += hapticFeedbackType
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                IndexLauncherTheme {
                    LauncherScreen(
                        state = LauncherUiState(apps = apps, isLoading = false),
                        favoriteIds = apps.map { it.id },
                        homeWidgets = emptyList(),
                        widgetHost = host,
                        widgetManager = manager,
                        suppressRail = false,
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
}
