package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.indexlauncher.R
import app.indexlauncher.model.HomePage
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetRow
import app.indexlauncher.model.WidgetSpacing
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.model.resizedWidgetHeight
import app.indexlauncher.model.widgetWidthAfterDrag
import app.indexlauncher.model.widgetHeightRange
import app.indexlauncher.model.widgetRows
import kotlin.math.roundToInt

private const val ACCESSIBILITY_HEIGHT_STEP_DP = 40

@Composable
fun WidgetPageScreen(
    page: HomePage.Widgets,
    widgets: List<WidgetPlacement>,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onWidthChange: (Int, WidgetWidth) -> Unit,
    onHeightChange: (Int, Int) -> Unit,
    active: Boolean = true,
    spacing: WidgetSpacing = WidgetSpacing.Default,
) {
    var editing by rememberSaveable(page.key) { mutableStateOf(false) }
    WidgetEditor(
        pageKey = page.key,
        spacing = spacing,
        pageName = stringResource(R.string.widget_page, page.ordinal),
        handleBack = active,
        widgets = widgets,
        host = host,
        manager = manager,
        editing = editing,
        onEditingChange = { editing = it },
        onAddWidget = onAddWidget,
        onRemoveWidget = onRemoveWidget,
        onMoveWidget = onMoveWidget,
        onWidthChange = onWidthChange,
        onHeightChange = onHeightChange,
    )
}

@Composable
fun HomeWidgetRegion(
    widgets: List<WidgetPlacement>,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onWidthChange: (Int, WidgetWidth) -> Unit,
    onHeightChange: (Int, Int) -> Unit,
    active: Boolean = true,
    spacing: WidgetSpacing = WidgetSpacing.Default,
) {
    WidgetEditor(
        pageKey = HomePage.Apps.key,
        spacing = spacing,
        pageName = stringResource(R.string.home_widgets),
        widgets = widgets,
        host = host,
        manager = manager,
        editing = editing,
        onEditingChange = onEditingChange,
        onAddWidget = onAddWidget,
        onRemoveWidget = onRemoveWidget,
        onMoveWidget = onMoveWidget,
        onWidthChange = onWidthChange,
        onHeightChange = onHeightChange,
        isHome = true,
        handleBack = active,
    )
}

