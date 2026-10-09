package app.indexlauncher.data

import app.indexlauncher.model.WidgetPlacement
import app.indexlauncher.model.WidgetWidth

internal fun encodeWidgetPlacements(placements: List<WidgetPlacement>): String =
    placements.joinToString(",") { "${it.id}:${it.width.name}:${it.heightDp ?: ""}" }

internal fun decodeWidgetPlacements(
    saved: String?,
    onInvalid: (String) -> Unit,
): List<WidgetPlacement> {
    if (saved == null) return emptyList()
    val seen = mutableSetOf<Int>()
    return saved.split(',').mapNotNull { entry ->
        val fields = entry.split(':')
        val id = fields[0].toIntOrNull()
        val width = if (fields.size == 1) WidgetWidth.FULL
            else WidgetWidth.entries.firstOrNull { it.name == fields[1] }
        val height = fields.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (id == null || id < 0 || width == null || fields.size > 3 ||
            (fields.size >= 3 && fields[2].isNotEmpty() && (height == null || height <= 0)) ||
            !seen.add(id)
        ) {
            onInvalid(entry)
            null
        } else WidgetPlacement(id, width, height)
    }
}
