package app.indexlauncher.model

/** Vertical padding on each side of an app icon; even compact rows keep a 48 dp touch target. */
data class ItemSpacing(val paddingDp: Int = 12) {
    init {
        require(paddingDp in MIN_PADDING_DP..MAX_PADDING_DP)
    }

    val rowHeightDp: Int get() = ICON_SIZE_DP + paddingDp * 2

    companion object {
        const val ICON_SIZE_DP = 40
        const val MIN_PADDING_DP = 4
        const val MAX_PADDING_DP = 28
        val Default = ItemSpacing() // Preserve the original 64 dp row height.

        fun fromPadding(paddingDp: Int) = ItemSpacing(paddingDp.coerceIn(MIN_PADDING_DP, MAX_PADDING_DP))
    }
}
