package app.indexlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteAppsTest {
    private val clock = AppEntry("example.clock", "MainActivity", "Clock")
    private val maps = AppEntry("example.maps", "MainActivity", "Maps")
    private val camera = AppEntry("example.camera", "MainActivity", "Camera")

    @Test fun addingThenRemovingAFavoritePreservesOtherOrdering() {
        val ids = listOf(clock.id, maps.id)
        assertEquals(listOf(clock.id, maps.id, camera.id), toggleFavorite(ids, camera.id))
        assertEquals(listOf(clock.id), toggleFavorite(ids, maps.id))
    }

    @Test fun missingAppsAreNotShownButReturnInTheirSavedPositionOnReinstall() {
        val ids = listOf(maps.id, clock.id)
        assertEquals(listOf(clock), favoriteApps(listOf(clock, camera), ids))
        assertEquals(listOf(maps, clock), favoriteApps(listOf(clock, maps, camera), ids))
    }

    @Test fun emptyFavoritesDoNotDependOnAlphabeticalSorting() {
        assertEquals(emptyList<AppEntry>(), favoriteApps(listOf(clock, camera), emptyList()))
        assertEquals(listOf(clock, camera), favoriteApps(listOf(camera, clock), listOf(clock.id, camera.id)))
    }

    @Test fun sameComponentInWorkProfileCanBeFavoritedIndependently() {
        val workClock = clock.copy(profileSerial = 42)
        assertEquals("example.clock/MainActivity", clock.id)
        assertEquals("profile:42:example.clock/MainActivity", workClock.id)
        assertEquals(
            listOf(workClock, clock),
            favoriteApps(listOf(clock, workClock), listOf(workClock.id, clock.id)),
        )
        assertEquals(listOf(clock.id), toggleFavorite(listOf(clock.id, workClock.id), workClock.id))
        assertEquals(listOf(clock), favoriteApps(listOf(clock, workClock), listOf(clock.id)))
    }

    @Test fun savedWorkFavoriteIsHiddenWhileProfileIsUnavailable() {
        val workClock = clock.copy(profileSerial = 42)
        val ids = listOf(workClock.id, clock.id)
        assertEquals(listOf(clock), favoriteApps(listOf(clock), ids))
        assertEquals(listOf(workClock, clock), favoriteApps(listOf(clock, workClock), ids))
    }
}
