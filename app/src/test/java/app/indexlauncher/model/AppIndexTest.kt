package app.indexlauncher.model

import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppIndexTest {
    private val apps = listOf(
        AppEntry("example.camera", "CameraActivity", "Camera"),
        AppEntry("example.chess", "ChessActivity", "Chess"),
        AppEntry("example.notes", "NotesActivity", "Notes"),
    )

    @Test fun searchIsCaseInsensitiveAndPreservesOrder() {
        assertEquals(apps.take(2), searchApps(apps, "  C  "))
        assertEquals(apps, searchApps(apps, " "))
    }

    @Test fun searchMatchesAnywhereInLabelsAndHandlesNoResults() {
        assertEquals(listOf(apps[0]), searchApps(apps, "  MER  "))
        assertEquals(emptyList<AppEntry>(), searchApps(apps, "no matching app"))
    }

    @Test fun searchAndSectionsKeepPersonalAndWorkInstallations() {
        val workCamera = apps[0].copy(profileSerial = 42)
        val both = listOf(apps[0], workCamera)
        assertEquals(both, searchApps(both, "camera"))
        assertEquals(both, groupAppsByLetter(both).single().apps)
        assertEquals(2, both.map(AppEntry::id).distinct().size)
    }

    @Test fun sectionsIncludeAccentsAndFallback() {
        assertEquals('E', sectionFor("Émail"))
        assertEquals('#', sectionFor("123 Tasks"))
        assertEquals('#', sectionFor("שלום"))
        assertEquals('#', sectionFor("العربية"))
        assertEquals('#', sectionFor(""))
        assertEquals(listOf('C', 'N'), groupAppsByLetter(apps).map { it.letter })
        assertEquals(apps.take(2), groupAppsByLetter(apps).first().apps)
        assertEquals(
            listOf('#', 'C', 'N'),
            groupAppsByLetter(listOf(AppEntry("example.123", "MainActivity", "123 Tasks")) + apps).map { it.letter },
        )
    }

    @Test fun untransliteratedAppsRemainSearchableAndVisibleInHashSection() {
        val hebrew = AppEntry("example.hebrew", "MainActivity", "שלום")
        val arabic = AppEntry("example.arabic", "MainActivity", "العربية")
        val sections = groupAppsByLetter(listOf(hebrew, arabic) + apps)
        assertEquals(listOf(hebrew, arabic), sections.first().apps)
        assertEquals('#', sections.first().letter)
        assertEquals(listOf(hebrew), searchApps(sections.flatMap { it.apps }, "לום"))
    }

    @Test fun romanizedKeyIndexesSortsAndSearchesWithoutReplacingOriginalLabel() {
        val bank = AppEntry(
            "example.bank", "MainActivity", "בנק דיסקונט",
            indexLabel = preferredIndexLabel("bnq dysqwnt", "Discount Bank"),
            englishLabel = "Discount Bank",
        )
        val sorted = sortAppsForIndex(listOf(apps[0], bank, apps[1]), Collator.getInstance(Locale.ENGLISH))
        assertEquals(listOf(bank, apps[0], apps[1]), sorted)
        assertEquals(listOf('B', 'C'), groupAppsByLetter(sorted).map { it.letter })
        assertEquals(listOf(bank), searchApps(sorted, "BNQ"))
        assertEquals(listOf(bank), searchApps(sorted, "דיסקונט"))
        assertEquals(listOf(bank), searchApps(sorted, "discount"))
        assertEquals("בנק דיסקונט", bank.label)
        assertEquals("bnq dysqwnt", bank.indexLabel)
        assertEquals("example.bank/MainActivity", bank.id)
    }

    @Test fun englishLabelOnlySuppliesTheRailLetterIfRomanizationHasNoLatinInitial() {
        assertEquals("bnq dysqwnt", preferredIndexLabel("bnq dysqwnt", "Discount Bank"))
        assertEquals("Discount Bank", preferredIndexLabel("שלום", "Discount Bank"))
        assertEquals("שלום", preferredIndexLabel("שלום", null))
        assertEquals("שלום", preferredIndexLabel("שלום", "123 Bank"))
        val fallback = AppEntry(
            "example.fallback", "MainActivity", "שלום",
            indexLabel = preferredIndexLabel("שלום", "Discount Bank"), englishLabel = "Discount Bank",
        )
        assertEquals('D', groupAppsByLetter(listOf(fallback)).single().letter)
        assertEquals(listOf(fallback), searchApps(listOf(fallback), "Discount"))
        val numeric = AppEntry(
            "example.numeric", "MainActivity", "שלום",
            indexLabel = preferredIndexLabel("שלום", "123 Bank"), englishLabel = "123 Bank",
        )
        assertEquals('#', groupAppsByLetter(listOf(numeric)).single().letter)
        assertEquals(listOf(numeric), searchApps(listOf(numeric), "123"))
    }

    @Test fun alternateLabelsStayAttachedToTheirProfileSpecificActivity() {
        val personal = AppEntry(
            "example.bank", "MainActivity", "בנק דיסקונט",
            indexLabel = "bnq dysqwnt", englishLabel = "Discount Bank",
        )
        val work = personal.copy(profileSerial = 42, englishLabel = null)
        assertEquals(listOf(personal), searchApps(listOf(personal, work), "discount"))
        assertEquals(listOf(personal, work), searchApps(listOf(personal, work), "בנק"))
        assertEquals(2, listOf(personal, work).map(AppEntry::id).distinct().size)
    }
}
