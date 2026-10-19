package app.indexlauncher.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.ItemSpacing
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val actionWidth = 80.dp
private val revealAnimation = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)

/** Only leftward drags claim a closed row. Rightward drags remain available to the widget pager. */
@Composable
fun AppRow(
    app: AppEntry,
    catalogVersion: Int,
    iconShape: IconShape,
    loadIcon: suspend (AppEntry, Int, IconShape) -> Bitmap?,
    isFavorite: Boolean,
    showLabelShadow: Boolean,
    isOpen: Boolean,
    onReveal: (String?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClick: (AppEntry) -> Unit,
    itemSpacing: ItemSpacing = ItemSpacing.Default,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
) {
    val rowHeight = itemSpacing.rowHeightDp.dp
    val bitmap by produceState<Bitmap?>(null, app.id, catalogVersion, iconShape) {
        value = loadIcon(app, catalogVersion, iconShape)
    }
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val labelStyle = if (showLabelShadow) {
        MaterialTheme.typography.titleMedium.copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.50f),
                offset = Offset(0f, with(density) { 1.dp.toPx() }),
                blurRadius = with(density) { 2.dp.toPx() },
            ),
        )
    } else {
        MaterialTheme.typography.titleMedium
    }
    val offset = remember(app.id) { Animatable(0f) }
    var dragging by remember(app.id) { mutableStateOf(false) }
    var dragOffset by remember(app.id) { mutableFloatStateOf(0f) }
    val displayedOffset = if (dragging) dragOffset else offset.value
    val scope = rememberCoroutineScope()
    val reveal by rememberUpdatedState(onReveal)
    val toggle by rememberUpdatedState(onToggleFavorite)
    val actionLabel = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite)
    val workDescription = if (app.profileSerial != null) {
        stringResource(R.string.work_app_description, app.label)
    } else null

    LaunchedEffect(isOpen, actionWidthPx) {
        offset.animateTo(if (isOpen) actionWidthPx else 0f, revealAnimation)
    }

    Box(Modifier.fillMaxWidth().height(rowHeight).clipToBounds()) {
        // Clip the action panel to the part actually uncovered by the row; the row stays transparent.
        if (displayedOffset > 0f) {
            Box(Modifier.align(AbsoluteAlignment.CenterRight)
                .width(with(density) { displayedOffset.toDp() }).height(rowHeight).clipToBounds()) {
                Box(
                    modifier = Modifier
                        .align(AbsoluteAlignment.CenterRight)
                        .requiredWidth(actionWidth)
                        .height(rowHeight)
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    FilledTonalIconButton(onClick = {
                        toggle(app.id)
                        reveal(null)
                    }) {
                        Icon(
                            painterResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline),
                            contentDescription = actionLabel,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .absoluteOffset { IntOffset(-displayedOffset.roundToInt(), 0) }
                .pointerInput(app.id, actionWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var beyondSlop = 0f
                        val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, delta ->
                            if (delta < 0f || offset.value > 0f) {
                                beyondSlop = -delta
                                change.consume()
                            }
                        } ?: return@awaitEachGesture

                        dragOffset = (offset.value + beyondSlop).coerceIn(0f, actionWidthPx)
                        dragging = true
                        var canceled = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == dragStart.id } ?: break
                            if (!change.pressed) break
                            if (change.isConsumed) {
                                canceled = true
                                break
                            }
                            val delta = -change.positionChange().x
                            if (delta != 0f) {
                                change.consume()
                                dragOffset = (dragOffset + delta).coerceIn(0f, actionWidthPx)
                            }
                        }
                        val shouldOpen = !canceled && dragOffset >= actionWidthPx * 0.4f
                        val releasedAt = dragOffset
                        reveal(if (shouldOpen) app.id else null)
                        scope.launch {
                            offset.snapTo(releasedAt)
                            dragging = false
                            offset.animateTo(if (shouldOpen) actionWidthPx else 0f, revealAnimation)
                        }
                    }
                }
                .clickable { if (isOpen) onReveal(null) else onClick(app) }
                .semantics {
                    if (workDescription != null) contentDescription = workDescription
                    // The action is available to accessibility users without requiring a swipe.
                    customActions = listOf(CustomAccessibilityAction(actionLabel) {
                        toggle(app.id)
                        reveal(null)
                        true
                    }) + accessibilityActions
                }
                .padding(horizontal = 24.dp, vertical = itemSpacing.paddingDp.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    // AppIconRepository applies either the device mask or Index Launcher's selected mask.
                    modifier = Modifier.size(ItemSpacing.ICON_SIZE_DP.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(ItemSpacing.ICON_SIZE_DP.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(app.label, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
