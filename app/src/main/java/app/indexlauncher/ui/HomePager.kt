package app.indexlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import app.indexlauncher.R
import app.indexlauncher.model.HomeFold
import app.indexlauncher.model.HomePage
import app.indexlauncher.model.PageCounts
import app.indexlauncher.model.MAX_WIDGET_PAGES
import app.indexlauncher.model.homePaneSplit
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SCRIM_MAX_ALPHA = 0.60f
private const val SCRIM_PAGE_FADE_DISTANCE = 1f / 3f

/** Physical left-growing paging in compact windows; independently paged widgets beside pinned apps. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePager(
    counts: PageCounts,
    appPage: @Composable (
        suppressRail: Boolean,
        active: Boolean,
        onRailInteraction: () -> Unit,
        onScrimOpacityChange: (Float) -> Unit,
    ) -> Unit,
    widgetPage: @Composable (HomePage.Widgets, active: Boolean) -> Unit,
    foldingFeatures: List<FoldingFeature> = emptyList(),
) {
    var compactOrdinal by rememberSaveable { mutableIntStateOf(0) }
    var widgetOrdinal by rememberSaveable { mutableIntStateOf(1) }
    var wasExpanded by rememberSaveable { mutableStateOf(false) }
    var awaitingRailInteraction by rememberSaveable { mutableStateOf(false) }
    var appScrimOpacity by remember { mutableFloatStateOf(0f) }
    val widgetStateHolder = rememberSaveableStateHolder()
    val latestWidgetPage by rememberUpdatedState(widgetPage)
    val widgetContents = remember {
        (1..MAX_WIDGET_PAGES).associateWith { ordinal ->
            movableContentOf<Boolean> { active ->
                val page = HomePage.Widgets(ordinal)
                widgetStateHolder.SaveableStateProvider(page.key) { latestWidgetPage(page, active) }
            }
        }
    }
    val readingDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    // safeDrawing includes the IME. Keyboard visibility must not switch pane mode while typing.
    val safe = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fold = foldingFeatures.firstOrNull {
            it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL ||
                it.state == FoldingFeature.State.HALF_OPENED
        }?.let {
            HomeFold(
                left = with(density) { it.bounds.left.toDp().value },
                right = with(density) { it.bounds.right.toDp().value },
                vertical = it.orientation == FoldingFeature.Orientation.VERTICAL,
            )
        }
        val split = homePaneSplit(
            width = maxWidth.value,
            height = maxHeight.value + with(density) { WindowInsets.ime.getBottom(this).toDp().value },
            safeLeft = with(density) { safe.getLeft(this, LayoutDirection.Ltr).toDp().value },
            safeRight = with(density) { safe.getRight(this, LayoutDirection.Ltr).toDp().value },
            safeTop = with(density) { safe.getTop(this).toDp().value },
            safeBottom = with(density) { safe.getBottom(this).toDp().value },
            fontScale = density.fontScale,
            fold = fold,
        )
        val expanded = split != null
        val initialOrdinal = if (expanded) {
            (if (!wasExpanded && compactOrdinal > 0) compactOrdinal else widgetOrdinal)
                .coerceIn(1, counts.left.coerceAtLeast(1))
        } else {
            if (wasExpanded) 0 else compactOrdinal.coerceIn(0, counts.left)
        }
        val pager = key(expanded, counts.left) {
            rememberPagerState(
                initialPage = counts.indexOfOrdinal(initialOrdinal),
                pageCount = { if (expanded) counts.left.coerceAtLeast(1) else counts.pageCount },
            )
        }
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.collect {
                if (expanded) {
                    if (counts.left > 0) widgetOrdinal = counts.left - it
                } else {
                    compactOrdinal = counts.left - it
                    if (compactOrdinal > 0) {
                        widgetOrdinal = compactOrdinal
                        awaitingRailInteraction = true
                    }
                }
            }
        }
        SideEffect {
            if (expanded) compactOrdinal = 0 // Folding always returns to the pinned app screen.
            wasExpanded = expanded
        }
        // Focus/IME activation follows the settled page, not transient scroll progress. Toggling
        // search focus during a bring-into-view scroll would continually restart that scroll.
        val appsActive = expanded || pager.settledPage == counts.appsIndex
        val fling = PagerDefaults.flingBehavior(pager)
        Layout(
            modifier = Modifier.fillMaxSize().clipToBounds()
                // In compact mode this ancestor owns paging, so unconsumed rightward row drags
                // reach it. The app content has one permanent composition, independent of panes.
                .scrollable(pager, Orientation.Horizontal, enabled = !expanded,
                    reverseDirection = true, flingBehavior = fling)
                .drawBehind {
                    val fade = if (expanded) 1f else {
                        val distance = abs(counts.appsIndex - pager.currentPage - pager.currentPageOffsetFraction)
                        (1f - distance / SCRIM_PAGE_FADE_DISTANCE).coerceIn(0f, 1f)
                    }
                    val alpha = SCRIM_MAX_ALPHA * appScrimOpacity * fade
                    if (alpha > 0f) {
                        val start = if (expanded) with(density) { split!!.rightStart.dp.toPx() } else 0f
                        drawRect(Color.Black.copy(alpha = alpha),
                            topLeft = Offset(start, 0f),
                            size = Size((size.width - start).coerceAtLeast(0f), size.height))
                    }
                },
            content = {
                Box(Modifier.fillMaxSize()) {
                    if (expanded && counts.left == 0) {
                        Text(
                            stringResource(R.string.foldable_no_widget_pages),
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else key(expanded, counts.left) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            HorizontalPager(
                                state = pager,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = expanded,
                                key = { index -> when (val page = counts.pageAt(index)) {
                                    HomePage.Apps -> HomePage.Apps.key
                                    is HomePage.Widgets -> page.key
                                } },
                            ) { index ->
                                // The apps slot is a placeholder. Its actual content is the stable
                                // sibling below, placed at exactly this page's physical position.
                                when (val page = counts.pageAt(index)) {
                                    HomePage.Apps -> Box(Modifier.fillMaxSize())
                                    is HomePage.Widgets -> {
                                        CompositionLocalProvider(LocalLayoutDirection provides readingDirection) {
                                            widgetContents.getValue(page.ordinal)(pager.settledPage == index && !pager.isScrollInProgress)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxSize().semantics {
                    if (!appsActive) hideFromAccessibility()
                }) {
                    appPage(
                        !expanded && (pager.isScrollInProgress || pager.currentPage != counts.appsIndex || awaitingRailInteraction),
                        appsActive,
                        {
                            if (appsActive) awaitingRailInteraction = false
                        },
                        { appScrimOpacity = it },
                    )
                }
            },
        ) { measurables, constraints ->
            // A resize can measure the previous node once before BoxWithConstraints recomposes it.
            val leftWidth = if (split == null) constraints.maxWidth else
                with(density) { split.leftWidth.dp.toPx() }.roundToInt().coerceIn(0, constraints.maxWidth)
            val rightStart = if (split == null) 0 else
                with(density) { split.rightStart.dp.toPx() }.roundToInt().coerceIn(leftWidth, constraints.maxWidth)
            val widgetPane = measurables[0].measure(Constraints.fixed(leftWidth, constraints.maxHeight))
            val appPane = measurables[1].measure(Constraints.fixed(constraints.maxWidth - rightStart, constraints.maxHeight))
            layout(constraints.maxWidth, constraints.maxHeight) {
                widgetPane.place(0, 0)
                val appX = if (expanded) rightStart else
                    ((counts.appsIndex - pager.currentPage - pager.currentPageOffsetFraction) * constraints.maxWidth).roundToInt()
                appPane.place(appX, 0)
            }
        }
    }
}
