package app.indexlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserManager
import android.util.LruCache
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads activity-specific icons off the UI thread and bounds their bitmap memory use. */
class AppIconRepository(context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val packageManager = context.packageManager
    private val resources = context.resources
    private val iconSizePx = (44 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun clear() = cache.evictAll()

    // A new catalog version avoids caching an icon from an in-flight, pre-refresh request.
    suspend fun loadIcon(app: AppEntry, catalogVersion: Int, shape: IconShape): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$catalogVersion:${shape.name}:${app.id}"
        cache.get(key)?.let { return@withContext it }

        val bitmap = try {
            val user = app.userHandle(userManager) ?: return@withContext null
            if (user !in launcherApps.profiles || userManager.isQuietModeEnabled(user)) return@withContext null
            val activity = launcherApps.resolveActivity(
                Intent.makeMainActivity(ComponentName(app.packageName, app.activityName)), user,
            ) ?: return@withContext null
            val icon = activity.getIcon(0)
            val rendered = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                if (icon is AdaptiveIconDrawable && shape != IconShape.DeviceDefault) {
                    drawAdaptiveIcon(canvas, icon, shape)
                } else {
                    // Let Android apply its own mask; do not crop legacy single-image artwork.
                    drawLayer(canvas, icon, 0, iconSizePx)
                }
            }
            if (app.profileSerial == null) rendered else {
                val badged = packageManager.getUserBadgedIcon(BitmapDrawable(resources, rendered), user)
                Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888).also {
                    drawLayer(Canvas(it), badged, 0, iconSizePx)
                    rendered.recycle()
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return@withContext null // Uninstalled between listing and loading.
        } catch (e: RuntimeException) {
            return@withContext null // A broken or inaccessible app icon must not crash Home.
        }
        cache.put(key, bitmap)
        bitmap
    }

    private fun drawAdaptiveIcon(canvas: Canvas, icon: AdaptiveIconDrawable, shape: IconShape) {
        // Adaptive icon assets are 108dp square; the mask shows the centered 72dp.
        // Draw each layer at 150% before masking so artwork has its intended safe zone.
        val inset = iconSizePx / 4
        icon.background?.let { drawLayer(canvas, it, -inset, iconSizePx + inset) }
        icon.foreground?.let { drawLayer(canvas, it, -inset, iconSizePx + inset) }

        // Mask the entire bitmap, including corners *outside* the path. Drawing only
        // the path with DST_IN would leave those pixels untouched.
        val mask = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
        Canvas(mask).drawPath(iconMask(shape, iconSizePx.toFloat()), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        })
        canvas.drawBitmap(mask, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        mask.recycle()
    }

    private fun drawLayer(canvas: Canvas, drawable: Drawable, min: Int, max: Int) {
        drawable.setBounds(min, min, max, max)
        drawable.draw(canvas)
    }
}

/** Superellipse (power 4) for a squircle; a rounded square uses circular corner arcs. */
internal fun iconMask(shape: IconShape, size: Float): Path = Path().apply {
    when (shape) {
        IconShape.Circle -> addCircle(size / 2, size / 2, size / 2, Path.Direction.CW)
        IconShape.RoundedSquare -> addRoundRect(0f, 0f, size, size, size * 0.22f, size * 0.22f, Path.Direction.CW)
        IconShape.Squircle -> {
            val center = size / 2
            for (step in 0..128) {
                val angle = step * 2 * Math.PI / 128
                val x = center + center * sign(cos(angle)) * abs(cos(angle)).pow(0.5)
                val y = center + center * sign(sin(angle)) * abs(sin(angle)).pow(0.5)
                if (step == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
            }
            close()
        }
        IconShape.DeviceDefault -> error("The device default is drawn by Android")
    }
}
