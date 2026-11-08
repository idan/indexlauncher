package app.indexlauncher.data

import android.content.Context
import app.indexlauncher.model.MAX_WIDGET_PAGES
import app.indexlauncher.model.PageCounts
import app.indexlauncher.model.normalizedPageOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persistent count and stable screen order. Hidden screens keep their identities and placements. */
class PageSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("workspace_left", Context.MODE_PRIVATE)
    private val _counts = MutableStateFlow(readCounts())
    val counts = _counts.asStateFlow()

    fun refresh() {
        _counts.value = readCounts()
    }

    fun setLeft(count: Int) {
        val left = count.coerceIn(0, MAX_WIDGET_PAGES)
        prefs.edit().putInt("left_pages", left).apply()
        _counts.value = readCounts().copy(left = left)
    }

    fun reorder(nearestFirst: List<Int>) {
        // Re-read in case Settings changed the count while Home was stopped.
        val current = readCounts()
        if (nearestFirst.size != current.left || nearestFirst.toSet() != current.visibleOrdinals.toSet()) return
        val updated = current.withVisibleOrder(nearestFirst)
        prefs.edit().putString("page_order", updated.order.joinToString(",")).apply()
        _counts.value = updated
    }

    private fun readCounts(): PageCounts {
        val order = normalizedPageOrder(
            prefs.getString("page_order", null).orEmpty().split(',').mapNotNull { it.toIntOrNull() },
        )
        return PageCounts(prefs.getInt("left_pages", 1).coerceIn(0, MAX_WIDGET_PAGES), order)
    }
}
