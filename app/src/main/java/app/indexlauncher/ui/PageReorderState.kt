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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/** Dragging previews an order; a cancelled gesture leaves the overview's draft alone. */
internal class PageReorderState(val list: LazyListState) {
    var order by mutableStateOf<List<Int>?>(null)
        private set
    var draggedId by mutableStateOf<Int?>(null)
        private set
    private var left by mutableFloatStateOf(0f)
    private var fingerX by mutableFloatStateOf(0f)
    private var width = 0

    fun start(ids: List<Int>, x: Float) {
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { x >= it.offset && x < it.offset + it.size && it.key in ids } ?: return
        draggedId = item.key as Int
        order = ids
        left = item.offset.toFloat()
        fingerX = x
        width = item.size
    }

    fun dragBy(dx: Float) {
        left += dx
        fingerX += dx
        updateTarget()
    }

    fun translation(id: Int): Float {
        if (id != draggedId) return 0f
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return 0f
        return left - item.offset
    }

    fun updateTarget() {
        val ids = order ?: return
        val from = ids.indexOf(draggedId)
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedId } ?: return
        val center = left + width / 2f
        val rightward = center > item.offset + item.size / 2f
        val crossed = list.layoutInfo.visibleItemsInfo.mapNotNull { candidate ->
            val index = ids.indexOf(candidate.key)
            val candidateCenter = candidate.offset + candidate.size / 2f
            index.takeIf {
                if (rightward) it > from && center > candidateCenter
                else it in 0 until from && center < candidateCenter
            }
        }
        val to = if (rightward) crossed.maxOrNull() else crossed.minOrNull()
        if (to != null) {
            list.requestScrollToItem(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
            order = ids.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    fun scrollSpeed(edge: Float, speed: Float): Float {
        val ids = order ?: return 0f
        val index = ids.indexOf(draggedId)
        val layout = list.layoutInfo
        return when {
            index > 0 && list.canScrollBackward && fingerX < layout.viewportStartOffset + edge ->
                -speed * ((layout.viewportStartOffset + edge - fingerX) / edge).coerceIn(0f, 1f)
            index < ids.lastIndex && list.canScrollForward && fingerX > layout.viewportEndOffset - edge ->
                speed * ((fingerX - layout.viewportEndOffset + edge) / edge).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    fun finish(): List<Int>? {
        val result = order
        draggedId = null
        order = null
        return result
    }
}

@Composable
internal fun rememberPageReorderState(list: LazyListState): PageReorderState {
    val state = remember(list) { PageReorderState(list) }
    val density = LocalDensity.current
    val edge = with(density) { 48.dp.toPx() }
    val speed = with(density) { 800.dp.toPx() }
    LaunchedEffect(state, edge, speed) {
        snapshotFlow { state.scrollSpeed(edge, speed) }.collectLatest { velocity ->
            if (velocity == 0f) return@collectLatest
            var previous = withFrameNanos { it }
            while (state.draggedId != null) {
                val frame = withFrameNanos { it }
                list.scrollBy(velocity * ((frame - previous) / 1_000_000_000f).coerceAtMost(0.05f))
                previous = frame
                state.updateTarget()
            }
        }
    }
    return state
}

@Composable
internal fun Modifier.reorderPages(state: PageReorderState, ids: List<Int>, onDrop: (List<Int>) -> Unit): Modifier {
    val currentIds by rememberUpdatedState(ids)
    val drop by rememberUpdatedState(onDrop)
    // Stable detector: moving keyed cards must not cancel/restart a long-press gesture.
    return pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            state.start(currentIds, longPress.position.x)
            if (state.draggedId == null) return@awaitEachGesture
            try {
                var completed = false
                while (true) {
                    // Claim moves/up before the LazyRow or arrow buttons can consume them.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == longPress.id } ?: break
                    if (change.isConsumed) break
                    if (!change.pressed) {
                        change.consume()
                        completed = true
                        break
                    }
                    state.dragBy(change.positionChange().x)
                    change.consume()
                }
                if (completed) state.finish()?.let(drop)
            } finally {
                state.finish()
            }
        }
    }
}
