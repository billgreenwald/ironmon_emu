package org.libsdl.app

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View

internal open class SDLGenericMotionListener_API24 : SDLGenericMotionListener_API12() {
    // Generic Motion (mouse hover, joystick...) events go here
    private var mRelativeModeEnabled = false
    override fun onGenericMotion(v: View, event: MotionEvent): Boolean {

        // Handle relative mouse mode
        if (mRelativeModeEnabled) {
            if (event.source == InputDevice.SOURCE_MOUSE) {
                val action = event.actionMasked
                if (action == MotionEvent.ACTION_HOVER_MOVE) {
                    val x = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                    val y = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                    SDLActivity.onNativeMouse(0, action, x, y, true)
                    return true
                }
            }
        }

        // Event was not managed, call SDLGenericMotionListener_API12 method
        return super.onGenericMotion(v, event)
    }

    override fun supportsRelativeMouse(): Boolean {
        return true
    }

    override fun inRelativeMode(): Boolean {
        return mRelativeModeEnabled
    }

    override fun setRelativeMouseEnabled(enabled: Boolean): Boolean {
        mRelativeModeEnabled = enabled
        return true
    }

    override fun getEventX(event: MotionEvent): Float {
        return if (mRelativeModeEnabled) {
            event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
        } else {
            event.getX(0)
        }
    }

    override fun getEventY(event: MotionEvent): Float {
        return if (mRelativeModeEnabled) {
            event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
        } else {
            event.getY(0)
        }
    }
}
