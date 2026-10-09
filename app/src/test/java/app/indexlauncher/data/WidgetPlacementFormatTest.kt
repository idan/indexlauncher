package app.indexlauncher.data

import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPlacementFormatTest {
    @Test fun oldWidgetIdsMigrateToFullWidthWithoutLosingOrder() {
        val invalid = mutableListOf<String>()
        assertEquals(
            listOf(WidgetPlacement(12, WidgetWidth.FULL), WidgetPlacement(4, WidgetWidth.FULL)),
            decodeWidgetPlacements("12,4", invalid::add),
        )
        assertEquals(emptyList<String>(), invalid)
    }

    @Test fun savedSizesAndAutomaticDefaultsRoundTrip() {
        val placements = listOf(
            WidgetPlacement(12, WidgetWidth.HALF, 240),
            WidgetPlacement(4, WidgetWidth.AUTO),
            WidgetPlacement(7, WidgetWidth.FULL, 400),
        )
        val invalid = mutableListOf<String>()
        assertEquals(placements, decodeWidgetPlacements(encodeWidgetPlacements(placements), invalid::add))
        assertEquals(emptyList<String>(), invalid)
    }

    @Test fun malformedOrDuplicateEntriesAreReportedAndDoNotDiscardOtherWidgets() {
        val invalid = mutableListOf<String>()
        assertEquals(
            listOf(WidgetPlacement(2, WidgetWidth.HALF, 200), WidgetPlacement(3, WidgetWidth.FULL)),
            decodeWidgetPlacements("2:HALF:200,?,2:FULL:,3,4:HALF:bad", invalid::add),
        )
        assertEquals(listOf("?", "2:FULL:", "4:HALF:bad"), invalid)
    }
}
