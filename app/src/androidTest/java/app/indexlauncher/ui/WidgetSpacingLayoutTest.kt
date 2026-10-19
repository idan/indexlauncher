package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.model.HomePage
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetSpacing
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.ui.theme.IndexLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetSpacingLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var spacing by mutableStateOf(WidgetSpacing.Default)

    @Test fun homeWidgetsUseConfiguredOuterPaddingAndRowGaps() = checkSpacing(isHome = true)

    @Test fun leftWidgetPagesUseConfiguredOuterPaddingAndRowGaps() = checkSpacing(isHome = false)

    private fun checkSpacing(isHome: Boolean) {
        val host = AppWidgetHost(compose.activity, 12345)
        val manager = AppWidgetManager.getInstance(compose.activity)
        val widgets = listOf(WidgetPlacement(-1, WidgetWidth.FULL), WidgetPlacement(-2, WidgetWidth.FULL))
        compose.setContent {
            IndexLauncherTheme {
                Box(Modifier.width(360.dp).height(650.dp)) {
                    if (isHome) {
                        HomeWidgetRegion(
                            widgets = widgets, host = host, manager = manager,
                            editing = false, onEditingChange = {}, onAddWidget = {}, onRemoveWidget = {},
                            onMoveWidget = { _, _ -> }, onWidthChange = { _, _ -> }, onHeightChange = { _, _ -> },
                            spacing = spacing,
                        )
                    } else {
                        WidgetPageScreen(
                            page = HomePage.Widgets(1), widgets = widgets, host = host, manager = manager,
                            onAddWidget = {}, onRemoveWidget = {}, onMoveWidget = { _, _ -> },
                            onWidthChange = { _, _ -> }, onHeightChange = { _, _ -> }, spacing = spacing,
                        )
                    }
                }
            }
        }
        val density = compose.activity.resources.displayMetrics.density
        val original = widgetBounds()
        assertEquals(16f * density, original.second.top - original.first.bottom, 1f)
        compose.runOnIdle { spacing = WidgetSpacing(0, 0, 0) }
        val compact = widgetBounds()
        assertEquals(0f, compact.second.top - compact.first.bottom, 1f)
        assertEquals(20f * density, original.first.left - compact.first.left, 1f)
        assertEquals(20f * density, original.first.top - compact.first.top, 1f)
        assertEquals(40f * density, compact.first.width - original.first.width, 1f)

        compose.runOnIdle { spacing = WidgetSpacing(40, 40, 40) }
        val roomy = widgetBounds()
        assertEquals(40f * density, roomy.second.top - roomy.first.bottom, 1f)
        assertEquals(40f * density, roomy.first.left - compact.first.left, 1f)
        assertEquals(40f * density, roomy.first.top - compact.first.top, 1f)
        assertEquals(80f * density, compact.first.width - roomy.first.width, 1f)
    }

    private fun widgetBounds(): Pair<Rect, Rect> {
        val widgets = compose.onAllNodesWithText(compose.activity.getString(R.string.widget_unavailable))
        return widgets.onFirst().onParent().fetchSemanticsNode().boundsInRoot to
            widgets.onLast().onParent().fetchSemanticsNode().boundsInRoot
    }
}
