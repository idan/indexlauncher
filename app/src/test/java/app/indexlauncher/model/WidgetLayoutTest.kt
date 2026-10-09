package app.indexlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WidgetLayoutTest {
    private val widgets = (1..5).map { WidgetPlacement(it) }

    @Test fun twoHalvesShareRowAndAnOddOneLeavesAnEmptySlot() {
        val rows = widgetRows(widgets) { it.id in setOf(1, 2, 4) }
        assertEquals(
            listOf(WidgetRow(widgets[0], widgets[1]), WidgetRow(widgets[2]),
                WidgetRow(widgets[3]), WidgetRow(widgets[4])),
            rows,
        )
    }

    @Test fun fullWidthWidgetBreaksPairWithoutChangingOrder() {
        val rows = widgetRows(widgets.take(3)) { it.id != 2 }
        assertEquals(widgets.take(3).map { WidgetRow(it) }, rows)
    }

    @Test fun temporaryFullWidthReflowsWithoutChangingSavedPlacements() {
        val saved = widgets.take(2).map { it.copy(width = WidgetWidth.HALF) }
        assertEquals(listOf(WidgetRow(saved[0], saved[1])), widgetRows(saved) { true })
        assertEquals(saved.map { WidgetRow(it) }, widgetRows(saved) { false })
        assertEquals(WidgetWidth.HALF, saved.first().width)
    }

    @Test fun movingChangesOrderAndKeepsSizeSelections() {
        val sized = widgets.take(3).map { it.copy(width = WidgetWidth.HALF, heightDp = it.id * 100) }
        val moved = moveWidget(sized, 2, -1)
        assertEquals(listOf(sized[1], sized[0], sized[2]), moved)
        assertEquals(listOf(2, 1, 3), moved.map { it.id })
        assertSame(sized, moveWidget(sized, 1, -1))
        assertSame(sized, moveWidget(sized, 3, 1))
    }

    @Test fun nonResizableProviderCanGrowButNotShrinkBelowItsDefaultHeight() {
        val heights = widgetHeightRange(40, verticallyResizable = false, minResizeDp = 12, maxResizeDp = 40)
        assertEquals(40, heights.first)
        assertEquals(Int.MAX_VALUE, heights.last)
        assertEquals(80, (40 + 40).coerceIn(heights))
    }

    @Test fun verticallyResizableProviderUsesItsSupportedHeightRange() {
        val heights = widgetHeightRange(100, verticallyResizable = true, minResizeDp = 60, maxResizeDp = 140)
        assertEquals(60..140, heights)
        assertEquals(140, (120 + 40).coerceIn(heights))
    }

    @Test fun draggingHeightUsesCurrentDensityAndClampsToProviderBounds() {
        assertEquals(125, resizedWidgetHeight(100, 50f, 2f, 60..140))
        assertEquals(60, resizedWidgetHeight(100, -200f, 2f, 60..140))
        assertEquals(140, resizedWidgetHeight(100, 200f, 2f, 60..140))
        assertEquals(100, resizedWidgetHeight(100, -30f, 2f, 100..Int.MAX_VALUE))
    }

    @Test fun draggingWidthRequiresOutwardThresholdAndRespectForMinWidth() {
        assertEquals(null, widgetWidthAfterDrag(isHalf = true, canMakeHalf = true, outwardDp = 31f))
        assertEquals(WidgetWidth.FULL, widgetWidthAfterDrag(isHalf = true, canMakeHalf = true, outwardDp = 32f))
        assertEquals(WidgetWidth.HALF, widgetWidthAfterDrag(isHalf = false, canMakeHalf = true, outwardDp = -32f))
        assertEquals(null, widgetWidthAfterDrag(isHalf = false, canMakeHalf = false, outwardDp = -100f))
    }
}
