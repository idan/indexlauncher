package app.indexlauncher.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.model.widgetHeightRange
import kotlin.math.roundToInt

internal fun supportsHalfWidth(info: AppWidgetProviderInfo?, halfWidth: Dp, density: Density): Boolean {
    if (info == null || halfWidth <= 0.dp) return false
    val minWidth = if (info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0 && info.minResizeWidth > 0) {
        info.minResizeWidth
    } else info.minWidth
    return minWidth > 0 && with(density) { minWidth.toDp() <= halfWidth }
}

internal fun isHalfWidget(placement: WidgetPlacement, info: AppWidgetProviderInfo?, halfWidth: Dp, density: Density): Boolean =
    when (placement.width) {
        WidgetWidth.AUTO -> info != null && info.minWidth > 0 && with(density) { info.minWidth.toDp() <= halfWidth }
        WidgetWidth.HALF -> supportsHalfWidth(info, halfWidth, density)
        WidgetWidth.FULL -> false
    }

internal fun defaultWidgetHeight(info: AppWidgetProviderInfo?, density: Density): Int? =
    info?.minHeight?.takeIf { it > 0 }?.let { with(density) { it.toDp().value.roundToInt().coerceAtLeast(1) } }

internal fun widgetHeightLimits(info: AppWidgetProviderInfo?, density: Density): IntRange = widgetHeightRange(
    defaultDp = defaultWidgetHeight(info, density) ?: 140,
    verticallyResizable = info != null && info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0,
    minResizeDp = info?.minResizeHeight?.takeIf { it > 0 }?.let {
        with(density) { it.toDp().value.roundToInt().coerceAtLeast(1) }
    },
    maxResizeDp = info?.maxResizeHeight?.takeIf { it > 0 }?.let {
        with(density) { it.toDp().value.roundToInt().coerceAtLeast(1) }
    },
)
