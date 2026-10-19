package app.indexlauncher

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.indexlauncher.data.IconShapeRepository
import app.indexlauncher.data.ItemSpacingRepository
import app.indexlauncher.data.PageSettingsRepository
import app.indexlauncher.data.WidgetSpacingRepository
import app.indexlauncher.ui.SettingsScreen
import app.indexlauncher.ui.theme.IndexLauncherSettingsTheme

class SettingsActivity : ComponentActivity() {
    private val pageSettings by lazy { PageSettingsRepository(applicationContext) }
    private val iconShapes by lazy { IconShapeRepository(applicationContext) }
    private val itemSpacing by lazy { ItemSpacingRepository(applicationContext) }
    private val widgetSpacing by lazy { WidgetSpacingRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndexLauncherSettingsTheme {
                val counts = pageSettings.counts.collectAsStateWithLifecycle().value
                val iconShape = iconShapes.shape.collectAsStateWithLifecycle().value
                SettingsScreen(
                    counts = counts,
                    iconShape = iconShape,
                    itemSpacing = itemSpacing.spacing.collectAsStateWithLifecycle().value,
                    onItemSpacingChange = itemSpacing::setSpacing,
                    widgetSpacing = widgetSpacing.spacing.collectAsStateWithLifecycle().value,
                    onWidgetSpacingChange = widgetSpacing::setSpacing,
                    onIconShapeChange = iconShapes::setShape,
                    onLeftCountChange = pageSettings::setLeft,
                    onChooseWallpaper = ::chooseWallpaper,
                    onBack = ::finish,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pageSettings.refresh()
        iconShapes.refresh()
        itemSpacing.refresh()
        widgetSpacing.refresh()
    }

    private fun chooseWallpaper() {
        // Let the system wallpaper picker handle static and live wallpapers.
        for (action in listOf(Intent.ACTION_SET_WALLPAPER, WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) {
            try {
                startActivity(Intent(action))
                return
            } catch (e: ActivityNotFoundException) {
                // Try the platform's live-wallpaper chooser instead.
            } catch (e: SecurityException) {
                // This picker isn't available to this user/profile.
            }
        }
        Toast.makeText(this, R.string.wallpaper_picker_unavailable, Toast.LENGTH_SHORT).show()
    }
}
