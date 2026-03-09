package org.libsdl.app

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.View.OnGenericMotionListener
import org.libsdl.app.SDLControllerManager.handleJoystickMotionEvent

open class SDLGenericMotionListener_API12 : OnGenericMotionListener {
    // Generic Motion (mouse hover, joystick...) events go here
    override fun onGenericMotion(v: View, event: MotionEvent): Boolean {
        val x: Float
        val y: Float
        val action: Int
        when (event.source) {
            InputDevice.SOURCE_JOYSTICK -> return handleJoystickMotionEvent(event)
            InputDevice.SOURCE_MOUSE -> {
                action = event.actionMasked
                when (action) {
                    MotionEvent.ACTION_SCROLL -> {
                        x = event.getAxisValue(MotionEvent.AXIS_HSCROLL, 0)
                        y = event.getAxisValue(MotionEvent.AXIS_VSCROLL, 0)
                        SDLActivity.onNativeMouse(0, action, x, y, false)
                        return true
                    }

                    MotionEvent.ACTION_HOVER_MOVE -> {
                        x = event.getX(0)
                        y = event.getY(0)
                        SDLActivity.onNativeMouse(0, action, x, y, false)
                        return true
                    }

                    else -> {}
                }
            }

            else -> {}
        }

        // Event was not managed
        return false
    }

    open fun supportsRelativeMouse(): Boolean {
        return false
    }

    open fun inRelativeMode(): Boolean {
        return false
    }

    open fun setRelativeMouseEnabled(enabled: Boolean): Boolean {
        return false
    }

    open fun reclaimRelativeMouseModeIfNeeded() {}
    open fun getEventX(event: MotionEvent): Float {
        return event.getX(0)
    }

    open fun getEventY(event: MotionEvent): Float {
        return event.getY(0)
    }
}
