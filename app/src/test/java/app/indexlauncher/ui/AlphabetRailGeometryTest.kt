package app.indexlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetRailGeometryTest {
    @Test fun leftBubbleIsBesideRightRailAndCenteredOnTouchY() {
        val position = bubble(RailSide.Left, Offset(24f, 300f))
        assertEquals(IntOffset(232, 264), position)
        assertEquals(8, 360 - 48 - (position.x + 72))
        assertEquals(300, position.y + 36)
    }

    @Test fun leftBubbleIgnoresHorizontalFingerMovementEvenOutsideGutter() {
        val expected = bubble(RailSide.Left, Offset(0f, 300f))
        assertEquals(expected, bubble(RailSide.Left, Offset(47f, 300f)))
        assertEquals(expected, bubble(RailSide.Left, Offset(300f, 300f)))
    }

    @Test fun rightBubbleRetainsExistingHorizontalFingerFollowing() {
        assertEquals(IntOffset(208, 264), bubble(RailSide.Right, Offset(24f, 300f)))
        assertEquals(IntOffset(220, 264), bubble(RailSide.Right, Offset(36f, 300f)))
    }

    @Test fun bothSidesKeepBubbleVisibleAtVerticalEdges() {
        RailSide.entries.forEach { side ->
            assertEquals(0, bubble(side, Offset(24f, -10f)).y)
            assertEquals(728, bubble(side, Offset(24f, 810f)).y)
        }
    }

    @Test fun undersizedWindowsNeverProduceNegativeOffsets() {
        RailSide.entries.forEach { side ->
            assertEquals(IntOffset.Zero, bubble(side, Offset(24f, 40f), width = 40, height = 50))
        }
    }

    @Test fun leftBubbleScalesToPaneWidthWithoutOverlappingRail() {
        for (width in listOf(240, 360, 600)) {
            val position = bubble(RailSide.Left, Offset(24f, 300f), width = width)
            assertTrue(position.x >= 0)
            assertEquals(width - 48 - 8, position.x + 72)
        }
    }

    private fun bubble(side: RailSide, finger: Offset, width: Int = 360, height: Int = 800) = railBubbleOffset(
        widthPx = width,
        heightPx = height,
        railWidthPx = 48,
        bubbleSizePx = 72,
        horizontalDistancePx = 92,
        railGapPx = 8,
        finger = finger,
        side = side,
    )
}
