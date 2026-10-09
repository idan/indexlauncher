package app.indexlauncher.model

import java.text.Collator
import java.text.Normalizer

/** A launchable activity (an app may expose more than one). */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val profileSerial: Long? = null,
    val indexLabel: String = label,
    val englishLabel: String? = null,
) {
    // Keep existing personal-app IDs so saved favorites survive the profile-aware catalog.
    val id: String get() = if (profileSerial == null) "$packageName/$activityName"
        else "profile:$profileSerial:$packageName/$activityName"
}

data class AppSection(val letter: Char, val apps: List<AppEntry>)

/** Sort by the same Latin key used for sections, without changing the displayed label. */
fun sortAppsForIndex(apps: List<AppEntry>, collator: Collator): List<AppEntry> =
    apps.sortedWith(compareBy<AppEntry, String>(collator) { it.indexLabel }.thenBy { it.id })

/** Keep the repository's alphabetical ordering while narrowing the list. */
fun searchApps(apps: List<AppEntry>, query: String): List<AppEntry> {
    val term = query.trim()
    return if (term.isEmpty()) apps else apps.filter {
        it.label.contains(term, ignoreCase = true) ||
            it.indexLabel.contains(term, ignoreCase = true) ||
            it.englishLabel?.contains(term, ignoreCase = true) == true
    }
}

/** Numeric and untransliterated initials live under #; Latin names use A–Z. */
fun sectionFor(label: String): Char {
    val first = Normalizer.normalize(label.trimStart(), Normalizer.Form.NFD)
        .firstOrNull()?.uppercaseChar()
    return first?.takeIf { it in 'A'..'Z' } ?: '#'
}

fun preferredIndexLabel(romanizedLabel: String, englishLabel: String?): String =
    if (sectionFor(romanizedLabel) == '#' && englishLabel != null && sectionFor(englishLabel) != '#') {
        englishLabel
    } else romanizedLabel

fun groupAppsByLetter(apps: List<AppEntry>): List<AppSection> {
    val grouped = apps.groupBy { sectionFor(it.indexLabel) }
    return (listOf('#') + ('A'..'Z')).mapNotNull { letter ->
        grouped[letter]?.let { AppSection(letter, it) }
    }
}
