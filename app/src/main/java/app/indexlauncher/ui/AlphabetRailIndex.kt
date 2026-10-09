package app.indexlauncher.ui

import app.indexlauncher.model.AppSection

private val alphabet = ('A'..'Z').toList()
internal const val RAIL_TRACK_HEIGHT_FRACTION = 0.76f
internal data class RailTrack(val topPx: Float, val heightPx: Float)

internal sealed interface AlphabetRailTarget {
    data object Favorites : AlphabetRailTarget
    data class Section(val letter: Char) : AlphabetRailTarget
    data object Settings : AlphabetRailTarget
}

/** Favorites and launcher tools bookend # and the populated Latin initials. */
internal fun alphabetRailTargets(sections: List<AppSection>): List<AlphabetRailTarget> = buildList {
    add(AlphabetRailTarget.Favorites)
    add(AlphabetRailTarget.Section('#'))
    val populated = sections.mapTo(mutableSetOf()) { it.letter }
    alphabet.filter { it in populated }.forEach { add(AlphabetRailTarget.Section(it)) }
    add(AlphabetRailTarget.Settings)
}

/** Account for headers and rows in the all-apps list when jumping to a populated section. */
internal fun sectionJumpPositions(sections: List<AppSection>): Map<Char, Int> {
    return buildMap {
        put('#', 0)
        var index = 0
        sections.forEach { section ->
            put(section.letter, index)
            index += 1 + section.apps.size // Header and rows in the LazyColumn.
        }
    }
}

/** Halve the free space in each old slot, then lift the track above the safe-area bottom. */
internal fun railTrack(
    heightPx: Int,
    targetCount: Int,
    contentHeightPx: Float,
    bottomInsetPx: Float,
): RailTrack {
    if (heightPx <= 0 || targetCount <= 0) return RailTrack(0f, 0f)
    val originalSlot = heightPx * RAIL_TRACK_HEIGHT_FRACTION / (alphabet.size + 3)
    val slot = originalSlot - (originalSlot - contentHeightPx).coerceAtLeast(0f) / 2f
    val inset = bottomInsetPx.coerceIn(0f, heightPx / 3f)
    val trackHeight = (slot * targetCount).coerceAtMost(heightPx - inset)
    return RailTrack(heightPx - inset - trackHeight, trackHeight)
}

/** Map a touch to an equally sized target slot; beyond the visible track, clamp to its ends. */
internal fun railTargetAtOffset(
    targets: List<AlphabetRailTarget>,
    yPx: Float,
    track: RailTrack,
): AlphabetRailTarget? {
    if (targets.isEmpty() || track.heightPx <= 0f) return null
    val y = (yPx - track.topPx).coerceIn(0f, (track.heightPx - 1f).coerceAtLeast(0f))
    val index = (y * targets.size / track.heightPx).toInt().coerceAtMost(targets.lastIndex)
    return targets[index]
}
