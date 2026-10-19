package app.indexlauncher.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.indexlauncher.R
import app.indexlauncher.SettingsActivity
import app.indexlauncher.data.ItemSpacingRepository
import app.indexlauncher.model.ItemSpacing
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemSpacingSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<SettingsActivity>()
    private var original: ItemSpacing? = null

    @After fun restoreSpacing() {
        original?.let { ItemSpacingRepository(compose.activity).setSpacing(it) }
    }

    @Test fun sliderPersistsAndRestoresChosenPadding() {
        val repository = ItemSpacingRepository(compose.activity)
        original = repository.spacing.value
        val title = compose.activity.getString(R.string.item_spacing_title)
        compose.onNodeWithContentDescription(title).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(28f) }
        assertEquals(ItemSpacing.fromPadding(28), ItemSpacingRepository(compose.activity).spacing.value)
        repository.refresh()
        assertEquals(ItemSpacing.fromPadding(28), repository.spacing.value)

        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(compose.activity.getString(R.string.item_spacing_value, 28))
            .performScrollTo().assertIsDisplayed()
    }
}
