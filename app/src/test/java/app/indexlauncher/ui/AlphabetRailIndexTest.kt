package app.indexlauncher.ui

import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.groupAppsByLetter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AlphabetRailIndexTest {
    private val targets = listOf(
        AlphabetRailTarget.Favorites,
        AlphabetRailTarget.Section('A'),
        AlphabetRailTarget.Settings,
    )

    @Test fun railHalvesInterTargetSpaceAndReservesBottomInset() {
        val track = railTrack(heightPx = 1_000, targetCount = 3, contentHeightPx = 16f, bottomInsetPx = 54f)
        val originalSlot = 1_000 * RAIL_TRACK_HEIGHT_FRACTION / 29
        assertEquals((originalSlot - 16f) / 2, track.heightPx / 3 - 16f, 0.001f)
        val crowdedTrack = railTrack(1_000, 29, 16f, 54f)
        assertEquals(track.heightPx / 3, crowdedTrack.heightPx / 29, 0.001f)
        assertEquals(946f, track.topPx + track.heightPx, 0.001f)
        assertEquals(AlphabetRailTarget.Favorites, railTargetAtOffset(targets, 0f, track))
        assertEquals(AlphabetRailTarget.Favorites, railTargetAtOffset(targets, track.topPx, track))
        assertEquals(AlphabetRailTarget.Section('A'),
            railTargetAtOffset(targets, track.topPx + track.heightPx / 3, track))
        assertEquals(AlphabetRailTarget.Settings, railTargetAtOffset(targets, 946f, track))
    }

    @Test fun raisingTheIndexPreservesSpacingAndMovesTouchTargetsWithIt() {
        val original = railTrack(1_000, targets.size, 16f, 30f)
        val raised = railTrack(1_000, targets.size, 16f, 54f)
        assertEquals(original.topPx - 24f, raised.topPx, 0.001f)
        assertEquals(original.heightPx, raised.heightPx, 0.001f)
        targets.forEachIndexed { index, target ->
            val y = raised.topPx + raised.heightPx / targets.size * (index + 0.5f)
            assertEquals(target, railTargetAtOffset(targets, y, raised))
        }
    }

    @Test fun bottomInsetIsCappedInShortWindows() {
        val track = railTrack(60, targets.size, 16f, 54f)
        assertEquals(40f, track.topPx + track.heightPx, 0.001f)
        assertEquals(AlphabetRailTarget.Favorites, railTargetAtOffset(targets, 0f, track))
        assertEquals(AlphabetRailTarget.Settings, railTargetAtOffset(targets, 60f, track))
    }

    @Test fun draggingPastEitherEndStaysOnTheEndTarget() {
        val track = railTrack(300, targets.size, 16f, 30f)
        assertEquals(AlphabetRailTarget.Favorites, railTargetAtOffset(targets, -25f, track))
        assertEquals(AlphabetRailTarget.Settings, railTargetAtOffset(targets, 500f, track))
        assertNull(railTargetAtOffset(emptyList(), 20f, track))
        assertNull(railTargetAtOffset(targets, 20f, railTrack(0, targets.size, 16f, 30f)))
    }

    @Test fun railShowsOnlyPopulatedLatinLettersAndKeepsHashForOtherScripts() {
        val sections = groupAppsByLetter(listOf(
            app("Camera"), app("Émail"), app("Files"), app("שלום"), app("123 Tasks"),
            AppEntry("example.bank", "MainActivity", "בנק דיסקונט", indexLabel = "bnq dysqwnt"),
        ))
        assertEquals(
            listOf(
                AlphabetRailTarget.Favorites,
                AlphabetRailTarget.Section('#'),
                AlphabetRailTarget.Section('B'),
                AlphabetRailTarget.Section('C'),
                AlphabetRailTarget.Section('E'),
                AlphabetRailTarget.Section('F'),
                AlphabetRailTarget.Settings,
            ),
            alphabetRailTargets(sections),
        )
        assertEquals(
            listOf(AlphabetRailTarget.Favorites, AlphabetRailTarget.Section('#'), AlphabetRailTarget.Settings),
            alphabetRailTargets(emptyList()),
        )
    }

    @Test fun onlyPopulatedLettersHaveJumpPositionsWithHeadersAndRowsCounted() {
        val sections = groupAppsByLetter(listOf(app("Camera"), app("Clock"), app("Files"), app("123 Tasks")))
        val positions = sectionJumpPositions(sections)
        assertEquals(0, positions['#'])
        assertEquals(2, positions['C'])
        assertEquals(5, positions['F'])
        assertFalse('A' in positions)
        assertFalse('D' in positions)
        assertFalse('Z' in positions)
    }

    @Test fun numericOnlyCatalogStillHasUsableAlphabet() {
        val sections = groupAppsByLetter(listOf(app("123 Tasks")))
        assertEquals(mapOf('#' to 0), sectionJumpPositions(sections))
        assertEquals(
            listOf(AlphabetRailTarget.Favorites, AlphabetRailTarget.Section('#'), AlphabetRailTarget.Settings),
            alphabetRailTargets(sections),
        )
    }

    private fun app(label: String) = AppEntry("example.${label.lowercase()}", "MainActivity", label)
}
