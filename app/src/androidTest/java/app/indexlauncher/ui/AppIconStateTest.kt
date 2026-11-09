package app.indexlauncher.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app = AppEntry("test.app", "Main", "Test")
    private val icon = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
    private val frames = mutableListOf<Bitmap?>()

    @Test fun cachedIconIsAvailableOnFirstCompositionWithoutAsyncLoad() {
        var loads = 0
        compose.setContent {
            val bitmap = rememberAppIcon(app, 1, IconShape.Circle,
                getCachedIcon = { _, _, _ -> icon },
                loadIcon = { _, _, _ -> loads++; null },
            )
            SideEffect { frames += bitmap }
        }
        compose.runOnIdle {
            assertSame(icon, frames.first())
            assertEquals(0, loads)
        }
    }

    @Test fun disposedRowsReenterWithCachedIconImmediately() {
        var visible by mutableStateOf(true)
        compose.setContent {
            if (visible) {
                val bitmap = rememberAppIcon(app, 1, IconShape.Circle,
                    getCachedIcon = { _, _, _ -> icon },
                    loadIcon = { _, _, _ -> error("A cached icon must not load again") },
                )
                SideEffect { frames += bitmap }
            }
        }
        compose.runOnIdle { visible = false }
        compose.runOnIdle {
            frames.clear()
            visible = true
        }
        compose.runOnIdle {
            assertTrue(frames.isNotEmpty())
            assertTrue(frames.all { it === icon })
        }
    }

    @Test fun cacheMissLoadsAsynchronously() {
        val pending = CompletableDeferred<Bitmap?>()
        compose.setContent {
            val bitmap = rememberAppIcon(app, 1, IconShape.Circle,
                getCachedIcon = { _, _, _ -> null },
                loadIcon = { _, _, _ -> pending.await() },
            )
            SideEffect { frames += bitmap }
        }
        compose.runOnIdle {
            assertNull(frames.first())
            pending.complete(icon)
        }
        compose.runOnIdle { assertSame(icon, frames.last()) }
    }

    @Test fun catalogAndShapeChangesResetToTheMatchingCachedIcon() {
        var version by mutableStateOf(1)
        var shape by mutableStateOf(IconShape.Circle)
        val updated = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val reshaped = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            val bitmap = rememberAppIcon(app, version, shape,
                getCachedIcon = { _, catalog, mask ->
                    when {
                        mask == IconShape.Squircle -> reshaped
                        catalog == 2 -> updated
                        else -> icon
                    }
                },
                loadIcon = { _, _, _ -> error("All test icons are cached") },
            )
            SideEffect { frames += bitmap }
        }
        compose.runOnIdle {
            frames.clear()
            version = 2
        }
        compose.runOnIdle {
            assertSame(updated, frames.first())
            frames.clear()
            shape = IconShape.Squircle
        }
        compose.runOnIdle { assertSame(reshaped, frames.first()) }
    }

    @Test fun catalogRefreshKeepsVisibleIconUntilReplacementLoads() {
        var version by mutableStateOf(1)
        val pending = CompletableDeferred<Bitmap?>()
        val updated = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            val bitmap = rememberAppIcon(app, version, IconShape.Circle,
                getCachedIcon = { _, catalog, _ -> if (catalog == 1) icon else null },
                loadIcon = { _, _, _ -> pending.await() },
            )
            SideEffect { frames += bitmap }
        }
        compose.runOnIdle {
            frames.clear()
            version = 2
        }
        compose.runOnIdle {
            assertSame(icon, frames.first())
            pending.complete(updated)
        }
        compose.runOnIdle { assertSame(updated, frames.last()) }
    }

    @Test fun uncachedShapeDoesNotKeepDisplayingPreviousShape() {
        var shape by mutableStateOf(IconShape.Circle)
        val pending = CompletableDeferred<Bitmap?>()
        compose.setContent {
            val bitmap = rememberAppIcon(app, 1, shape,
                getCachedIcon = { _, _, mask -> if (mask == IconShape.Circle) icon else null },
                loadIcon = { _, _, _ -> pending.await() },
            )
            SideEffect { frames += bitmap }
        }
        compose.runOnIdle {
            frames.clear()
            shape = IconShape.Squircle
        }
        compose.runOnIdle {
            assertNull(frames.first())
            pending.complete(icon)
        }
        compose.runOnIdle { assertSame(icon, frames.last()) }
    }
}
