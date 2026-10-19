package app.indexlauncher.model

/** Keep the saved order; missing apps stay saved but are not shown until reinstalled. */
fun favoriteApps(apps: List<AppEntry>, favoriteIds: List<String>): List<AppEntry> {
    val byId = apps.associateBy(AppEntry::id)
    return favoriteIds.mapNotNull(byId::get)
}

fun toggleFavorite(favoriteIds: List<String>, appId: String): List<String> =
    if (appId in favoriteIds) favoriteIds.filterNot { it == appId } else favoriteIds + appId

/** Reorder the supplied favorites without dropping unavailable apps or changing their saved slots. */
fun reorderFavorites(favoriteIds: List<String>, orderedIds: List<String>): List<String> {
    val existing = favoriteIds.toSet()
    val reordered = orderedIds.filter { it in existing }.distinct()
    val included = reordered.toSet()
    val next = reordered.iterator()
    return favoriteIds.map { if (it in included) next.next() else it }
}
