package app.indexlauncher.data

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.icu.text.Transliterator
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.preferredIndexLabel
import app.indexlauncher.model.sortAppsForIndex
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun AppEntry.userHandle(userManager: UserManager): UserHandle? =
    if (profileSerial == null) Process.myUserHandle() else userManager.getUserForSerialNumber(profileSerial)

private fun englishLabel(
    activity: LauncherActivityInfo,
    packageManager: PackageManager,
    configuration: Configuration,
): String? {
    val info = activity.activityInfo
    val labelRes = when {
        info.nonLocalizedLabel != null -> return null
        info.labelRes != 0 -> info.labelRes
        info.applicationInfo.nonLocalizedLabel != null -> return null
        else -> info.applicationInfo.labelRes
    }
    if (labelRes == 0) return null
    val label = try {
        packageManager.getResourcesForApplication(info.applicationInfo, configuration).getString(labelRes)
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w("InstalledApps", "Cannot load English label for ${activity.componentName}", e)
        return null
    } catch (e: Resources.NotFoundException) {
        Log.w("InstalledApps", "Missing English label resource for ${activity.componentName}", e)
        return null
    } catch (e: SecurityException) {
        Log.w("InstalledApps", "Cannot access English label for ${activity.componentName}", e)
        return null
    }
    return label.trim().takeIf { it.isNotEmpty() }
}

/** Queries launchable activities in this user and its accessible profiles. */
class InstalledAppsRepository(private val context: Context) {
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val userManager = context.getSystemService(UserManager::class.java)
        val collator = Collator.getInstance(Locale.getDefault())
        val transliterator = Transliterator.getInstance("Any-Latin; Latin-ASCII")
        val englishConfiguration = Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val packageManager = context.packageManager

        val apps = launcherApps.profiles.flatMap { user ->
            if (user != Process.myUserHandle() &&
                launcherApps.getLauncherUserInfo(user)?.userType != UserManager.USER_TYPE_PROFILE_MANAGED
            ) return@flatMap emptyList()
            if (userManager.isQuietModeEnabled(user)) return@flatMap emptyList()
            val serial = if (user == Process.myUserHandle()) null else userManager.getSerialNumberForUser(user)
            check(serial == null || serial >= 0L) { "Missing serial number for accessible profile" }
            launcherApps.getActivityList(null, user).mapNotNull { activity ->
                val component = activity.componentName
                if (component.packageName == context.packageName) return@mapNotNull null
                val label = activity.label.toString().ifBlank { component.packageName }
                val romanized = transliterator.transliterate(label)
                val english = englishLabel(activity, packageManager, englishConfiguration)
                    ?.takeUnless { it.equals(label, ignoreCase = true) || it.equals(romanized, ignoreCase = true) }
                AppEntry(
                    packageName = component.packageName,
                    activityName = component.className,
                    label = label,
                    profileSerial = serial,
                    indexLabel = preferredIndexLabel(romanized, english),
                    englishLabel = english,
                )
            }
        }
        sortAppsForIndex(apps.distinctBy(AppEntry::id), collator)
    }
}
