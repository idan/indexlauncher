package app.indexlauncher.ui

import android.appwidget.AppWidgetManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.indexlauncher.R
import app.indexlauncher.model.PageCounts
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetSpacing
import app.indexlauncher.model.movePage

@Composable
internal fun WidgetPageOverview(
    counts: PageCounts,
    widgets: Map<String, List<WidgetPlacement>>,
    host: RetainedWidgetHost,
    manager: AppWidgetManager,
    spacing: WidgetSpacing,
    version: Int,
    sourceOrdinal: Int,
    onConfirm: (List<Int>) -> Unit,
    onCancel: () -> Unit,
) {
    var snapshots by remember { mutableStateOf<Map<Int, WidgetPageSnapshot>>(emptyMap()) }
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        WidgetPageSnapshotRenderer(
            pages = counts.visibleOrdinals,
            widgets = widgets,
            host = host,
            manager = manager,
            spacing = spacing,
            version = version,
            onReset = { snapshots = emptyMap() },
            onSnapshot = { ordinal, snapshot -> snapshots = snapshots + (ordinal to snapshot) },
            modifier = Modifier.fillMaxSize(),
        )
        PageOrderingContent(counts, snapshots, sourceOrdinal, onConfirm, onCancel)
    }
}

@Composable
internal fun PageOrderingContent(
    counts: PageCounts,
    snapshots: Map<Int, WidgetPageSnapshot> = emptyMap(),
    sourceOrdinal: Int = 0,
    onConfirm: (List<Int>) -> Unit,
    onCancel: () -> Unit,
) {
    // Draft is physical left-to-right, whereas persistence is nearest Home first.
    var draft by rememberSaveable(counts, stateSaver = listSaver(save = { it }, restore = { it })) {
        mutableStateOf(counts.visibleOrdinals.reversed())
    }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (counts.left - 2).coerceAtLeast(0))
    val drag = rememberPageReorderState(list)
    val order = drag.order ?: draft
    val title = stringResource(R.string.reorder_widget_pages)
    BackHandler {
        if (drag.draggedId != null) drag.finish() else onCancel()
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .semantics { paneTitle = title },
    ) {
        val previewHeight = (maxHeight * 0.42f).coerceIn(120.dp, 240.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                stringResource(R.string.reorder_pages_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            // Physical directions match HomePager even in RTL locales.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LazyRow(
                    state = list,
                    modifier = Modifier.fillMaxWidth().testTag("page-order-list")
                        .reorderPages(drag, draft) { draft = it },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(order, key = { it }) { ordinal ->
                        val index = order.indexOf(ordinal)
                        PagePreviewCard(
                            ordinal = ordinal,
                            snapshot = snapshots[ordinal],
                            positionFromHome = order.size - index,
                            count = order.size,
                            previewHeight = previewHeight,
                            selected = sourceOrdinal == ordinal,
                            canMoveAway = index > 0 && drag.draggedId == null,
                            canMoveToward = index < order.lastIndex && drag.draggedId == null,
                            onMove = { direction ->
                                list.requestScrollToItem(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
                                draft = movePage(draft, ordinal, direction)
                            },
                            modifier = Modifier.width(148.dp).zIndex(if (drag.draggedId == ordinal) 1f else 0f)
                                .graphicsLayer { translationX = drag.translation(ordinal) },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { onConfirm(draft.reversed()) }, enabled = drag.draggedId == null) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}

@Composable
private fun PagePreviewCard(
    ordinal: Int,
    snapshot: WidgetPageSnapshot?,
    positionFromHome: Int,
    count: Int,
    previewHeight: Dp,
    selected: Boolean,
    canMoveAway: Boolean,
    canMoveToward: Boolean,
    onMove: (Int) -> Unit,
    modifier: Modifier,
) {
    val away = stringResource(R.string.page_move_away, ordinal)
    val toward = stringResource(R.string.page_move_toward, ordinal)
    val position = stringResource(R.string.page_order_position, positionFromHome, count)
    Column(
        modifier.clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.large)
            .semantics(mergeDescendants = true) {
                stateDescription = position
                customActions = buildList {
                    if (canMoveAway) add(CustomAccessibilityAction(away) { onMove(-1); true })
                    if (canMoveToward) add(CustomAccessibilityAction(toward) { onMove(1); true })
                }
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.widget_page, ordinal), style = MaterialTheme.typography.titleSmall)
        Box(
            Modifier.fillMaxWidth().height(previewHeight).clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            if (snapshot == null) {
                CircularProgressIndicator(Modifier.size(24.dp))
            } else if (snapshot.labels.isEmpty()) {
                Text(stringResource(R.string.empty_widget_page), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            } else {
                Image(snapshot.bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
        Text(
            snapshot?.labels?.joinToString(", ") ?: stringResource(R.string.widget_gallery_loading),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (snapshot?.overflows == true) {
            Text(stringResource(R.string.page_snapshot_more), style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveAway) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = away, modifier = Modifier.size(20.dp))
            }
            Text(positionFromHome.toString(), style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = { onMove(1) }, enabled = canMoveToward) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = toward,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f })
            }
        }
    }
}
