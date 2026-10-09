package app.indexlauncher.model

const val MAX_WIDGET_PAGES = 5

/** Apps are rightmost; page 1 is the nearest widget page and new pages grow to the left. */
sealed interface HomePage {
    data object Apps : HomePage {
        const val key = "home"
    }
    data class Widgets(val ordinal: Int) : HomePage {
        init { require(ordinal in 1..MAX_WIDGET_PAGES) }
        val key: String get() = "left-$ordinal"
    }
}

data class PageCounts(val left: Int = 1) {
    init { require(left in 0..MAX_WIDGET_PAGES) }

    val pageCount: Int get() = left + 1
    val appsIndex: Int get() = left

    fun pageAt(index: Int): HomePage {
        require(index in 0..left)
        return if (index == appsIndex) HomePage.Apps else HomePage.Widgets(left - index)
    }

    /** A logical ordinal survives adding/removing outer pages, unlike its pager index. */
    fun indexOfOrdinal(ordinal: Int): Int = left - ordinal.coerceIn(0, left)
}
