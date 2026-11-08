package app.indexlauncher.model

const val MAX_WIDGET_PAGES = 5

/** Ordinals identify screens, not their current position. Apps are always rightmost. */
sealed interface HomePage {
    data object Apps : HomePage {
        const val key = "home"
    }
    data class Widgets(val ordinal: Int) : HomePage {
        init { require(ordinal in 1..MAX_WIDGET_PAGES) }
        val key: String get() = "left-$ordinal"
    }
}

/** Order is nearest Home first, including hidden screens at the end. */
data class PageCounts(val left: Int = 1, val order: List<Int> = (1..MAX_WIDGET_PAGES).toList()) {
    init {
        require(left in 0..MAX_WIDGET_PAGES)
        require(order.size == MAX_WIDGET_PAGES && order.toSet() == (1..MAX_WIDGET_PAGES).toSet())
    }

    val visibleOrdinals: List<Int> get() = order.take(left)
    val pageCount: Int get() = left + 1
    val appsIndex: Int get() = left

    fun pageAt(index: Int): HomePage {
        require(index in 0..left)
        return if (index == appsIndex) HomePage.Apps else HomePage.Widgets(order[left - index - 1])
    }

    fun ordinalAt(index: Int): Int = when (val page = pageAt(index)) {
        HomePage.Apps -> 0
        is HomePage.Widgets -> page.ordinal
    }

    /** Keep the same screen when reordered; hiding it selects the outermost remaining screen. */
    fun indexOfOrdinal(ordinal: Int): Int {
        if (ordinal == 0) return appsIndex
        val position = visibleOrdinals.indexOf(ordinal)
        return if (position >= 0) left - position - 1 else 0
    }

    fun withVisibleOrder(nearestFirst: List<Int>): PageCounts {
        require(nearestFirst.size == left && nearestFirst.toSet() == visibleOrdinals.toSet())
        return copy(order = nearestFirst + order.drop(left))
    }
}

/** Repair missing, duplicate, or invalid stored identities without losing any valid screens. */
fun normalizedPageOrder(ordinals: List<Int>): List<Int> =
    (ordinals.filter { it in 1..MAX_WIDGET_PAGES } + (1..MAX_WIDGET_PAGES)).distinct()

fun movePage(order: List<Int>, ordinal: Int, direction: Int): List<Int> {
    require(direction == -1 || direction == 1)
    val from = order.indexOf(ordinal)
    val to = from + direction
    if (from < 0 || to !in order.indices) return order
    return order.toMutableList().apply { add(to, removeAt(from)) }
}
