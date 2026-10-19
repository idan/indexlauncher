package app.indexlauncher.model

/** Launcher-owned widget spacing, independent of any padding drawn by the widget provider. */
data class WidgetSpacing(
    val outerPaddingDp: Int = 20,
    val columnGapDp: Int = 12,
    val rowGapDp: Int = 16,
) {
    init {
        require(outerPaddingDp in 0..MAX_SPACING_DP)
        require(columnGapDp in 0..MAX_SPACING_DP)
        require(rowGapDp in 0..MAX_SPACING_DP)
    }

    companion object {
        const val MAX_SPACING_DP = 40
        val Default = WidgetSpacing()

        fun fromStored(outerPaddingDp: Int, columnGapDp: Int, rowGapDp: Int) = WidgetSpacing(
            outerPaddingDp = outerPaddingDp.coerceIn(0, MAX_SPACING_DP),
            columnGapDp = columnGapDp.coerceIn(0, MAX_SPACING_DP),
            rowGapDp = rowGapDp.coerceIn(0, MAX_SPACING_DP),
        )
    }
}
