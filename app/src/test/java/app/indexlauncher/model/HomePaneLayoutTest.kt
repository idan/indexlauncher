package app.indexlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePaneLayoutTest {
    @Test fun narrowOrShortWindowsUseOnePane() {
        assertNull(homePaneSplit(639f, 800f))
        assertNull(homePaneSplit(900f, 319f))
    }

    @Test fun wideWindowsSplitEvenly() {
        assertEquals(HomePaneSplit(400f, 400f), homePaneSplit(800f, 600f))
    }

    @Test fun safeInsetsAndFontScaleMustFitBothPanes() {
        assertNull(homePaneSplit(660f, 600f, safeLeft = 30f))
        assertNull(homePaneSplit(800f, 600f, fontScale = 1.5f))
        assertNull(homePaneSplit(800f, 360f, safeTop = 30f, safeBottom = 30f))
        assertEquals(HomePaneSplit(410f, 410f), homePaneSplit(820f, 600f, safeLeft = 20f, safeRight = 20f))
    }

    @Test fun verticalHingeIsExcludedAndCanBeOffCenter() {
        assertEquals(HomePaneSplit(350f, 375f), homePaneSplit(800f, 600f, fold = HomeFold(350f, 375f, true)))
        assertEquals(HomePaneSplit(400f, 400f), homePaneSplit(800f, 600f, fold = HomeFold(400f, 400f, true)))
    }

    @Test fun narrowHingePaneAndInvalidBoundsFallBackToSinglePane() {
        assertNull(homePaneSplit(800f, 600f, fold = HomeFold(300f, 320f, true)))
        assertNull(homePaneSplit(800f, 600f, fold = HomeFold(-1f, 350f, true)))
        assertNull(homePaneSplit(800f, 600f, fold = HomeFold(400f, 900f, true)))
        assertNull(homePaneSplit(800f, 600f, fold = HomeFold(420f, 400f, true)))
    }

    @Test fun tabletopPostureUsesSinglePaneRatherThanSideBySide() {
        assertNull(homePaneSplit(900f, 800f, fold = HomeFold(0f, 900f, false)))
    }
}
