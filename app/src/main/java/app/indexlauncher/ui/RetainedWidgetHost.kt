package app.indexlauncher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.ViewGroup

/** One host view per bound ID, shared by the page and its on-demand snapshot renderer. */
internal class RetainedWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    private val views = mutableMapOf<Int, AppWidgetHostView>()

    fun obtainView(context: Context, id: Int, info: AppWidgetProviderInfo): AppWidgetHostView {
        val view = views.getOrPut(id) { createView(context, id, info) }
        // Transfer ownership even if Compose has not released the outgoing frame yet.
        // There is still only one live view/listener for this ID.
        (view.parent as? ViewGroup)?.removeView(view)
        view.setAppWidget(id, info)
        return view
    }

    override fun deleteAppWidgetId(appWidgetId: Int) {
        views.remove(appWidgetId)
        super.deleteAppWidgetId(appWidgetId)
    }

    override fun onAppWidgetRemoved(appWidgetId: Int) {
        views.remove(appWidgetId)
        super.onAppWidgetRemoved(appWidgetId)
    }
}
