package app.indexlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.indexlauncher.R
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.ItemSpacing
import app.indexlauncher.model.MAX_WIDGET_PAGES
import app.indexlauncher.model.PageCounts
import app.indexlauncher.model.WidgetSpacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    counts: PageCounts,
    iconShape: IconShape,
    itemSpacing: ItemSpacing,
    onItemSpacingChange: (ItemSpacing) -> Unit,
    widgetSpacing: WidgetSpacing,
    onWidgetSpacingChange: (WidgetSpacing) -> Unit,
    onIconShapeChange: (IconShape) -> Unit,
    onLeftCountChange: (Int) -> Unit,
    onChooseWallpaper: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.headlineSmall)
            WallpaperCard(onChooseWallpaper)
            IconShapeCard(iconShape, onIconShapeChange)
            ItemSpacingCard(itemSpacing, onItemSpacingChange)
            WidgetSpacingCard(widgetSpacing, onWidgetSpacingChange)
            Text(stringResource(R.string.widget_screens_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.widget_screens_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PageCountCard(
                title = stringResource(R.string.widget_pages_title),
                subtitle = stringResource(R.string.widget_pages_description),
                count = counts.left,
                onCountChange = onLeftCountChange,
            )
            Text(
                stringResource(R.string.hidden_pages_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WallpaperCard(onChooseWallpaper: () -> Unit) {
    Card(
        onClick = onChooseWallpaper,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wallpaper),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.wallpaper), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.wallpaper_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IconShapeCard(selected: IconShape, onShapeChange: (IconShape) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.icon_shape_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.icon_shape_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.selectableGroup()) {
                IconShape.entries.forEach { shape ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = shape == selected, role = Role.RadioButton) { onShapeChange(shape) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = shape == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(when (shape) {
                                IconShape.DeviceDefault -> R.string.icon_shape_device_default
                                IconShape.Circle -> R.string.icon_shape_circle
                                IconShape.RoundedSquare -> R.string.icon_shape_rounded_square
                                IconShape.Squircle -> R.string.icon_shape_squircle
                            }),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemSpacingCard(spacing: ItemSpacing, onSpacingChange: (ItemSpacing) -> Unit) {
    val title = stringResource(R.string.item_spacing_title)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.item_spacing_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.item_spacing_value, spacing.paddingDp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = spacing.paddingDp.toFloat(),
                onValueChange = { onSpacingChange(ItemSpacing.fromPadding(it.roundToInt())) },
                valueRange = ItemSpacing.MIN_PADDING_DP.toFloat()..ItemSpacing.MAX_PADDING_DP.toFloat(),
                steps = (ItemSpacing.MAX_PADDING_DP - ItemSpacing.MIN_PADDING_DP) / 2 - 1,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = title },
            )
        }
    }
}

@Composable
private fun WidgetSpacingCard(spacing: WidgetSpacing, onSpacingChange: (WidgetSpacing) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.widget_spacing_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.widget_spacing_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WidgetSpacingSlider(stringResource(R.string.widget_outer_padding), spacing.outerPaddingDp) {
                onSpacingChange(spacing.copy(outerPaddingDp = it))
            }
            WidgetSpacingSlider(stringResource(R.string.widget_column_gap), spacing.columnGapDp) {
                onSpacingChange(spacing.copy(columnGapDp = it))
            }
            WidgetSpacingSlider(stringResource(R.string.widget_row_gap), spacing.rowGapDp) {
                onSpacingChange(spacing.copy(rowGapDp = it))
            }
        }
    }
}

@Composable
private fun WidgetSpacingSlider(title: String, value: Int, onValueChange: (Int) -> Unit) {
    Text(stringResource(R.string.widget_spacing_value, title, value), style = MaterialTheme.typography.bodyLarge)
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = 0f..WidgetSpacing.MAX_SPACING_DP.toFloat(),
        steps = WidgetSpacing.MAX_SPACING_DP / 2 - 1,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = title },
    )
}

@Composable
private fun PageCountCard(
    title: String,
    subtitle: String,
    count: Int,
    onCountChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(onClick = { onCountChange(count - 1) }, enabled = count > 0) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.remove_screen, title),
                    )
                }
                Text(
                    count.toString(),
                    modifier = Modifier.width(44.dp),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                FilledTonalIconButton(
                    onClick = { onCountChange(count + 1) },
                    enabled = count < MAX_WIDGET_PAGES,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_screen, title),
                    )
                }
            }
        }
    }
}
