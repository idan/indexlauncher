package app.indexlauncher.data

import android.content.Context
import app.indexlauncher.model.IconShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Appearance setting used only within Index Launcher; it does not modify another launcher's shape picker. */
class IconShapeRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _shape = MutableStateFlow(readShape())
    val shape = _shape.asStateFlow()

    fun refresh() {
        _shape.value = readShape()
    }

    fun setShape(shape: IconShape) {
        prefs.edit().putString("icon_shape", shape.name).apply()
        _shape.value = shape
    }

    private fun readShape() = IconShape.fromStored(prefs.getString("icon_shape", null))
}
