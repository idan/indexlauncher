package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.ItemSpacing
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetSpacing
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.model.favoriteApps
import app.indexlauncher.model.groupAppsByLetter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val railWidth = 48.dp
private val railBottomInset = 54.dp // Keep the lowest entries away from the phone's corner.
private val bubbleSize = 72.dp
private val bubbleHorizontalDistance = 92.dp
private const val RAIL_IDLE_DELAY_MS = 3_000L
private const val RAIL_FADE_DURATION_MS = 300

private data class RailScrub(val target: AlphabetRailTarget, val finger: Offset)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    state: LauncherUiState,
    favoriteIds: List<String>,
    homeWidgets: List<WidgetPlacement>,
    widgetHost: AppWidgetHost,
    widgetManager: AppWidgetManager,
    suppressRail: Boolean,
    onRailInteraction: () -> Unit,
    onScrimOpacityChange: (Float) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onReorderFavorites: (List<String>) -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onLoadIcon: suspend (AppEntry, Int, IconShape) -> Bitmap?,
    iconShape: IconShape,
    onRetry: () -> Unit,
    onAddHomeWidget: () -> Unit,
    onRemoveHomeWidget: (Int) -> Unit,
    onMoveHomeWidget: (Int, Int) -> Unit,
    onHomeWidgetWidthChange: (Int, WidgetWidth) -> Unit,
    onHomeWidgetHeightChange: (Int, Int) -> Unit,
    active: Boolean = true,
    itemSpacing: ItemSpacing = ItemSpacing.Default,
    homeRequest: Int = 0,
    widgetSpacing: WidgetSpacing = WidgetSpacing.Default,
) {
    val sections = remember(state.apps) { groupAppsByLetter(state.apps) }
    val favorites = remember(state.apps, favoriteIds) { favoriteApps(state.apps, favoriteIds) }
    val railTargets = remember(sections) { alphabetRailTargets(sections) }
    val jumpPositions = remember(sections) { sectionJumpPositions(sections) }
    val favoritesListState = rememberLazyListState()
    val favoriteReorder = rememberFavoriteReorderState(favoritesListState)
    val favoriteKeys = remember(favorites) { favorites.map { it.id } }
    val favoritesById = remember(favorites) { favorites.associateBy { it.id } }
    val displayedFavorites = favoriteReorder.order?.mapNotNull(favoritesById::get) ?: favorites
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val moveUpLabel = stringResource(R.string.move_favorite_up)
    val moveDownLabel = stringResource(R.string.move_favorite_down)
    val allAppsListState = rememberLazyListState()
    var homeEditing by rememberSaveable { mutableStateOf(false) }
    var allAppsVisible by rememberSaveable { mutableStateOf(false) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var openRowId by remember { mutableStateOf<String?>(null) }
    var scrub by remember { mutableStateOf<RailScrub?>(null) }
    var railExpanded by remember { mutableStateOf(true) }
    var handledHomeRequest by rememberSaveable { mutableIntStateOf(0) }

    fun showFavorites(collapseRail: Boolean = true) {
        // Reset even when already on favorites, including offsets inside an oversized widget.
        favoriteReorder.finish()
        favoritesListState.requestScrollToItem(0)
        allAppsVisible = false
        searchVisible = false
        openRowId = null
        if (collapseRail) railExpanded = false
    }

    LaunchedEffect(homeRequest) {
        if (homeRequest != handledHomeRequest) {
            handledHomeRequest = homeRequest
            showFavorites()
            homeEditing = false
            scrub = null
        }
    }
    val openSearch by rememberUpdatedState {
        searchVisible = true
        openRowId = null
        railExpanded = false
    }
    val editWidgetLabel = stringResource(R.string.edit_widgets)
    val fadeSpec = tween<Float>(durationMillis = RAIL_FADE_DURATION_MS, easing = FastOutSlowInEasing)
    val hasRail = state.apps.isNotEmpty()
    val railOpacity by animateFloatAsState(
        targetValue = when {
            !hasRail || searchVisible -> 0f
            railExpanded -> 1f
            allAppsVisible -> 0.5f
            else -> 0f
        },
        animationSpec = fadeSpec,
        label = "Alphabet rail fade",
    )
    val scrimOpacity = animateFloatAsState(
        targetValue = if (hasRail && (searchVisible || allAppsVisible)) 1f else 0f,
        animationSpec = fadeSpec,
        label = "Wallpaper scrim fade",
    )
    // The scrim is drawn by HomePager in screen space so it fades, rather than slides, between pages.
    val reportScrimOpacity by rememberUpdatedState(onScrimOpacityChange)
    LaunchedEffect(scrimOpacity) {
        snapshotFlow { scrimOpacity.value }.collect { reportScrimOpacity(it) }
    }

    // Home editing must still handle Back when its lazy widget header is offscreen.
    BackHandler(enabled = active && (searchVisible || allAppsVisible || openRowId != null || homeEditing || favoriteReorder.draggedId != null)) {
        when {
            favoriteReorder.draggedId != null -> favoriteReorder.finish()
            searchVisible -> searchVisible = false
            openRowId != null -> openRowId = null
            allAppsVisible -> {
                allAppsVisible = false
                railExpanded = false
            }
            else -> homeEditing = false
        }
    }

    fun openAllAppsAt(letter: Char) {
        allAppsVisible = true
        openRowId = null
        allAppsListState.requestScrollToItem(jumpPositions.getValue(letter))
    }

    fun openAllAppsTools() {
        allAppsVisible = true
        openRowId = null
        allAppsListState.requestScrollToItem(sections.sumOf { 1 + it.apps.size } + 2)
    }

    val favoritesContent: @Composable () -> Unit = {
        val favoriteModifier = Modifier.padding(end = if (hasRail) railWidth else 0.dp)
        LazyColumn(
            state = favoritesListState,
            modifier = Modifier.fillMaxSize()
                .reorderFavorites(
                    state = favoriteReorder,
                    ids = favoriteKeys,
                    enabled = active && !homeEditing,
                    endInsetPx = with(density) { (if (hasRail) railWidth else 0.dp).toPx() },
                    onStart = {
                        openRowId = null
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrop = { if (it != favoriteKeys) onReorderFavorites(it) },
                )
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(editWidgetLabel) {
                        homeEditing = true
                        true
                    })
                },
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            if (homeWidgets.isNotEmpty() || homeEditing) {
                item(key = "home-widgets") {
                    HomeWidgetRegion(
                        widgets = homeWidgets,
                        spacing = widgetSpacing,
                        host = widgetHost,
                        manager = widgetManager,
                        editing = homeEditing,
                        onEditingChange = { homeEditing = it },
                        onAddWidget = onAddHomeWidget,
                        onRemoveWidget = onRemoveHomeWidget,
                        onMoveWidget = onMoveHomeWidget,
                        onWidthChange = onHomeWidgetWidthChange,
                        onHeightChange = onHomeWidgetHeightChange,
                        active = active,
                    )
                }
            }
            if (favorites.isEmpty()) {
                item(key = "empty-favorites") {
                    Box(favoriteModifier) {
                        EmptyFavorites(
                            hasApps = state.apps.isNotEmpty(),
                            onBrowseApps = { allAppsVisible = true },
                        )
                    }
                }
            }
            items(displayedFavorites, key = { it.id }) { app ->
                val dragging = favoriteReorder.draggedId == app.id
                val index = favoriteKeys.indexOf(app.id)
                val moveActions = buildList {
                    fun moveTo(target: Int): Boolean {
                        openRowId = null
                        onReorderFavorites(favoriteKeys.toMutableList().apply { add(target, removeAt(index)) })
                        return true
                    }
                    if (index > 0) add(CustomAccessibilityAction(moveUpLabel) { moveTo(index - 1) })
                    if (index < favoriteKeys.lastIndex) add(CustomAccessibilityAction(moveDownLabel) { moveTo(index + 1) })
                }
                Box(
                    favoriteModifier
                        .zIndex(if (dragging) 1f else 0f)
                        .then(if (dragging) Modifier else Modifier.animateItem())
                        .graphicsLayer { translationY = favoriteReorder.translation(app.id) }
                        .then(if (dragging) Modifier
                            .shadow(8.dp, MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                        else Modifier),
                ) {
                    AppRow(
                        app = app,
                        catalogVersion = state.iconCatalogVersion,
                        iconShape = iconShape,
                        loadIcon = onLoadIcon,
                        itemSpacing = itemSpacing,
                        isFavorite = true,
                        showLabelShadow = true,
                        isOpen = openRowId == app.id,
                        onReveal = { openRowId = it },
                        onToggleFavorite = onToggleFavorite,
                        onClick = onAppClick,
                        accessibilityActions = moveActions,
                    )
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.apps.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.loadFailed && state.apps.isEmpty() -> {
                    EmptyState(stringResource(R.string.load_error), onRetry)
                }
                searchVisible -> {
                    SearchScreen(
                        apps = state.apps,
                        favoriteIds = favoriteIds,
                        catalogVersion = state.iconCatalogVersion,
                        iconShape = iconShape,
                        loadIcon = onLoadIcon,
                        itemSpacing = itemSpacing,
                        onToggleFavorite = onToggleFavorite,
                        onAppClick = onAppClick,
                        onDismiss = { searchVisible = false },
                        active = active,
                    )
                }
                else -> {
                    Box(Modifier.fillMaxSize()) {
                        if (allAppsVisible) {
                            Box(Modifier.fillMaxSize().padding(end = if (hasRail) railWidth else 0.dp)) {
                                LazyColumn(
                                    state = allAppsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                                ) {
                                    if (state.apps.isEmpty()) {
                                        item(key = "empty-apps") {
                                            Text(
                                                text = stringResource(R.string.no_apps),
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp),
                                            )
                                        }
                                    }
                                    sections.forEach { section ->
                                        stickyHeader(key = "section-${section.letter}") {
                                            // Keep the sticky header transparent; only the letter gets a dark pill.
                                            Box(
                                                Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
                                            ) {
                                                Text(
                                                    text = section.letter.toString(),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .background(Color.Black.copy(alpha = 0.68f), CircleShape)
                                                        .padding(horizontal = 18.dp, vertical = 6.dp),
                                                )
                                            }
                                        }
                                        items(section.apps, key = { it.id }) { app ->
                                            AppRow(
                                                app = app,
                                                catalogVersion = state.iconCatalogVersion,
                                                iconShape = iconShape,
                                                itemSpacing = itemSpacing,
                                                loadIcon = onLoadIcon,
                                                isFavorite = app.id in favoriteIds,
                                                showLabelShadow = false,
                                                isOpen = openRowId == app.id,
                                                onReveal = { openRowId = it },
                                                onToggleFavorite = onToggleFavorite,
                                                onClick = onAppClick,
                                            )
                                        }
                                    }
                                    item(key = "settings-gap") { Spacer(Modifier.height(24.dp)) }
                                    item(key = "index-launcher-settings") {
                                        LauncherActionRow(
                                            title = stringResource(R.string.settings_tile_title),
                                            iconRes = R.drawable.ic_settings_outline,
                                            onClick = onOpenSettings,
                                        )
                                    }
                                    item(key = "homescreen-widgets") {
                                        LauncherActionRow(
                                            title = stringResource(R.string.home_widgets_tile_title),
                                            iconRes = R.drawable.ic_add,
                                            onClick = {
                                                allAppsVisible = false
                                                homeEditing = true
                                                railExpanded = false
                                            },
                                        )
                                    }
                                }
                                FloatingActionButton(
                                    onClick = { openSearch() },
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                                ) {
                                    Icon(painterResource(R.drawable.ic_search), contentDescription = stringResource(R.string.search_apps))
                                }
                            }
                        } else {
                            favoritesContent()
                        }
                        if (state.apps.isNotEmpty()) {
                            LetterRail(
                                modifier = Modifier.align(Alignment.TopEnd),
                                targets = railTargets,
                                activeTarget = scrub?.target
                                    ?: AlphabetRailTarget.Favorites.takeUnless { allAppsVisible },
                                onScrub = { target, finger ->
                                    if (scrub?.target != target) {
                                        when (target) {
                                            AlphabetRailTarget.Favorites -> showFavorites(collapseRail = false)
                                            is AlphabetRailTarget.Section -> openAllAppsAt(target.letter)
                                            AlphabetRailTarget.Settings -> openAllAppsTools()
                                        }
                                    }
                                    scrub = RailScrub(target, finger)
                                },
                                onEnd = { scrub = null },
                                onSelect = { target ->
                                    when (target) {
                                        AlphabetRailTarget.Favorites -> showFavorites()
                                        is AlphabetRailTarget.Section -> openAllAppsAt(target.letter)
                                        AlphabetRailTarget.Settings -> openAllAppsTools()
                                    }
                                },
                                opacity = if (suppressRail) 0f else railOpacity,
                                onExpandedChange = { expanded ->
                                    if (expanded) onRailInteraction()
                                    railExpanded = expanded
                                },
                            )
                        }
                    }
                }
            }
        }

        // Keep the bubble directly left of the finger, vertically centered on the active target.
        scrub?.takeUnless { suppressRail }?.let { active ->
            val density = LocalDensity.current
            val bubblePx = with(density) { bubbleSize.roundToPx() }
            val distancePx = with(density) { bubbleHorizontalDistance.roundToPx() }
            val railPx = with(density) { railWidth.roundToPx() }
            val x = (constraints.maxWidth - railPx + active.finger.x - distancePx - bubblePx / 2f)
                .roundToInt().coerceIn(0, (constraints.maxWidth - bubblePx).coerceAtLeast(0))
            val y = (active.finger.y - bubblePx / 2f)
                .roundToInt().coerceIn(0, (constraints.maxHeight - bubblePx).coerceAtLeast(0))

            Box(
                modifier = Modifier
                    .offset { IntOffset(x, y) }
                    .size(bubbleSize)
                    .shadow(8.dp, CircleShape)
                    .background(Color.Black.copy(alpha = 0.78f), CircleShape)
                    .clearAndSetSemantics { }, // The rail already announces the selected target.
                contentAlignment = Alignment.Center,
            ) {
                when (val target = active.target) {
                    AlphabetRailTarget.Favorites -> Icon(
                        painterResource(R.drawable.ic_star),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                    is AlphabetRailTarget.Section -> Text(
                        target.letter.toString(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    AlphabetRailTarget.Settings -> Icon(
                        painterResource(R.drawable.ic_settings_outline),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFavorites(hasApps: Boolean, onBrowseApps: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(if (hasApps) R.string.no_favorites else R.string.no_apps),
                style = MaterialTheme.typography.titleLarge,
            )
            if (hasApps) {
                Text(stringResource(R.string.favorites_hint), style = MaterialTheme.typography.bodyLarge)
                FilledTonalButton(onClick = onBrowseApps) { Text(stringResource(R.string.browse_apps)) }
            }
        }
    }
}

@Composable
private fun LauncherActionRow(title: String, @DrawableRes iconRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LetterRail(
    modifier: Modifier,
    targets: List<AlphabetRailTarget>,
    activeTarget: AlphabetRailTarget?,
    onScrub: (AlphabetRailTarget, Offset) -> Unit,
    onEnd: () -> Unit,
    onSelect: (AlphabetRailTarget) -> Unit,
    opacity: Float,
    onExpandedChange: (Boolean) -> Unit,
) {
    val scrub by rememberUpdatedState(onScrub)
    val end by rememberUpdatedState(onEnd)
    val select by rememberUpdatedState(onSelect)
    val changeExpanded by rememberUpdatedState(onExpandedChange)
    val hapticFeedback by rememberUpdatedState(LocalHapticFeedback.current)
    var scrubbing by remember { mutableStateOf(false) }
    var idleRestart by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val glyphHeightPx = with(density) {
        maxOf(16.dp.toPx(), MaterialTheme.typography.labelMedium.lineHeight.toPx())
    }
    val bottomInsetPx = with(density) { railBottomInset.toPx() }

    LaunchedEffect(scrubbing, idleRestart) {
        if (!scrubbing) {
            delay(RAIL_IDLE_DELAY_MS)
            changeExpanded(false)
        }
    }

    BoxWithConstraints(modifier.width(railWidth).fillMaxHeight()) {
        val track = railTrack(constraints.maxHeight, targets.size, glyphHeightPx, bottomInsetPx)
        Box(
            Modifier.fillMaxSize().pointerInput(targets, track) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    scrubbing = true
                    changeExpanded(true)
                    var lastTarget: AlphabetRailTarget? = null
                    var released = false
                    fun update(position: Offset) {
                        railTargetAtOffset(targets, position.y, track)?.let { target ->
                            if (target != lastTarget) {
                                // A soft selection tick, not a pulse for every pointer movement.
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                lastTarget = target
                            }
                            scrub(target, position)
                        }
                    }
                    try {
                        update(down.position)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                released = true
                                break
                            }
                            change.consume()
                            update(change.position)
                        }
                    } finally {
                        val completedTarget = lastTarget.takeIf { released }
                        end()
                        scrubbing = false
                        if (completedTarget == AlphabetRailTarget.Favorites) {
                            // Returning home should clear the index without waiting for the idle delay.
                            changeExpanded(false)
                        } else {
                            idleRestart++ // Start the idle timeout after the finger leaves the rail.
                        }
                    }
                }
            },
        ) {
        // The full-height invisible touch area remains active over any home widget beneath it.
        Column(
            Modifier
                .fillMaxWidth()
                .height(with(density) { track.heightPx.toDp() })
                .align(Alignment.TopCenter)
                .offset(y = with(density) { track.topPx.toDp() })
                .alpha(opacity),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            targets.forEach { target ->
                val clickLabel = when (target) {
                    AlphabetRailTarget.Favorites -> stringResource(R.string.show_favorites)
                    is AlphabetRailTarget.Section -> stringResource(R.string.jump_to_letter, target.letter.toString())
                    AlphabetRailTarget.Settings -> stringResource(R.string.show_launcher_tools)
                }
                val selected = activeTarget == target
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clearAndSetSemantics {
                            contentDescription = clickLabel
                            onClick {
                                changeExpanded(true)
                                idleRestart++ // Accessibility clicks also reset the timeout.
                                select(target)
                                true
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (target) {
                            AlphabetRailTarget.Favorites -> Icon(
                                painterResource(R.drawable.ic_star),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(16.dp),
                            )
                            is AlphabetRailTarget.Section -> Text(
                                target.letter.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onBackground,
                            )
                            AlphabetRailTarget.Settings -> Icon(
                                painterResource(R.drawable.ic_settings_outline),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
