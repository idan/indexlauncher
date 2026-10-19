package app.indexlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemSpacingTest {
    @Test fun defaultPreservesOriginalRowHeight() {
        assertEquals(12, ItemSpacing.Default.paddingDp)
        assertEquals(64, ItemSpacing.Default.rowHeightDp)
    }

    @Test fun compactRowsKeepMinimumTouchTarget() {
        assertEquals(48, ItemSpacing.fromPadding(ItemSpacing.MIN_PADDING_DP).rowHeightDp)
    }

    @Test fun storedPaddingIsClampedToSupportedRange() {
        assertEquals(ItemSpacing.MIN_PADDING_DP, ItemSpacing.fromPadding(Int.MIN_VALUE).paddingDp)
        assertEquals(ItemSpacing.MAX_PADDING_DP, ItemSpacing.fromPadding(Int.MAX_VALUE).paddingDp)
        assertEquals(18, ItemSpacing.fromPadding(18).paddingDp)
        assertEquals(76, ItemSpacing.fromPadding(18).rowHeightDp)
    }
}
