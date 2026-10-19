package app.indexlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSpacingTest {
    @Test fun defaultsPreserveExistingWidgetLayout() {
        assertEquals(WidgetSpacing(20, 12, 16), WidgetSpacing.Default)
    }

    @Test fun storedValuesAreClampedIndependentlyAndZeroSpacingIsAllowed() {
        assertEquals(WidgetSpacing(0, 40, 0), WidgetSpacing.fromStored(Int.MIN_VALUE, Int.MAX_VALUE, 0))
        assertEquals(WidgetSpacing(40, 0, 40), WidgetSpacing.fromStored(Int.MAX_VALUE, -1, Int.MAX_VALUE))
        assertEquals(WidgetSpacing(8, 10, 24), WidgetSpacing.fromStored(8, 10, 24))
    }

    @Test fun changingOneDimensionPreservesTheOthers() {
        assertEquals(WidgetSpacing(0, 12, 16), WidgetSpacing.Default.copy(outerPaddingDp = 0))
        assertEquals(WidgetSpacing(20, 0, 16), WidgetSpacing.Default.copy(columnGapDp = 0))
        assertEquals(WidgetSpacing(20, 12, 0), WidgetSpacing.Default.copy(rowGapDp = 0))
    }
}
