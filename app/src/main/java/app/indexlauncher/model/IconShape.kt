package app.indexlauncher.model

/** Launcher-only choice; the device default is rendered by Android's AdaptiveIconDrawable. */
enum class IconShape {
    DeviceDefault,
    Circle,
    RoundedSquare,
    Squircle;

    companion object {
        fun fromStored(value: String?): IconShape = entries.firstOrNull { it.name == value } ?: DeviceDefault
    }
}
