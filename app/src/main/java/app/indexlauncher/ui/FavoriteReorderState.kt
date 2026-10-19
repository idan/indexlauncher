package app.indexlauncher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/** A local preview: only a completed drop writes the order, cancellation leaves favorites alone. */
internal class FavoriteReorderState(val listState: LazyListState) {
    var order by mutableStateOf<List<String>?>(null)
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set
    private var top by mutableFloatStateOf(0f)
    private var height = 0
    private var fingerY by mutableFloatStateOf(0f)

    fun start(ids: List<String>, id: String, y: Float): Boolean {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return false
        order = ids
        top = item.offset.toFloat()
        height = item.size
        fingerY = y
        draggedId = id
        return true
    }

    fun dragBy(deltaY: Float) {
        top += deltaY
        fingerY += deltaY
        updateTarget()
    }

    fun translation(id: String): Float {
        if (id != draggedId) return 0f
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return 0f
        return top - item.offset
    }

    fun updateTarget() {
        val ids = order ?: return
        val from = ids.indexOf(draggedId)
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedId } ?: return
        val center = top + height / 2f
        val movingDown = center > item.offset + item.size / 2f
        // Use keys rather than lazy indices: the widget header is not a reorderable favorite.
        val crossed = listState.layoutInfo.visibleItemsInfo.mapNotNull { candidate ->
            val index = ids.indexOf(candidate.key)
            val candidateCenter = candidate.offset + candidate.size / 2f
            index.takeIf {
                if (movingDown) it > from && center > candidateCenter
                else it in 0 until from && center < candidateCenter
            }
        }
        val to = if (movingDown) crossed.maxOrNull() else crossed.minOrNull()
        if (to != null) {
            // Keep the viewport fixed, rather than following the first visible key to its new slot.
            listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            order = ids.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    fun scrollSpeed(edge: Float, maxSpeed: Float): Float {
        val ids = order ?: return 0f
        val index = ids.indexOf(draggedId)
        val layout = listState.layoutInfo
        return when {
            index > 0 && listState.canScrollBackward && fingerY < layout.viewportStartOffset + edge ->
                -maxSpeed * ((layout.viewportStartOffset + edge - fingerY) / edge).coerceIn(0f, 1f)
            index < ids.lastIndex && listState.canScrollForward && fingerY > layout.viewportEndOffset - edge ->
                maxSpeed * ((fingerY - layout.viewportEndOffset + edge) / edge).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    fun finish(): List<String>? {
        val result = order
        draggedId = null
        order = null
        return result
    }
}

@Composable
internal fun rememberFavoriteReorderState(listState: LazyListState): FavoriteReorderState {
    val state = remember(listState) { FavoriteReorderState(listState) }
    val density = LocalDensity.current
    val edge = with(density) { 56.dp.toPx() }
    val maxSpeed = with(density) { 900.dp.toPx() }
    LaunchedEffect(state, edge, maxSpeed) {
        snapshotFlow { state.scrollSpeed(edge, maxSpeed) }.collectLatest { speed ->
            if (speed == 0f) return@collectLatest
            var previousFrame = withFrameNanos { it }
            while (state.draggedId != null) {
                val frame = withFrameNanos { it }
                val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(0.05f)
                previousFrame = frame
                state.listState.scrollBy(speed * seconds)
                state.updateTarget()
            }
        }
    }
    return state
}

/** The detector stays on the list so moving a keyed row never restarts the active gesture. */
@Composable
internal fun Modifier.reorderFavorites(
    state: FavoriteReorderState,
    ids: List<String>,
    enabled: Boolean,
    endInsetPx: Float,
    onStart: () -> Unit,
    onDrop: (List<String>) -> Unit,
): Modifier {
    val currentIds by rememberUpdatedState(ids)
    val start by rememberUpdatedState(onStart)
    val drop by rememberUpdatedState(onDrop)
    val direction = LocalLayoutDirection.current
    return pointerInput(state, ids, enabled, endInsetPx, direction) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val onRail = if (direction == LayoutDirection.Ltr) down.position.x >= size.width - endInsetPx
                else down.position.x < endInsetPx
            if (onRail) return@awaitEachGesture
            val item = state.listState.layoutInfo.visibleItemsInfo.firstOrNull {
                down.position.y >= it.offset && down.position.y < it.offset + it.size && it.key in currentIds
            } ?: return@awaitEachGesture
            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            if (!state.start(currentIds, item.key as String, longPress.position.y)) return@awaitEachGesture
            start()
            try {
                var completed = false
                while (true) {
                    // Claim moves and the final up before row clicks/swipes or ancestor scrolling.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == longPress.id } ?: break
                    if (change.isConsumed) break
                    if (!change.pressed) {
                        change.consume()
                        completed = true
                        break
                    }
                    state.dragBy(change.positionChange().y)
                    change.consume()
                }
                if (completed) state.finish()?.let(drop)
            } finally {
                state.finish()
            }
        }
    }
}
