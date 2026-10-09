package app.indexlauncher.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.indexlauncher.R
import app.indexlauncher.data.WidgetApp
import app.indexlauncher.data.WidgetCatalogRepository
import app.indexlauncher.data.WidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun WidgetGalleryScreen(
    repository: WidgetCatalogRepository,
    manager: AppWidgetManager,
    version: Int,
    selecting: Boolean,
    onRetry: () -> Unit,
    onSelect: (WidgetProvider) -> Unit,
    onDismiss: () -> Unit,
) {
    var apps by remember { mutableStateOf<List<WidgetApp>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(repository, version) {
        apps = null
        failed = false
        try {
            apps = repository.load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            Log.e("WidgetGallery", "Cannot load widget catalog", e)
            failed = true
        }
    }
    BackHandler { onDismiss() }
    val backgroundInteractions = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxSize()
            .background(Color(0xFF101A14))
            .clickable(interactionSource = backgroundInteractions, indication = null) {}
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
            }
            Text(stringResource(R.string.widget_gallery_title), style = MaterialTheme.typography.headlineSmall)
        }
        if (failed) {
            Text(stringResource(R.string.widget_gallery_error), Modifier.padding(20.dp))
            TextButton(onClick = onRetry, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.retry))
            }
        } else if (apps == null) {
            Text(stringResource(R.string.widget_gallery_loading), Modifier.padding(20.dp))
        } else if (apps.isNullOrEmpty()) {
            Text(stringResource(R.string.widget_gallery_empty), Modifier.padding(20.dp))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                apps.orEmpty().forEach { app ->
                    item(key = app.key) {
                        WidgetAppHeader(app, expanded == app.key) {
                            expanded = if (expanded == app.key) null else app.key
                        }
                    }
                    if (expanded == app.key) {
                        items(app.providers, key = { it.key }) { provider ->
                            WidgetGalleryTile(provider, manager, selecting) { onSelect(provider) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetAppHeader(app: WidgetApp, expanded: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            app.icon?.let { icon ->
                AndroidView(
                    factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                    update = { it.setImageDrawable(icon) },
                    modifier = Modifier.size(44.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (app.isWork) {
                        pluralStringResource(R.plurals.widget_work_group, app.providers.size, app.providers.size)
                    } else {
                        pluralStringResource(R.plurals.widget_group_count, app.providers.size, app.providers.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun WidgetGalleryTile(
    provider: WidgetProvider,
    manager: AppWidgetManager,
    selecting: Boolean,
    onSelect: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val label = remember(provider.info) {
        try {
            provider.info.loadLabel(context.packageManager)
        } catch (e: RuntimeException) {
            Log.w("WidgetGallery", "Cannot read label of ${provider.key}", e)
            provider.info.provider.className
        }
    }
    val description = remember(provider.info) {
        try {
            provider.info.loadDescription(context)?.toString()?.takeIf { it.isNotBlank() }
        } catch (e: RuntimeException) {
            Log.w("WidgetGallery", "Cannot read description of ${provider.key}", e)
            null
        }
    }
    val widthDp = with(density) { provider.info.minWidth.toDp().value.roundToInt() }
    val heightDp = with(density) { provider.info.minHeight.toDp().value.roundToInt() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium)
            if (widthDp > 0 && heightDp > 0) {
                Text(
                    stringResource(R.string.widget_suggested_size, widthDp, heightDp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            WidgetPreview(provider, manager, label)
            Button(onClick = onSelect, enabled = !selecting) {
                Text(stringResource(R.string.widget_add_named, label))
            }
        }
    }
}

@Composable
private fun WidgetPreview(provider: WidgetProvider, manager: AppWidgetManager, label: String) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val generated by produceState<RemoteViews?>(null, provider.info, manager) {
        value = withContext(Dispatchers.IO) {
            try {
                manager.getWidgetPreview(
                    provider.info.provider, provider.profile, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                )
            } catch (e: RuntimeException) {
                Log.w("WidgetGallery", "Cannot load generated preview for ${provider.key}", e)
                null
            }
        }
    }
    val previewHeight = with(density) { provider.info.minHeight.toDp() }.coerceIn(96.dp, 240.dp)
    key(provider.info, generated) {
        AndroidView(
            factory = { viewContext ->
                object : FrameLayout(viewContext) {
                    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true
                    override fun onTouchEvent(event: MotionEvent): Boolean = true
                }.apply {
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    val previews = listOfNotNull(
                        generated,
                        provider.info.previewLayout.takeIf { it != 0 && provider.profileSerial == null }?.let {
                            RemoteViews(provider.info.provider.packageName, it)
                        },
                    )
                    val view = previews.firstNotNullOfOrNull { preview ->
                        try {
                            preview.apply(viewContext, null)
                        } catch (e: RuntimeException) {
                            Log.w("WidgetGallery", "Cannot display layout preview for ${provider.key}", e)
                            null
                        }
                    }
                    if (view != null) {
                        addView(view, ViewGroup.LayoutParams(-1, -1))
                    } else {
                        val image = loadPreviewDrawable(provider, viewContext)
                        if (image != null) {
                            addView(ImageView(viewContext).apply {
                                setImageDrawable(image)
                                scaleType = ImageView.ScaleType.FIT_CENTER
                            }, ViewGroup.LayoutParams(-1, -1))
                        } else {
                            addView(TextView(viewContext).apply {
                                text = viewContext.getString(R.string.widget_preview_unavailable)
                                gravity = android.view.Gravity.CENTER
                                setTextColor(android.graphics.Color.WHITE)
                            }, ViewGroup.LayoutParams(-1, -1))
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(previewHeight)
                .background(Color(0xFF26332B), MaterialTheme.shapes.medium)
                .semantics { contentDescription = context.getString(R.string.widget_preview_description, label) },
        )
    }
}

private fun loadPreviewDrawable(provider: WidgetProvider, context: android.content.Context): Drawable? {
    val image = try {
        provider.info.loadPreviewImage(context, context.resources.displayMetrics.densityDpi)
    } catch (e: RuntimeException) {
        Log.w("WidgetGallery", "Cannot load image preview for ${provider.key}", e)
        null
    }
    if (image != null) return image
    val icon = try {
        provider.info.loadIcon(context, context.resources.displayMetrics.densityDpi)
    } catch (e: RuntimeException) {
        Log.w("WidgetGallery", "Cannot load icon preview for ${provider.key}", e)
        null
    }
    return if (icon != null && provider.profileSerial != null) {
        try {
            context.packageManager.getUserBadgedIcon(icon, provider.profile)
        } catch (e: RuntimeException) {
            Log.w("WidgetGallery", "Cannot badge icon preview for ${provider.key}", e)
            null
        }
    } else icon
}
