package app.indexlauncher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import app.indexlauncher.data.AppIconRepository
import app.indexlauncher.data.FavoritesRepository
import app.indexlauncher.data.IconShapeRepository
import app.indexlauncher.data.InstalledAppsRepository
import app.indexlauncher.data.ItemSpacingRepository
import app.indexlauncher.data.PageSettingsRepository
import app.indexlauncher.data.WidgetCatalogRepository
import app.indexlauncher.data.WidgetPlacementRepository
import app.indexlauncher.data.WidgetProvider
import app.indexlauncher.data.WidgetSpacingRepository
import app.indexlauncher.data.userHandle
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.HomePage
import app.indexlauncher.ui.HomePager
import app.indexlauncher.ui.LauncherScreen
import app.indexlauncher.ui.LauncherViewModel
import app.indexlauncher.ui.WidgetGalleryScreen
import app.indexlauncher.ui.WidgetPageScreen
import app.indexlauncher.ui.theme.IndexLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val WIDGET_HOST_ID = 1024
private const val CONFIGURE_WIDGET_REQUEST = 1025
private const val PENDING_WIDGET_ID = "pending_widget_id"
private const val PENDING_WIDGET_PAGE = "pending_widget_page"
private const val PENDING_WIDGET_PROVIDER = "pending_widget_provider"
private const val PENDING_WIDGET_PROFILE = "pending_widget_profile"
private const val GALLERY_PAGE = "gallery_page"
private const val HOME_REQUEST = "home_request"

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels {
        viewModelFactory {
            initializer {
                LauncherViewModel(
                    InstalledAppsRepository(applicationContext),
                    AppIconRepository(applicationContext),
                )
            }
        }
    }
    private val pageSettings by lazy { PageSettingsRepository(applicationContext) }
    private val iconShapes by lazy { IconShapeRepository(applicationContext) }
    private val itemSpacing by lazy { ItemSpacingRepository(applicationContext) }
    private val widgetSpacing by lazy { WidgetSpacingRepository(applicationContext) }
    private val widgetPlacements by lazy { WidgetPlacementRepository(applicationContext) }
    private val widgetCatalog by lazy { WidgetCatalogRepository(applicationContext) }
    private val favorites by lazy { FavoritesRepository(applicationContext) }
    private val launcherApps by lazy { getSystemService(LauncherApps::class.java) }
    private val userManager by lazy { getSystemService(UserManager::class.java) }
    private val widgetHost by lazy { AppWidgetHost(this, WIDGET_HOST_ID) }
    private val widgetManager by lazy { AppWidgetManager.getInstance(this) }
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingWidgetPage: String? = null
    private var pendingProvider: ComponentName? = null
    private var pendingProfileSerial: Long? = null
    private var galleryPage by mutableStateOf<String?>(null)
    private var homeRequest by mutableIntStateOf(0)
    private var selectingWidget by mutableStateOf(false)
    private var widgetCatalogVersion by mutableIntStateOf(0)
    private val appChanges = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = refreshCatalogs()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refreshCatalogs()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refreshCatalogs()
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
            refreshCatalogs()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
            refreshCatalogs()
    }

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            handleBoundWidget(result.data)
        } else {
            cancelPendingWidget()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeRequest = savedInstanceState?.getInt(HOME_REQUEST) ?: 0
        pendingWidgetId = savedInstanceState?.getInt(PENDING_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetPage = savedInstanceState?.getString(PENDING_WIDGET_PAGE)
        pendingProvider = savedInstanceState?.getString(PENDING_WIDGET_PROVIDER)?.let(ComponentName::unflattenFromString)
        pendingProfileSerial = savedInstanceState?.takeIf { it.containsKey(PENDING_WIDGET_PROFILE) }
            ?.getLong(PENDING_WIDGET_PROFILE)
        galleryPage = savedInstanceState?.getString(GALLERY_PAGE)
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && pendingProvider == null) {
            cancelPendingWidget()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            IndexLauncherTheme {
                val foldingFeatures = remember {
                    WindowInfoTracker.getOrCreate(this@MainActivity).windowLayoutInfo(this@MainActivity)
                        .map { it.displayFeatures.filterIsInstance<FoldingFeature>() }
                }.collectAsStateWithLifecycle(initialValue = emptyList()).value
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                val counts = pageSettings.counts.collectAsStateWithLifecycle().value
                val iconShape = iconShapes.shape.collectAsStateWithLifecycle().value
                val spacing = itemSpacing.spacing.collectAsStateWithLifecycle().value
                val widgetGaps = widgetSpacing.spacing.collectAsStateWithLifecycle().value
                val widgets = widgetPlacements.widgets.collectAsStateWithLifecycle().value
                val favoriteIds = favorites.ids.collectAsStateWithLifecycle().value
                Box(Modifier.fillMaxSize()) {
                    HomePager(
                        counts = counts,
                        homeRequest = homeRequest,
                        foldingFeatures = foldingFeatures,
                        appPage = { suppressRail, active, onRailInteraction, onScrimOpacityChange ->
                            LauncherScreen(
                                state = state,
                                favoriteIds = favoriteIds,
                                homeWidgets = widgets[HomePage.Apps.key].orEmpty(),
                                widgetHost = widgetHost,
                                widgetManager = widgetManager,
                                suppressRail = suppressRail,
                                active = active,
                                onRailInteraction = onRailInteraction,
                                onScrimOpacityChange = onScrimOpacityChange,
                                onToggleFavorite = favorites::toggle,
                                onReorderFavorites = favorites::reorder,
                                onAppClick = ::launchApp,
                                onOpenSettings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                                onLoadIcon = viewModel::loadIcon,
                                iconShape = iconShape,
                                itemSpacing = spacing,
                                widgetSpacing = widgetGaps,
                                homeRequest = homeRequest,
                                onRetry = viewModel::refreshApps,
                                onAddHomeWidget = { addWidget(HomePage.Apps.key) },
                                onRemoveHomeWidget = { id -> removeWidget(HomePage.Apps.key, id) },
                                onMoveHomeWidget = { id, direction ->
                                    widgetPlacements.move(HomePage.Apps.key, id, direction)
                                },
                                onHomeWidgetWidthChange = { id, width ->
                                    widgetPlacements.setWidth(HomePage.Apps.key, id, width)
                                },
                                onHomeWidgetHeightChange = { id, height ->
                                    widgetPlacements.setHeight(HomePage.Apps.key, id, height)
                                },
                            )
                        },
                        widgetPage = { page, active ->
                            WidgetPageScreen(
                                page = page,
                                active = active,
                                widgets = widgets[page.key].orEmpty(),
                                spacing = widgetGaps,
                                host = widgetHost,
                                manager = widgetManager,
                                onAddWidget = { addWidget(page.key) },
                                onRemoveWidget = { id -> removeWidget(page.key, id) },
                                onMoveWidget = { id, direction -> widgetPlacements.move(page.key, id, direction) },
                                onWidthChange = { id, width -> widgetPlacements.setWidth(page.key, id, width) },
                                onHeightChange = { id, height -> widgetPlacements.setHeight(page.key, id, height) },
                            )
                        },
                    )
                    if (galleryPage != null) {
                        WidgetGalleryScreen(
                            repository = widgetCatalog,
                            manager = widgetManager,
                            version = widgetCatalogVersion,
                            selecting = selectingWidget,
                            onRetry = { widgetCatalogVersion++ },
                            onSelect = ::selectWidget,
                            onDismiss = { if (!selectingWidget) galleryPage = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home is delivered to this singleTask activity even when it is already in front.
        // A normal resume (from an app, Settings, or a widget flow) must preserve the view.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            galleryPage = null
            homeRequest++
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
        launcherApps.registerCallback(appChanges)
    }

    override fun onResume() {
        super.onResume()
        pageSettings.refresh()
        iconShapes.refresh()
        itemSpacing.refresh()
        widgetSpacing.refresh()
        widgetPlacements.refresh()
        favorites.refresh()
        viewModel.refreshApps()
        widgetCatalogVersion++
    }

    override fun onStop() {
        launcherApps.unregisterCallback(appChanges)
        widgetHost.stopListening()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(HOME_REQUEST, homeRequest)
        outState.putInt(PENDING_WIDGET_ID, pendingWidgetId)
        outState.putString(PENDING_WIDGET_PAGE, pendingWidgetPage)
        outState.putString(PENDING_WIDGET_PROVIDER, pendingProvider?.flattenToString())
        pendingProfileSerial?.let { outState.putLong(PENDING_WIDGET_PROFILE, it) }
        outState.putString(GALLERY_PAGE, galleryPage)
        super.onSaveInstanceState(outState)
    }

    private fun addWidget(pageKey: String) {
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) return
        galleryPage = pageKey
    }

    private fun selectWidget(provider: WidgetProvider) {
        if (selectingWidget || pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) return
        val page = galleryPage ?: return
        selectingWidget = true
        lifecycleScope.launch {
            try {
                if (!withContext(Dispatchers.IO) { widgetCatalog.isAvailable(provider) }) {
                    Toast.makeText(this@MainActivity, R.string.widget_no_longer_available, Toast.LENGTH_SHORT).show()
                    widgetCatalogVersion++
                    return@launch
                }
                pendingWidgetId = widgetHost.allocateAppWidgetId()
                pendingWidgetPage = page
                pendingProvider = provider.info.provider
                pendingProfileSerial = provider.profileSerial
                galleryPage = null
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
                }
                if (widgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, provider.profile, provider.info.provider, options)) {
                    handleBoundWidget(null)
                } else {
                    bindWidget.launch(
                        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.info.provider)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options),
                    )
                }
            } catch (e: RuntimeException) {
                Log.e("WidgetGallery", "Cannot bind ${provider.key}", e)
                failPendingWidget()
            } finally {
                selectingWidget = false
            }
        }
    }

    private fun handleBoundWidget(data: Intent?) {
        if (data?.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID) == true &&
            data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) != pendingWidgetId
        ) {
            failPendingWidget()
            return
        }
        val info = pendingWidgetInfo() ?: run {
            failPendingWidget()
            return
        }
        if (info.configure != null) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(
                    this, pendingWidgetId, 0, CONFIGURE_WIDGET_REQUEST, null,
                )
            } catch (e: RuntimeException) {
                Log.e("WidgetGallery", "Cannot configure widget $pendingWidgetId", e)
                failPendingWidget()
            }
        } else {
            completePendingWidget()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION") // AppWidgetHost's cross-profile configure API returns here.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIGURE_WIDGET_REQUEST) {
            if (resultCode == Activity.RESULT_OK &&
                (data?.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID) != true ||
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) == pendingWidgetId)
            ) completePendingWidget()
            else if (resultCode == Activity.RESULT_OK) failPendingWidget()
            else cancelPendingWidget()
        }
    }

    private fun pendingWidgetInfo(): AppWidgetProviderInfo? {
        val provider = pendingProvider ?: return null
        return try {
            val serial = pendingProfileSerial
            val profile = if (serial == null) Process.myUserHandle()
            else userManager.getUserForSerialNumber(serial) ?: return null
            if (!widgetCatalog.isAccessible(profile)) return null
            val info = widgetManager.getAppWidgetInfo(pendingWidgetId) ?: return null
            if (info.provider != provider || info.profile != profile) return null
            info.takeIf { widgetCatalog.isAvailable(WidgetProvider(it, profile, serial)) }
        } catch (e: RuntimeException) {
            Log.w("WidgetGallery", "Cannot verify widget $pendingWidgetId", e)
            null
        }
    }

    private fun completePendingWidget() {
        val id = pendingWidgetId
        val page = pendingWidgetPage
        val available = id != AppWidgetManager.INVALID_APPWIDGET_ID && pendingWidgetInfo() != null
        if (available && page != null) {
            widgetPlacements.add(page, id)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            pendingWidgetPage = null
            pendingProvider = null
            pendingProfileSerial = null
        } else {
            failPendingWidget()
        }
    }

    private fun failPendingWidget() {
        cancelPendingWidget()
        Toast.makeText(this, R.string.widget_add_failed, Toast.LENGTH_SHORT).show()
    }

    private fun cancelPendingWidget() {
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetHost.deleteAppWidgetId(pendingWidgetId)
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetPage = null
        pendingProvider = null
        pendingProfileSerial = null
    }

    private fun refreshCatalogs() {
        runOnUiThread {
            viewModel.refreshApps()
            widgetCatalogVersion++
        }
    }

    private fun removeWidget(pageKey: String, id: Int) {
        widgetPlacements.remove(pageKey, id)
        widgetHost.deleteAppWidgetId(id)
    }

    private fun launchApp(app: AppEntry) {
        try {
            val user = app.userHandle(userManager) ?: run {
                showLaunchError()
                return
            }
            launcherApps.startMainActivity(ComponentName(app.packageName, app.activityName), user, null, null)
        } catch (e: ActivityNotFoundException) {
            showLaunchError()
        } catch (e: SecurityException) {
            showLaunchError()
        }
    }

    private fun showLaunchError() {
        Toast.makeText(this, R.string.launch_error, Toast.LENGTH_SHORT).show()
        viewModel.refreshApps()
    }
}
