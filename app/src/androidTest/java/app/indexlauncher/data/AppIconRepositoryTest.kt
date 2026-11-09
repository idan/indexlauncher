package app.indexlauncher.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.indexlauncher.MainActivity
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = AppIconRepository(context)
    private val app = AppEntry(context.packageName, MainActivity::class.java.name, "Index Launcher")

    @Test fun loadedIconIsCachedByCatalogShapeAndProfile() = runBlocking {
        assertNull(repository.getCachedIcon(app, 1, IconShape.Circle))
        val icon = repository.loadIcon(app, 1, IconShape.Circle)
        assertNotNull(icon)
        assertSame(icon, repository.getCachedIcon(app, 1, IconShape.Circle))
        assertNull(repository.getCachedIcon(app, 2, IconShape.Circle))
        assertNull(repository.getCachedIcon(app, 1, IconShape.Squircle))
        assertNull(repository.getCachedIcon(app.copy(profileSerial = Long.MAX_VALUE), 1, IconShape.Circle))

        // A hit must finish synchronously, without a trip to Dispatchers.IO.
        var completed = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            assertSame(icon, repository.loadIcon(app, 1, IconShape.Circle))
            completed = true
        }
        assertTrue(completed)
        job.join()
    }

    @Test fun preloadMakesIconsAvailableBeforeRowsRequestThem() = runBlocking {
        repository.preloadIcons(listOf(app), 1, IconShape.Circle)
        val icon = repository.getCachedIcon(app, 1, IconShape.Circle)
        assertNotNull(icon)
        assertSame(icon, repository.loadIcon(app, 1, IconShape.Circle))
    }

    @Test fun concurrentRequestsShareTheSameRenderedBitmap() = runBlocking {
        val icons = List(12) {
            async(Dispatchers.IO) { repository.loadIcon(app, 1, IconShape.Circle) }
        }.awaitAll()
        assertNotNull(icons.first())
        icons.forEach { assertSame(icons.first(), it) }
    }

    @Test fun clearRemovesCachedIcons() = runBlocking {
        assertNotNull(repository.loadIcon(app, 1, IconShape.Circle))
        repository.clear()
        assertNull(repository.getCachedIcon(app, 1, IconShape.Circle))
    }

    @Test fun unavailableIconsStillFallBackWithoutCrashing() = runBlocking {
        val missing = AppEntry("app.indexlauncher.nonexistent", "Missing", "Missing")
        repository.preloadIcons(listOf(missing, app), 1, IconShape.Circle)
        assertNull(repository.loadIcon(missing, 1, IconShape.Circle))
        assertNotNull(repository.getCachedIcon(app, 1, IconShape.Circle))
    }
}
