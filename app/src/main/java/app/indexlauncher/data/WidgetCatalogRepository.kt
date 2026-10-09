package app.indexlauncher.data

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetProvider(
    val info: AppWidgetProviderInfo,
    val profile: UserHandle,
    val profileSerial: Long?,
) {
    val key: String get() = "${profileSerial ?: "personal"}:${info.provider.flattenToString()}"
}

data class WidgetApp(
    val key: String,
    val label: String,
    val icon: Drawable?,
    val isWork: Boolean,
    val providers: List<WidgetProvider>,
)

class WidgetCatalogRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val manager = AppWidgetManager.getInstance(context)

    fun isAccessible(profile: UserHandle): Boolean =
        profile in launcherApps.profiles &&
            (profile == Process.myUserHandle() ||
                launcherApps.getLauncherUserInfo(profile)?.userType == UserManager.USER_TYPE_PROFILE_MANAGED) &&
            !userManager.isQuietModeEnabled(profile)

    suspend fun load(): List<WidgetApp> = withContext(Dispatchers.IO) {
        val collator = Collator.getInstance(Locale.getDefault())
        launcherApps.profiles.filter(::isAccessible).flatMap { profile ->
            val serial = if (profile == Process.myUserHandle()) null else userManager.getSerialNumberForUser(profile)
            check(serial == null || serial >= 0L) { "Missing serial number for accessible profile" }
            val providers = try {
                manager.getInstalledProvidersForProfile(profile)
            } catch (e: SecurityException) {
                Log.w("WidgetCatalog", "Cannot list widget providers for profile $serial", e)
                emptyList()
            }
            providers.asSequence()
                .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
                .filter { it.profile == profile && it.provider.packageName != context.packageName }
                .map { WidgetProvider(it, profile, serial) }
                .groupBy { it.info.provider.packageName }
                .map { (packageName, widgets) ->
                    val application = try {
                        launcherApps.getApplicationInfo(packageName, 0, profile)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w("WidgetCatalog", "Cannot load $packageName for profile $serial", e)
                        null
                    }
                    val icon = application?.let {
                        try {
                            val drawable = context.packageManager.getApplicationIcon(it)
                            if (serial == null) drawable else context.packageManager.getUserBadgedIcon(drawable, profile)
                        } catch (e: RuntimeException) {
                            Log.w("WidgetCatalog", "Cannot load icon for $packageName in profile $serial", e)
                            null
                        }
                    }
                    val label = application?.let {
                        try {
                            context.packageManager.getApplicationLabel(it).toString()
                        } catch (e: RuntimeException) {
                            Log.w("WidgetCatalog", "Cannot load label for $packageName in profile $serial", e)
                            null
                        }
                    } ?: packageName
                    WidgetApp(
                        key = "${serial ?: "personal"}:$packageName",
                        label = label,
                        icon = icon,
                        isWork = serial != null,
                        providers = widgets.sortedWith(compareBy<WidgetProvider, String>(collator) {
                            try {
                                it.info.loadLabel(context.packageManager)
                            } catch (e: RuntimeException) {
                                Log.w("WidgetCatalog", "Cannot load widget label for ${it.key}", e)
                                it.info.provider.className
                            }
                        }),
                    )
                }
        }.sortedWith(compareBy<WidgetApp, String>(collator) { it.label }.thenBy { it.key })
    }

    fun isAvailable(provider: WidgetProvider): Boolean {
        if (!isAccessible(provider.profile)) return false
        return manager.getInstalledProvidersForProfile(provider.profile).any {
            it.provider == provider.info.provider && it.profile == provider.profile &&
                it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0
        }
    }
}
