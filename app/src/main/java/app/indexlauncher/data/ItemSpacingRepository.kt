package app.indexlauncher.data

import android.content.Context
import app.indexlauncher.model.ItemSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One saved spacing for favorites, the alphabetical list, and search results. */
class ItemSpacingRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _spacing = MutableStateFlow(readSpacing())
    val spacing = _spacing.asStateFlow()

    fun refresh() {
        _spacing.value = readSpacing()
    }

    fun setSpacing(spacing: ItemSpacing) {
        prefs.edit().putInt("item_padding_dp", spacing.paddingDp).apply()
        _spacing.value = spacing
    }

    private fun readSpacing() = ItemSpacing.fromPadding(prefs.getInt("item_padding_dp", ItemSpacing.Default.paddingDp))
}
