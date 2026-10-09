package app.indexlauncher.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/** Require a deliberate drag beyond the end, not merely a scroll that reaches the last favorite. */
internal class SearchOverscrollDetector(private val thresholdPx: Float) {
    private var distancePx = 0f

    fun onDrag(unconsumedY: Float, canScrollForward: Boolean) {
        distancePx = if (canScrollForward || unconsumedY > 0f) {
            0f
        } else {
            distancePx - unconsumedY
        }
    }

    fun release(canScrollForward: Boolean): Boolean {
        val triggered = !canScrollForward && distancePx >= thresholdPx
        distancePx = 0f
        return triggered
    }

    fun reset() { distancePx = 0f }
}

internal class FavoritesSearchGesture(
    private val listState: LazyListState,
    thresholdPx: Float,
    private val openSearch: () -> Unit,
) : NestedScrollConnection {
    private val detector = SearchOverscrollDetector(thresholdPx)
    private var allowSearch = true

    fun onGestureStart(allowSearch: Boolean) {
        this.allowSearch = allowSearch
        detector.reset()
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (allowSearch && source == NestedScrollSource.UserInput) {
            detector.onDrag(available.y, listState.canScrollForward)
        } else {
            detector.reset()
        }
        return Offset.Zero // Never steal vertical scrolling from favorites (or horizontal paging).
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (detector.release(listState.canScrollForward)) openSearch()
        return Velocity.Zero
    }
}
