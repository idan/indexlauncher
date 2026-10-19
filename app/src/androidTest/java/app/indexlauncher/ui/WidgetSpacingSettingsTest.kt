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
import app.indexlauncher.data.WidgetSpacingRepository
import app.indexlauncher.model.WidgetSpacing
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetSpacingSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<SettingsActivity>()
    private var original: WidgetSpacing? = null

    @After fun restoreSpacing() {
        original?.let { WidgetSpacingRepository(compose.activity).setSpacing(it) }
    }

    @Test fun independentSlidersPersistAndRestoreAllWidgetSpacingValues() {
        val repository = WidgetSpacingRepository(compose.activity)
        original = repository.spacing.value
        setSlider(R.string.widget_outer_padding, 0)
        assertEquals(original!!.copy(outerPaddingDp = 0), WidgetSpacingRepository(compose.activity).spacing.value)
        setSlider(R.string.widget_column_gap, 24)
        setSlider(R.string.widget_row_gap, 40)
        val expected = WidgetSpacing(0, 24, 40)
        assertEquals(expected, WidgetSpacingRepository(compose.activity).spacing.value)
        repository.refresh()
        assertEquals(expected, repository.spacing.value)

        compose.activityRule.scenario.recreate()
        for ((title, value) in listOf(
            R.string.widget_outer_padding to 0,
            R.string.widget_column_gap to 24,
            R.string.widget_row_gap to 40,
        )) {
            compose.onNodeWithText(compose.activity.getString(
                R.string.widget_spacing_value, compose.activity.getString(title), value,
            )).performScrollTo().assertIsDisplayed()
        }
    }

    private fun setSlider(title: Int, value: Int) {
        compose.onNodeWithContentDescription(compose.activity.getString(title)).performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(value.toFloat()) }
    }
}
