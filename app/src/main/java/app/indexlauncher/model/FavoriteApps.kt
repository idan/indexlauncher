package app.indexlauncher.model

/** Keep the saved order; missing apps stay saved but are not shown until reinstalled. */
fun favoriteApps(apps: List<AppEntry>, favoriteIds: List<String>): List<AppEntry> {
    val byId = apps.associateBy(AppEntry::id)
    return favoriteIds.mapNotNull(byId::get)
}

fun toggleFavorite(favoriteIds: List<String>, appId: String): List<String> =
    if (appId in favoriteIds) favoriteIds.filterNot { it == appId } else favoriteIds + appId
