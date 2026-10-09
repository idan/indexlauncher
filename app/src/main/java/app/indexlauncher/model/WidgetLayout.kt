package app.indexlauncher.model

import kotlin.math.roundToInt

enum class WidgetWidth { AUTO, HALF, FULL }

data class WidgetPlacement(
    val id: Int,
    val width: WidgetWidth = WidgetWidth.AUTO,
    val heightDp: Int? = null,
)

data class WidgetRow(val first: WidgetPlacement, val second: WidgetPlacement? = null)

fun widgetHeightRange(
    defaultDp: Int,
    verticallyResizable: Boolean,
    minResizeDp: Int?,
    maxResizeDp: Int?,
): IntRange {
    require(defaultDp > 0)
    val minimum = if (verticallyResizable && minResizeDp != null && minResizeDp > 0) {
        minResizeDp
    } else defaultDp
    val maximum = if (verticallyResizable && maxResizeDp != null && maxResizeDp > 0) {
        maxResizeDp.coerceAtLeast(minimum)
    } else Int.MAX_VALUE
    return minimum..maximum
}

fun widgetRows(
    placements: List<WidgetPlacement>,
    isHalfWidth: (WidgetPlacement) -> Boolean,
): List<WidgetRow> = buildList {
    var pending: WidgetPlacement? = null
    for (placement in placements) {
        if (isHalfWidth(placement)) {
            val first = pending
            if (first == null) pending = placement
            else {
                add(WidgetRow(first, placement))
                pending = null
            }
        } else {
            pending?.let { add(WidgetRow(it)) }
            pending = null
            add(WidgetRow(placement))
        }
    }
    pending?.let { add(WidgetRow(it)) }
}

fun moveWidget(placements: List<WidgetPlacement>, id: Int, direction: Int): List<WidgetPlacement> {
    require(direction == -1 || direction == 1)
    val index = placements.indexOfFirst { it.id == id }
    val next = index + direction
    if (index < 0 || next !in placements.indices) return placements
    return placements.toMutableList().apply {
        val displaced = this[next]
        this[next] = this[index]
        this[index] = displaced
    }
}

fun resizedWidgetHeight(startDp: Int, deltaPx: Float, density: Float, range: IntRange): Int {
    require(startDp > 0 && density > 0 && !range.isEmpty())
    return (startDp.toLong() + (deltaPx / density).roundToInt()).coerceIn(
        range.first.toLong(), range.last.toLong(),
    ).toInt()
}

/** Horizontal drag is positive toward the outer edge of a half-width widget. */
fun widgetWidthAfterDrag(isHalf: Boolean, canMakeHalf: Boolean, outwardDp: Float): WidgetWidth? =
    when {
        isHalf && outwardDp >= 32f -> WidgetWidth.FULL
        !isHalf && canMakeHalf && outwardDp <= -32f -> WidgetWidth.HALF
        else -> null
    }
