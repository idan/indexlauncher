package app.indexlauncher.data

import android.content.Context
import app.indexlauncher.model.toggleFavorite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The current user's ordered favorites; an uninstalled app is kept until explicitly unfavorited. */
class FavoritesRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val _ids = MutableStateFlow(readIds())
    val ids = _ids.asStateFlow()

    fun refresh() {
        _ids.value = readIds()
    }

    fun toggle(appId: String) {
        val updated = toggleFavorite(_ids.value, appId)
        // Component names cannot contain newlines; storing a list instead of a set preserves order.
        prefs.edit().putString("ordered_ids", updated.joinToString("\n")).apply()
        _ids.value = updated
    }

    private fun readIds() = prefs.getString("ordered_ids", null)
        ?.lineSequence()?.filter(String::isNotBlank)?.distinct()?.toList().orEmpty()
}
