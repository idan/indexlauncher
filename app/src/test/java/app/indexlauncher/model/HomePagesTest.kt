package app.indexlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HomePagesTest {
    @Test fun appsAreRightmostAndWidgetsGrowLeft() {
        val pages = PageCounts(left = 3)
        assertEquals(4, pages.pageCount)
        assertEquals(HomePage.Widgets(3), pages.pageAt(0))
        assertEquals(HomePage.Widgets(2), pages.pageAt(1))
        assertEquals(HomePage.Widgets(1), pages.pageAt(2))
        assertEquals(HomePage.Apps, pages.pageAt(pages.appsIndex))
        assertEquals("left-1", (pages.pageAt(2) as HomePage.Widgets).key)
    }

    @Test fun neitherEndWrapsAround() {
        val pages = PageCounts()
        assertThrows(IllegalArgumentException::class.java) { pages.pageAt(-1) }
        assertThrows(IllegalArgumentException::class.java) { pages.pageAt(2) }
    }

    @Test fun zeroWidgetsLeavesOnlyApps() {
        val pages = PageCounts(left = 0)
        assertEquals(1, pages.pageCount)
        assertEquals(HomePage.Apps, pages.pageAt(0))
        assertEquals("home", HomePage.Apps.key)
        assertNotEquals(HomePage.Apps.key, HomePage.Widgets(1).key)
    }

    @Test fun nearestWidgetKeepsItsIdentityWhenPagesAreAdded() {
        assertEquals(PageCounts(1).pageAt(0), PageCounts(3).pageAt(2))
        assertEquals(0, PageCounts(1).indexOfOrdinal(1))
        assertEquals(2, PageCounts(3).indexOfOrdinal(1))
        assertEquals(3, PageCounts(3).indexOfOrdinal(0))
    }

    @Test fun hidingCurrentOuterPageClampsToLastWidgetNotApps() {
        assertEquals(0, PageCounts(2).indexOfOrdinal(5))
        assertEquals(0, PageCounts(0).indexOfOrdinal(5))
    }

    @Test fun reorderedPagesKeepTheirKeysAndAppsStayRightmost() {
        val pages = PageCounts(3).withVisibleOrder(listOf(3, 1, 2))
        assertEquals(HomePage.Widgets(2), pages.pageAt(0))
        assertEquals(HomePage.Widgets(1), pages.pageAt(1))
        assertEquals(HomePage.Widgets(3), pages.pageAt(2))
        assertEquals(HomePage.Apps, pages.pageAt(3))
        assertEquals(3, pages.ordinalAt(2))
        assertEquals(0, pages.ordinalAt(3))
        assertEquals(2, pages.indexOfOrdinal(3))
        assertEquals("left-3", (pages.pageAt(2) as HomePage.Widgets).key)
    }

    @Test fun hidingAndRestoringUsesChosenOrderWithoutTouchingHiddenScreens() {
        val ordered = PageCounts(3).withVisibleOrder(listOf(3, 1, 2))
        val hidden = ordered.copy(left = 2)
        assertEquals(listOf(3, 1), hidden.visibleOrdinals)
        assertEquals(0, hidden.indexOfOrdinal(2))
        val moved = hidden.withVisibleOrder(listOf(1, 3))
        assertEquals(listOf(1, 3, 2, 4, 5), moved.order)
        assertEquals(listOf(1, 3, 2), moved.copy(left = 3).visibleOrdinals)
    }

    @Test fun malformedSavedOrderIsRepaired() {
        assertEquals(listOf(1, 2, 3, 4, 5), normalizedPageOrder(emptyList()))
        assertEquals(listOf(3, 1, 2, 4, 5), normalizedPageOrder(listOf(3, 3, -1, 1, 6)))
    }

    @Test fun accessibleMovesHaveFiniteBoundaries() {
        assertEquals(listOf(3, 1, 2), movePage(listOf(3, 2, 1), 1, -1))
        assertEquals(listOf(3, 2, 1), movePage(listOf(3, 2, 1), 3, -1))
        assertEquals(listOf(3, 2, 1), movePage(listOf(3, 2, 1), 1, 1))
        assertEquals(listOf(3, 2, 1), movePage(listOf(3, 2, 1), 5, 1))
        assertThrows(IllegalArgumentException::class.java) { movePage(listOf(1, 2), 1, 0) }
    }

    @Test fun invalidOrdersAndReorderingHiddenScreensAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { PageCounts(2, listOf(1, 1, 3, 4, 5)) }
        assertThrows(IllegalArgumentException::class.java) { PageCounts(2, listOf(1, 2)) }
        assertThrows(IllegalArgumentException::class.java) { PageCounts(2).withVisibleOrder(listOf(1, 3)) }
        assertThrows(IllegalArgumentException::class.java) { PageCounts(2).withVisibleOrder(listOf(1, 1)) }
    }

    @Test fun invalidCountsAndOrdinalsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { PageCounts(-1) }
        assertThrows(IllegalArgumentException::class.java) { PageCounts(6) }
        assertThrows(IllegalArgumentException::class.java) { HomePage.Widgets(0) }
        assertThrows(IllegalArgumentException::class.java) { HomePage.Widgets(6) }
    }
}
