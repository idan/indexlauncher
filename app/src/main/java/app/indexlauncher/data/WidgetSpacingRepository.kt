package app.indexlauncher.data

import android.content.Context
import app.indexlauncher.model.WidgetSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared by home widgets and every left-side widget page. */
class WidgetSpacingRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _spacing = MutableStateFlow(readSpacing())
    val spacing = _spacing.asStateFlow()

    fun refresh() {
        _spacing.value = readSpacing()
    }

    fun setSpacing(spacing: WidgetSpacing) {
        prefs.edit()
            .putInt("widget_outer_padding_dp", spacing.outerPaddingDp)
            .putInt("widget_column_gap_dp", spacing.columnGapDp)
            .putInt("widget_row_gap_dp", spacing.rowGapDp)
            .apply()
        _spacing.value = spacing
    }

    private fun readSpacing() = WidgetSpacing.fromStored(
        prefs.getInt("widget_outer_padding_dp", WidgetSpacing.Default.outerPaddingDp),
        prefs.getInt("widget_column_gap_dp", WidgetSpacing.Default.columnGapDp),
        prefs.getInt("widget_row_gap_dp", WidgetSpacing.Default.rowGapDp),
    )
}
