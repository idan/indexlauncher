package app.indexlauncher.data

import android.content.Context
import android.util.Log
import app.indexlauncher.model.HomePage
import app.indexlauncher.model.MAX_WIDGET_PAGES
import app.indexlauncher.model.WidgetWidth
import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.moveWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Widget placements use stable home/left-side keys, including temporarily hidden pages. */
class WidgetPlacementRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("widget_placements", Context.MODE_PRIVATE)

    private val _widgets = MutableStateFlow(readAll())
    val widgets = _widgets.asStateFlow()

    fun refresh() {
        _widgets.value = readAll()
    }

    fun add(pageKey: String, widgetId: Int) {
        val placements = _widgets.value[pageKey].orEmpty()
        if (placements.none { it.id == widgetId }) save(pageKey, placements + WidgetPlacement(widgetId))
    }

    fun remove(pageKey: String, widgetId: Int) {
        update(pageKey, widgetId) { null }
    }

    fun move(pageKey: String, widgetId: Int, direction: Int) {
        val current = _widgets.value[pageKey].orEmpty()
        val moved = moveWidget(current, widgetId, direction)
        if (moved != current) save(pageKey, moved)
    }

    fun setWidth(pageKey: String, widgetId: Int, width: WidgetWidth) {
        update(pageKey, widgetId) { it.copy(width = width) }
    }

    fun setHeight(pageKey: String, widgetId: Int, heightDp: Int) {
        require(heightDp > 0)
        update(pageKey, widgetId) { it.copy(heightDp = heightDp) }
    }

    private fun update(pageKey: String, widgetId: Int, transform: (WidgetPlacement) -> WidgetPlacement?) {
        val current = _widgets.value[pageKey].orEmpty()
        if (current.none { it.id == widgetId }) return
        save(pageKey, current.mapNotNull { if (it.id == widgetId) transform(it) else it })
    }

    private fun save(pageKey: String, placements: List<WidgetPlacement>) {
        val key = "widgets_$pageKey"
        if (placements.isEmpty()) {
            prefs.edit().remove(key).apply()
            _widgets.value = _widgets.value - pageKey
        } else {
            prefs.edit().putString(key, encodeWidgetPlacements(placements)).apply()
            _widgets.value = _widgets.value + (pageKey to placements)
        }
    }

    private fun readPlacements(key: String): List<WidgetPlacement> =
        decodeWidgetPlacements(prefs.getString(key, null)) {
            Log.w("WidgetPlacements", "Invalid widget placement in $key: $it")
        }

    private fun readAll(): Map<String, List<WidgetPlacement>> = buildMap {
        val home = readPlacements("widgets_${HomePage.Apps.key}")
        if (home.isNotEmpty()) put(HomePage.Apps.key, home)
        for (ordinal in 1..MAX_WIDGET_PAGES) {
            val pageKey = HomePage.Widgets(ordinal).key
            val placements = readPlacements("widgets_$pageKey")
            if (placements.isNotEmpty()) put(pageKey, placements)
        }
    }
}
