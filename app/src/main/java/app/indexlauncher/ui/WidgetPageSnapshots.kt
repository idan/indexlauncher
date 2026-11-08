package app.indexlauncher.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import android.util.Log
import android.util.SizeF
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.indexlauncher.R
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetSpacing
import app.indexlauncher.model.widgetRows
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal data class WidgetPageSnapshot(val bitmap: Bitmap, val labels: List<String>, val overflows: Boolean)

/** Attached at real pane dimensions, behind the opaque overview. No preview IDs or disk cache. */
@Composable
internal fun WidgetPageSnapshotRenderer(
    pages: List<Int>,
    widgets: Map<String, List<WidgetPlacement>>,
    host: RetainedWidgetHost,
    manager: AppWidgetManager,
    spacing: WidgetSpacing,
    version: Int,
    onReset: () -> Unit,
    onSnapshot: (Int, WidgetPageSnapshot) -> Unit,
    modifier: Modifier = Modifier,
) {
    var view by remember { mutableStateOf<SnapshotPageView?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    AndroidView(
        factory = { context -> SnapshotPageView(context).also { view = it } },
        onRelease = { it.removeAllViews(); view = null },
        modifier = modifier.onSizeChanged { size = it },
    )
    LaunchedEffect(view, size, pages, widgets, spacing, version, configuration, density, direction) {
        onReset()
        val renderer = view ?: return@LaunchedEffect
        if (size.width <= 0 || size.height <= 0) return@LaunchedEffect
        // Give outgoing page AndroidViews time to release their retained children first.
        withFrameNanos { }
        withFrameNanos { }
        try {
            for (ordinal in pages) {
                val placements = widgets["left-$ordinal"].orEmpty()
                renderer.populate(placements, snapshotWidgetInfos(renderer.context, placements, manager), host, spacing, density, direction, size)
                // RemoteViews collection adapters/async inflation need an attached layout pass.
                delay(250)
                withFrameNanos { }
                onSnapshot(ordinal, renderer.capture(size))
                renderer.removeAllViews()
            }
        } finally {
            renderer.removeAllViews()
        }
    }
}

private fun snapshotWidgetInfos(
    context: Context,
    placements: List<WidgetPlacement>,
    manager: AppWidgetManager,
): Map<Int, AppWidgetProviderInfo?> = placements.associate { placement ->
    val info = try {
        manager.getAppWidgetInfo(placement.id)?.takeUnless {
            it.profile != Process.myUserHandle() && context.getSystemService(UserManager::class.java).isQuietModeEnabled(it.profile)
        }
    } catch (e: RuntimeException) {
        Log.w("PageSnapshots", "Cannot read widget ${placement.id}", e)
        null
    }
    placement.id to info
}

internal class SnapshotPageView(context: Context) : FrameLayout(context) {
    private var labels: List<String> = emptyList()
    private var overflows = false

    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        isFocusable = false
        clipChildren = true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean = true

    fun populate(
        placements: List<WidgetPlacement>,
        infos: Map<Int, AppWidgetProviderInfo?>,
        host: RetainedWidgetHost,
        spacing: WidgetSpacing,
        density: Density,
        direction: LayoutDirection,
        size: IntSize,
    ) {
        removeAllViews()
        labels = placements.map { placement ->
            try {
                infos[placement.id]?.loadLabel(context.packageManager) ?: context.getString(R.string.widget_unavailable)
            } catch (_: RuntimeException) {
                context.getString(R.string.widget_unavailable)
            }
        }
        val padding = with(density) { spacing.outerPaddingDp.dp.roundToPx() }
        val gap = with(density) { spacing.columnGapDp.dp.roundToPx() }
        val rowGap = with(density) { spacing.rowGapDp.dp.roundToPx() }
        val fullWidth = (size.width - padding * 2).coerceAtLeast(0)
        val halfWidth = ((fullWidth - gap) / 2).coerceAtLeast(0)
        val halfDp = with(density) { halfWidth.toDp() }
        val halves = placements.filter { isHalfWidget(it, infos[it.id], halfDp, density) }.map { it.id }.toSet()
        val rows = widgetRows(placements) { it.id in halves }
        var y = padding.toLong()
        for (row in rows) {
            var rowHeight = 0
            for ((column, placement) in listOfNotNull(row.first, row.second).withIndex()) {
                val info = infos[placement.id]
                val heightDp = (placement.heightDp ?: defaultWidgetHeight(info, density) ?: 140)
                    .coerceIn(widgetHeightLimits(info, density))
                val height = with(density) { heightDp.dp.roundToPx() }.coerceAtLeast(1)
                rowHeight = maxOf(rowHeight, height)
                if (y >= size.height || fullWidth == 0) continue
                val width = if (placement.id in halves) halfWidth else fullWidth
                if (width == 0) continue
                val child = hostedView(host, placement, info)
                val logicalX = padding + column * (halfWidth + gap)
                val x = if (direction == LayoutDirection.Rtl) size.width - logicalX - width else logicalX
                addView(child, LayoutParams(width, height).apply {
                    leftMargin = x
                    topMargin = y.toInt()
                })
                if (child is android.appwidget.AppWidgetHostView) {
                    try {
                        child.updateAppWidgetSize(Bundle(), listOf(with(density) { SizeF(width.toDp().value, height.toDp().value) }))
                    } catch (e: RuntimeException) {
                        // A provider/ID may disappear between reading metadata and rendering.
                        Log.w("PageSnapshots", "Cannot size widget ${placement.id}", e)
                        val params = child.layoutParams
                        removeView(child)
                        addView(unavailable(), params)
                    }
                }
            }
            y += rowHeight.toLong() + rowGap
        }
        overflows = rows.isNotEmpty() && y - rowGap + padding > size.height
    }

    private fun hostedView(host: RetainedWidgetHost, placement: WidgetPlacement, info: AppWidgetProviderInfo?): View =
        try {
            if (info == null || info.minHeight <= 0) unavailable()
            else host.obtainView(context, placement.id, info)
        } catch (e: RuntimeException) {
            Log.w("PageSnapshots", "Cannot render widget ${placement.id}", e)
            unavailable()
        }

    private fun unavailable(): TextView = TextView(context).apply {
        text = context.getString(R.string.widget_unavailable)
        setTextColor(Color.WHITE)
        textSize = 16f
        val padding = (20 * resources.displayMetrics.density).roundToInt()
        setPadding(padding, padding, padding, padding)
    }

    fun capture(size: IntSize): WidgetPageSnapshot {
        // Bound bitmap memory, but never measure a provider at these thumbnail dimensions.
        val scale = minOf(1f, 360f / size.width, 900f / size.height)
        val bitmap = Bitmap.createBitmap(
            (size.width * scale).roundToInt().coerceAtLeast(1),
            (size.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        // Explicitly lay out at the normal viewport, including widgets taller than it.
        measure(MeasureSpec.makeMeasureSpec(size.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(size.height, MeasureSpec.EXACTLY))
        layout(0, 0, size.width, size.height)
        try {
            draw(canvas)
        } catch (e: RuntimeException) {
            Log.w("PageSnapshots", "Cannot capture page", e)
            bitmap.eraseColor(Color.TRANSPARENT)
        }
        return WidgetPageSnapshot(bitmap, labels, overflows)
    }
}
