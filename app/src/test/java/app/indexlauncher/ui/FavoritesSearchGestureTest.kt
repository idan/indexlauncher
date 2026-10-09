package app.indexlauncher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesSearchGestureTest {
    @Test fun shortListOpensSearchOnlyAfterAFullUpwardOverscroll() {
        val detector = SearchOverscrollDetector(72f)
        detector.onDrag(-30f, canScrollForward = false)
        assertFalse(detector.release(canScrollForward = false))
        detector.onDrag(-40f, canScrollForward = false)
        detector.onDrag(-35f, canScrollForward = false)
        assertTrue(detector.release(canScrollForward = false))
        assertFalse(detector.release(canScrollForward = false))
    }

    @Test fun scrollingFavoritesNormallyDoesNotTriggerSearchUntilPastTheEnd() {
        val detector = SearchOverscrollDetector(72f)
        detector.onDrag(-150f, canScrollForward = true)
        detector.onDrag(-30f, canScrollForward = false)
        assertFalse(detector.release(canScrollForward = false))
        detector.onDrag(-80f, canScrollForward = false)
        assertTrue(detector.release(canScrollForward = false))
    }

    @Test fun reversingDirectionOrCancellingAbandonsTheGesture() {
        val detector = SearchOverscrollDetector(72f)
        detector.onDrag(-100f, canScrollForward = false)
        detector.onDrag(10f, canScrollForward = false)
        assertFalse(detector.release(canScrollForward = false))
        detector.onDrag(-100f, canScrollForward = false)
        detector.reset()
        assertFalse(detector.release(canScrollForward = false))
        detector.onDrag(-100f, canScrollForward = false)
        assertFalse(detector.release(canScrollForward = true))
    }
}
