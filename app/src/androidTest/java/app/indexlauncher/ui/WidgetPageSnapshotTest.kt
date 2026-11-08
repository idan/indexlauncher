package app.indexlauncher.ui

import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetSpacing
import app.indexlauncher.model.WidgetWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetPageSnapshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun snapshotDrawsConfiguredContentsAtNormalSizeAndTransfersOneHostView() {
        val context = compose.activity
        val manager = AppWidgetManager.getInstance(context)
        val info = manager.installedProviders.firstOrNull { it.minHeight > 0 }
        assumeNotNull(info)
        val provider = requireNotNull(info)
        val host = RetainedWidgetHost(context, 20432)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val id = host.allocateAppWidgetId()
        try {
            // Temporary shell identity, not a persistent package bind grant or consent UI.
            automation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            val bound = manager.bindAppWidgetIdIfAllowed(id, provider.provider)
            automation.dropShellPermissionIdentity()
            assumeTrue("Device must permit test widget binding", bound)
            compose.runOnIdle {
                val density = Density(context.resources.displayMetrics.density)
                val viewport = IntSize((360 * density.density).toInt(), (600 * density.density).toInt())
                val widget = host.obtainView(context, id, provider)
                // Configured instance content deliberately differs from the provider's gallery.
                widget.updateAppWidget(RemoteViews("android", android.R.layout.simple_list_item_1).apply {
                    setTextViewText(android.R.id.text1, "Configured instance")
                    setInt(android.R.id.text1, "setBackgroundColor", Color.MAGENTA)
                })
                val renderer = SnapshotPageView(context)
                renderer.populate(
                    listOf(WidgetPlacement(id, WidgetWidth.FULL, 800)), mapOf(id to provider),
                    host, WidgetSpacing.Default, density, LayoutDirection.Ltr, viewport,
                )
                assertSame(widget, renderer.getChildAt(0))
                val snapshot = renderer.capture(viewport)
                assertEquals(360, snapshot.bitmap.width)
                assertEquals(600, snapshot.bitmap.height)
                assertTrue(snapshot.overflows)
                assertEquals("Configured instance", widget.findViewById<TextView>(android.R.id.text1).text.toString())
                val pixels = IntArray(snapshot.bitmap.width * snapshot.bitmap.height)
                snapshot.bitmap.getPixels(pixels, 0, snapshot.bitmap.width, 0, 0, snapshot.bitmap.width, snapshot.bitmap.height)
                // Font scaling/text positioning and framework force-dark vary by device;
                // check the rendered artwork rather than one hard-coded pixel.
                assertTrue("Snapshot should contain the configured magenta artwork", pixels.any {
                    Color.red(it) > 100 && Color.blue(it) > 100 && Color.green(it) < 32
                })
                assertTrue("Provider must be measured at real pane width", widget.measuredWidth > snapshot.bitmap.width)
                val options = manager.getAppWidgetOptions(id)
                assertTrue("Never send thumbnail width", options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) > 250)
                assertTrue("Never send thumbnail height", options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) >= 750)
                val newFrame = FrameLayout(context)
                newFrame.addView(host.obtainView(context, id, provider))
                assertEquals(0, renderer.childCount)
                assertSame(widget, newFrame.getChildAt(0))
                newFrame.removeAllViews()
            }
        } finally {
            automation.dropShellPermissionIdentity()
            host.deleteAppWidgetId(id)
        }
    }

    @Test fun boundProviderRendersOnDemandWithoutASecondHostView() {
        val context = compose.activity
        val manager = AppWidgetManager.getInstance(context)
        val info = manager.installedProviders.firstOrNull {
            it.provider.packageName == "com.android.chrome" && it.provider.className.contains("Dino")
        }
        assumeNotNull("Real-provider smoke test needs Chrome's Dino widget", info)
        val provider = requireNotNull(info)
        val host = RetainedWidgetHost(context, 20434)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val id = host.allocateAppWidgetId()
        try {
            automation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            val bound = manager.bindAppWidgetIdIfAllowed(id, provider.provider)
            automation.dropShellPermissionIdentity()
            assumeTrue("Device must permit test widget binding", bound)
            host.startListening()
            var snapshot: WidgetPageSnapshot? = null
            var rendering by mutableStateOf(true)
            compose.setContent {
                Box(Modifier.fillMaxSize()) {
                    if (rendering) WidgetPageSnapshotRenderer(
                        pages = listOf(1), widgets = mapOf("left-1" to listOf(WidgetPlacement(id, WidgetWidth.FULL, 240))),
                        host = host, manager = manager, spacing = WidgetSpacing.Default, version = 0,
                        onReset = { snapshot = null }, onSnapshot = { _, result -> snapshot = result }, modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            compose.waitUntil(10_000) { snapshot != null }
            val result = requireNotNull(snapshot)
            assertEquals(listOf(provider.loadLabel(context.packageManager)), result.labels)
            assertTrue(result.bitmap.width <= 360 && result.bitmap.height <= 900)
            val pixels = IntArray(result.bitmap.width * result.bitmap.height)
            result.bitmap.getPixels(pixels, 0, result.bitmap.width, 0, 0, result.bitmap.width, result.bitmap.height)
            assertTrue("Real provider should have drawn content", pixels.any { Color.alpha(it) > 0 })
            compose.runOnIdle {
                rendering = false
            }
            compose.waitForIdle()
            compose.runOnIdle {
                val frame = FrameLayout(context)
                val view = host.obtainView(context, id, provider)
                frame.addView(view)
                frame.removeAllViews()
                assertSame(view, host.obtainView(context, id, provider))
            }
        } finally {
            automation.dropShellPermissionIdentity()
            host.stopListening()
            host.deleteAppWidgetId(id)
        }
    }

    @Test fun unavailableWidgetAndEmptyPageStillProduceBoundedSnapshots() {
        compose.runOnIdle {
            val renderer = SnapshotPageView(compose.activity)
            val viewport = IntSize(1080, 1800)
            val host = RetainedWidgetHost(compose.activity, 20433)
            renderer.populate(
                listOf(WidgetPlacement(-1, WidgetWidth.FULL)), emptyMap(), host,
                WidgetSpacing.Default, Density(3f), LayoutDirection.Rtl, viewport,
            )
            val unavailable = renderer.capture(viewport)
            assertEquals(1, unavailable.labels.size)
            assertEquals(360, unavailable.bitmap.width)
            assertEquals(600, unavailable.bitmap.height)
            renderer.populate(emptyList(), emptyMap(), host, WidgetSpacing.Default, Density(3f), LayoutDirection.Ltr, viewport)
            val empty = renderer.capture(viewport)
            assertTrue(empty.labels.isEmpty())
            assertEquals(0, renderer.childCount)
        }
    }
}
