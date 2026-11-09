package app.indexlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

internal enum class RailSide { Left, Right }

/** Left scrubs follow touch Y but anchor beside the right rail; right scrubs retain finger-following X. */
internal fun railBubbleOffset(
    widthPx: Int,
    heightPx: Int,
    railWidthPx: Int,
    bubbleSizePx: Int,
    horizontalDistancePx: Int,
    railGapPx: Int,
    finger: Offset,
    side: RailSide,
): IntOffset {
    val x = when (side) {
        RailSide.Left -> (widthPx - railWidthPx - railGapPx - bubbleSizePx).toFloat()
        RailSide.Right -> widthPx - railWidthPx + finger.x - horizontalDistancePx - bubbleSizePx / 2f
    }
    return IntOffset(
        x.roundToInt().coerceIn(0, (widthPx - bubbleSizePx).coerceAtLeast(0)),
        (finger.y - bubbleSizePx / 2f).roundToInt().coerceIn(0, (heightPx - bubbleSizePx).coerceAtLeast(0)),
    )
}
