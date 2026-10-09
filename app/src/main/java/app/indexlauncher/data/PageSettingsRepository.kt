package app.indexlauncher.data

import android.content.Context
import app.indexlauncher.model.MAX_WIDGET_PAGES
import app.indexlauncher.model.PageCounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persistent left-side page count. Old right-side workspace settings are intentionally not migrated. */
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
        _counts.value = PageCounts(left)
    }

    private fun readCounts() = PageCounts(prefs.getInt("left_pages", 1).coerceIn(0, MAX_WIDGET_PAGES))
}
