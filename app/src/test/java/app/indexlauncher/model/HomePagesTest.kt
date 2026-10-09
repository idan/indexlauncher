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

    @Test fun invalidCountsAndOrdinalsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { PageCounts(-1) }
        assertThrows(IllegalArgumentException::class.java) { PageCounts(6) }
        assertThrows(IllegalArgumentException::class.java) { HomePage.Widgets(0) }
        assertThrows(IllegalArgumentException::class.java) { HomePage.Widgets(6) }
    }
}
