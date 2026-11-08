package app.indexlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.indexlauncher.R

/** Use a caption only when all three actions fit on one line, at the current font size. */
@Composable
internal fun WidgetEditBar(
    onAddWidget: () -> Unit,
    onDone: () -> Unit,
    onReorderPages: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val labeledWidth = if (onReorderPages != null) {
            subcompose("measure-labeled") {
                // This measurement-only row must not expose duplicate accessibility actions.
                Row(
                    Modifier.clearAndSetSemantics {},
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetEditActions(onAddWidget, onDone, onReorderPages, showReorderLabel = true)
                }
            }.single().measure(Constraints()).width
        } else 0
        val bar = subcompose("bar") {
            FlowRow(
                itemVerticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WidgetEditActions(onAddWidget, onDone, onReorderPages, showReorderLabel = labeledWidth <= constraints.maxWidth)
            }
        }.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(bar.width, bar.height) { bar.placeRelative(0, 0) }
    }
}

@Composable
private fun WidgetEditActions(
    onAddWidget: () -> Unit,
    onDone: () -> Unit,
    onReorderPages: (() -> Unit)?,
    showReorderLabel: Boolean,
) {
    FilledTonalButton(onClick = onAddWidget) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.add_widget))
    }
    onReorderPages?.let { reorder ->
        val description = stringResource(R.string.reorder_widget_pages)
        if (showReorderLabel) {
            TextButton(onClick = reorder, modifier = Modifier.semantics { contentDescription = description }) {
                Icon(painterResource(R.drawable.ic_reorder_pages), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.reorder))
            }
        } else {
            IconButton(onClick = reorder) {
                Icon(painterResource(R.drawable.ic_reorder_pages), contentDescription = description)
            }
        }
    }
    TextButton(onClick = onDone) { Text(stringResource(R.string.done)) }
}