@Composable
private fun WidgetEditor(
    pageKey: String,
    pageName: String,
    spacing: WidgetSpacing,
    widgets: List<WidgetPlacement>,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onWidthChange: (Int, WidgetWidth) -> Unit,
    onHeightChange: (Int, Int) -> Unit,
    isHome: Boolean = false,
    handleBack: Boolean = true,
) {
    val pagePadding = spacing.outerPaddingDp.dp
    val columnGap = spacing.columnGapDp.dp
    val rowGap = spacing.rowGapDp.dp
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var pendingRemoval by remember { mutableStateOf<Int?>(null) }
    val addLabel = stringResource(R.string.add_widget)
    val editLabel = stringResource(R.string.edit_widgets)
    val ids = widgets.map { it.id }
    val order = ids.withIndex().associate { (index, id) -> id to index }
    val infos = remember(ids, configuration, manager) {
        ids.associateWith { id ->
            try {
                manager.getAppWidgetInfo(id)
            } catch (e: RuntimeException) {
                Log.w("WidgetPage", "Cannot read widget $id", e)
                null
            }
        }
    }
    BackHandler(handleBack && (editing || pendingRemoval != null)) {
        if (pendingRemoval != null) pendingRemoval = null else onEditingChange(false)
    }
    pendingRemoval?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.remove_widget_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRemoval = null
                    onRemoveWidget(id)
                }) { Text(stringResource(R.string.remove_widget)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Box(
        modifier = (if (isHome) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        })
            .semantics {
                paneTitle = pageName
                customActions = listOf(
                    CustomAccessibilityAction(addLabel) { onAddWidget(); true },
                    CustomAccessibilityAction(editLabel) { onEditingChange(true); true },
                )
            },
    ) {
        BoxWithConstraints(if (isHome) Modifier.fillMaxWidth() else Modifier.fillMaxSize()) {
            val availableWidth = (maxWidth - pagePadding * 2).coerceAtLeast(0.dp)
            val halfWidth = ((availableWidth - columnGap) / 2).coerceAtLeast(0.dp)
            val halfIds = widgets.filter { placement ->
                val info = infos[placement.id]
                when (placement.width) {
                    WidgetWidth.AUTO -> info != null && info.minWidth > 0 &&
                        with(density) { info.minWidth.toDp() <= halfWidth }
                    WidgetWidth.HALF -> supportsHalfWidth(info, halfWidth, density)
                    WidgetWidth.FULL -> false
                }
            }.mapTo(mutableSetOf()) { it.id }
            val rows = widgetRows(widgets) { it.id in halfIds }
            val rowContent: @Composable (WidgetRow) -> Unit = { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                    for (placement in listOfNotNull(row.first, row.second)) {
                        key(placement.id) {
                            val half = placement.id in halfIds
                            HostedWidgetCard(
                                placement = placement,
                                info = infos[placement.id],
                                host = host,
                                editing = editing,
                                allowLongPressEdit = !isHome,
                                halfWidth = halfWidth,
                                isHalf = half,
                                isFirst = order[placement.id] == 0,
                                isLast = order[placement.id] == widgets.lastIndex,
                                onMove = { direction -> onMoveWidget(placement.id, direction) },
                                onWidthChange = { width -> onWidthChange(placement.id, width) },
                                onHeightChange = { height -> onHeightChange(placement.id, height) },
                                onRemove = { pendingRemoval = placement.id },
                                onEdit = { onEditingChange(true) },
                                modifier = if (half) Modifier.width(halfWidth) else Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            if (isHome) {
                // The favorites LazyColumn owns scrolling; a nested scrollable would split the page.
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = pagePadding, vertical = pagePadding),
                    verticalArrangement = Arrangement.spacedBy(rowGap),
                ) {
                    rows.forEach { row -> key(row.first.id) { rowContent(row) } }
                    if (editing) Spacer(Modifier.height(96.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().pointerInput(pageKey, editing) {
                        detectTapGestures(onLongPress = { if (!editing) onAddWidget() })
                    },
                    contentPadding = PaddingValues(
                        start = pagePadding, end = pagePadding, top = pagePadding,
                        bottom = if (editing) 104.dp else pagePadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(rowGap),
                ) {
                    if (widgets.isEmpty()) {
                        item(key = "empty-widgets") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.empty_widget_page), style = MaterialTheme.typography.titleLarge)
                                    Text(stringResource(R.string.empty_widget_page_hint), style = MaterialTheme.typography.bodyLarge)
                                    FilledTonalButton(onClick = onAddWidget) {
                                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.add_widget))
                                    }
                                }
                            }
                        }
                    }
                    items(rows, key = { "row-${it.first.id}" }) { row -> rowContent(row) }
                }
            }
            if (editing) {
                Row(
                    Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = onAddWidget) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.add_widget))
                    }
                    TextButton(onClick = { onEditingChange(false) }) { Text(stringResource(R.string.done)) }
                }
            }
        }
    }
}

private fun supportsHalfWidth(info: AppWidgetProviderInfo?, halfWidth: Dp, density: Density): Boolean {
    if (info == null || halfWidth <= 0.dp) return false
    val minWidth = if (info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0 &&
        info.minResizeWidth > 0
    ) info.minResizeWidth else info.minWidth
    return minWidth > 0 && with(density) { minWidth.toDp() <= halfWidth }
}

