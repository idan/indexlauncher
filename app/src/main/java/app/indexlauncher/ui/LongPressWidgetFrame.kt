package app.indexlauncher.ui

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/** Observes touch before a provider's child views consume it, without interrupting ordinary widget gestures. */
internal class LongPressWidgetFrame(context: Context) : FrameLayout(context) {
    var isEditing = false
    var onWidgetLongPress: (() -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var downEvent: MotionEvent? = null
    private var tracking = false
    private var consuming = false
    private val triggerLongPress = Runnable {
        val onLongPress = onWidgetLongPress
        if (tracking && !isEditing && onLongPress != null) {
            tracking = false
            consuming = true
            downEvent?.let { down ->
                val cancel = MotionEvent.obtain(down)
                cancel.action = MotionEvent.ACTION_CANCEL
                cancel.setLocation(downX, downY)
                super.dispatchTouchEvent(cancel)
                cancel.recycle()
            }
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongPress()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isEditing) {
            stopTracking()
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                consuming = false
                stopTracking()
                if (onWidgetLongPress != null) {
                    downX = event.x
                    downY = event.y
                    downEvent = MotionEvent.obtain(event)
                    tracking = true
                    postDelayed(triggerLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (tracking && (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop)) {
                    stopTracking()
                }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopTracking()
        }
        if (consuming) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                consuming = false
            }
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        stopTracking()
        super.onDetachedFromWindow()
    }

    private fun stopTracking() {
        tracking = false
        removeCallbacks(triggerLongPress)
        downEvent?.recycle()
        downEvent = null
    }
}
