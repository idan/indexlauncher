package app.indexlauncher.model

/** Bounds in window dp, with physical (not start/end) coordinates. */
data class HomeFold(val left: Float, val right: Float, val vertical: Boolean)

data class HomePaneSplit(val leftWidth: Float, val rightStart: Float)

/**
 * Use two panes only when both have usable space. A separating horizontal fold/tabletop posture
 * stays single-pane; a vertical hinge is excluded from both panes. No device-model detection.
 */
fun homePaneSplit(
    width: Float,
    height: Float,
    safeLeft: Float = 0f,
    safeRight: Float = 0f,
    safeTop: Float = 0f,
    safeBottom: Float = 0f,
    fontScale: Float = 1f,
    fold: HomeFold? = null,
): HomePaneSplit? {
    if (fold != null && !fold.vertical) return null
    if (height - safeTop - safeBottom < 320f) return null
    val split = if (fold != null) {
        if (fold.left < 0f || fold.right > width || fold.left > fold.right) return null
        HomePaneSplit(fold.left, fold.right)
    } else {
        val middle = safeLeft + (width - safeLeft - safeRight) / 2f
        HomePaneSplit(middle, middle)
    }
    val minimumWidth = 320f * fontScale.coerceAtLeast(1f)
    return split.takeIf {
        it.leftWidth - safeLeft >= minimumWidth && width - it.rightStart - safeRight >= minimumWidth
    }
}