@Composable
private fun HostedWidgetCard(
    placement: WidgetPlacement,
    info: AppWidgetProviderInfo?,
    host: AppWidgetHost,
    editing: Boolean,
    allowLongPressEdit: Boolean,
    halfWidth: Dp,
    isHalf: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onMove: (Int) -> Unit,
    onWidthChange: (WidgetWidth) -> Unit,
    onHeightChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val defaultHeight = if (info != null && info.minHeight > 0) {
        with(density) { info.minHeight.toDp().value.roundToInt().coerceAtLeast(1) }
    } else null
    val resizable = info != null && info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
    val heightRange = widgetHeightRange(
        defaultDp = defaultHeight ?: 140,
        verticallyResizable = resizable,
        minResizeDp = info?.minResizeHeight?.takeIf { it > 0 }?.let {
            with(density) { it.toDp().value.roundToInt().coerceAtLeast(1) }
        },
        maxResizeDp = info?.maxResizeHeight?.takeIf { it > 0 }?.let {
            with(density) { it.toDp().value.roundToInt().coerceAtLeast(1) }
        },
    )
    val minHeight = heightRange.first
    val maxHeight = heightRange.last
    val savedHeight = (placement.heightDp ?: defaultHeight ?: minHeight).coerceIn(minHeight, maxHeight)
    var heightDragPx by remember(placement.id, placement.heightDp, density.density) { mutableFloatStateOf(0f) }
    var draggingHeight by remember(placement.id) { mutableStateOf(false) }
    var widthDragPx by remember(placement.id, isHalf) { mutableFloatStateOf(0f) }
    var draggingWidth by remember(placement.id) { mutableStateOf(false) }
    val height = if (draggingHeight) {
        resizedWidgetHeight(savedHeight, heightDragPx, density.density, heightRange)
    } else savedHeight
    val canMakeHalf = supportsHalfWidth(info, halfWidth, density)
    val outward = widthDragPx / density.density * if (layoutDirection == LayoutDirection.Ltr) 1 else -1
    val widthTarget = widgetWidthAfterDrag(isHalf, canMakeHalf, outward)
    var measuredSize by remember(placement.id) { mutableStateOf(IntSize.Zero) }
    var reported by remember(placement.id) { mutableStateOf<Pair<AppWidgetHostView, SizeF>?>(null) }
    val editLabel = stringResource(R.string.edit_widgets)
    val tallerLabel = stringResource(R.string.widget_taller)
    val shorterLabel = stringResource(R.string.widget_shorter)
    val heightHandleLabel = stringResource(R.string.widget_drag_height)
    val widthHandleLabel = stringResource(R.string.widget_drag_width)

    Column(
        modifier = if (editing) modifier
            .background(Color.Black.copy(alpha = 0.65f), MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), MaterialTheme.shapes.large)
        else modifier,
    ) {
        Box(Modifier.fillMaxWidth().height(height.dp)) {
            if (info == null || defaultHeight == null) {
                Box(
                    Modifier.fillMaxSize()
                        .then(if (allowLongPressEdit) Modifier.pointerInput(placement.id) {
                            detectTapGestures(onLongPress = { onEdit() })
                        } else Modifier)
                        .semantics {
                            customActions = listOf(CustomAccessibilityAction(editLabel) { onEdit(); true })
                        },
                ) {
                    Text(stringResource(R.string.widget_unavailable), modifier = Modifier.padding(20.dp))
                }
            } else {
                AndroidView(
                    factory = { viewContext ->
                        LongPressWidgetFrame(viewContext).apply {
                            val child = try {
                                host.createView(viewContext, placement.id, info)
                            } catch (e: RuntimeException) {
                                Log.w("WidgetPage", "Cannot display widget ${placement.id}", e)
                                TextView(viewContext).apply {
                                    text = context.getString(R.string.widget_unavailable)
                                    setTextColor(AndroidColor.WHITE)
                                }
                            }
                            addView(child, ViewGroup.LayoutParams(-1, -1))
                        }
                    },
                    update = { frame ->
                        frame.isEditing = editing
                        frame.onWidgetLongPress = if (allowLongPressEdit) onEdit else null
                        val view = frame.getChildAt(0)
                        if (view is AppWidgetHostView && measuredSize.width > 0 && measuredSize.height > 0) {
                            val size = with(density) {
                                SizeF(measuredSize.width.toDp().value, measuredSize.height.toDp().value)
                            }
                            if (reported?.first !== view || reported?.second != size) {
                                view.updateAppWidgetSize(Bundle(), listOf(size))
                                reported = view to size
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize().onSizeChanged { measuredSize = it }
                        .semantics {
                            customActions = listOf(CustomAccessibilityAction(editLabel) { onEdit(); true })
                        },
                )
            }
            if (editing && info != null) {
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .size(width = 64.dp, height = 48.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .pointerInput(placement.id, savedHeight, heightRange, density.density) {
                            detectDragGestures(
                                onDragStart = { draggingHeight = true; heightDragPx = 0f },
                                onDragEnd = {
                                    val next = resizedWidgetHeight(savedHeight, heightDragPx, density.density, heightRange)
                                    if (next != savedHeight) onHeightChange(next)
                                    draggingHeight = false
                                    heightDragPx = 0f
                                },
                                onDragCancel = { draggingHeight = false; heightDragPx = 0f },
                            ) { change, drag ->
                                heightDragPx += drag.y
                                change.consume()
                            }
                        }
                        .semantics {
                            contentDescription = heightHandleLabel
                            onClick(label = if (savedHeight < maxHeight) tallerLabel else shorterLabel) {
                                if (savedHeight < maxHeight) {
                                    onHeightChange((savedHeight.toLong() + ACCESSIBILITY_HEIGHT_STEP_DP)
                                        .coerceAtMost(maxHeight.toLong()).toInt())
                                } else if (savedHeight > minHeight) {
                                    onHeightChange((savedHeight - ACCESSIBILITY_HEIGHT_STEP_DP).coerceAtLeast(minHeight))
                                }
                                true
                            }
                            customActions = buildList {
                                if (savedHeight < maxHeight) add(CustomAccessibilityAction(tallerLabel) {
                                    onHeightChange((savedHeight.toLong() + ACCESSIBILITY_HEIGHT_STEP_DP)
                                        .coerceAtMost(maxHeight.toLong()).toInt())
                                    true
                                })
                                if (savedHeight > minHeight) add(CustomAccessibilityAction(shorterLabel) {
                                    onHeightChange((savedHeight - ACCESSIBILITY_HEIGHT_STEP_DP).coerceAtLeast(minHeight))
                                    true
                                })
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, MaterialTheme.shapes.small))
                }
                if (isHalf || canMakeHalf) {
                    Box(
                        Modifier.align(Alignment.CenterEnd)
                            .size(width = 48.dp, height = 64.dp)
                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                            .pointerInput(placement.id, isHalf, canMakeHalf, density.density, layoutDirection) {
                                detectDragGestures(
                                    onDragStart = { draggingWidth = true; widthDragPx = 0f },
                                    onDragEnd = {
                                        val direction = if (layoutDirection == LayoutDirection.Ltr) 1 else -1
                                        widgetWidthAfterDrag(
                                            isHalf, canMakeHalf, widthDragPx / density.density * direction,
                                        )?.let(onWidthChange)
                                        draggingWidth = false
                                        widthDragPx = 0f
                                    },
                                    onDragCancel = { draggingWidth = false; widthDragPx = 0f },
                                ) { change, drag ->
                                    widthDragPx += drag.x
                                    change.consume()
                                }
                            }
                            .semantics {
                                contentDescription = widthHandleLabel
                                onClick(label = widthHandleLabel) {
                                    onWidthChange(if (isHalf) WidgetWidth.FULL else WidgetWidth.HALF)
                                    true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(width = 4.dp, height = 32.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, MaterialTheme.shapes.small))
                    }
                }
                if (draggingHeight || draggingWidth) {
                    Text(
                        if (draggingHeight) stringResource(R.string.widget_height, height)
                        else stringResource(
                            if ((widthTarget ?: if (isHalf) WidgetWidth.HALF else WidgetWidth.FULL) == WidgetWidth.FULL) {
                                R.string.widget_full_width
                            } else R.string.widget_half_width,
                        ),
                        modifier = Modifier.align(Alignment.TopCenter)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                            .padding(8.dp),
                    )
                }
            }
        }
        if (editing) {
            if (!resizable && info != null) {
                Text(
                    stringResource(R.string.widget_height_may_not_adapt),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            if (!isHalf && !canMakeHalf && info != null) {
                Text(
                    stringResource(R.string.widget_half_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(enabled = !isFirst, onClick = { onMove(-1) }) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.widget_move_up),
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 90f },
                    )
                }
                IconButton(enabled = !isLast, onClick = { onMove(1) }) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.widget_move_down),
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = -90f },
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.remove_widget),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
