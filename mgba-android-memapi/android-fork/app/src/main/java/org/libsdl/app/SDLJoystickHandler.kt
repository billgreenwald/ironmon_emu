package org.libsdl.app

import android.view.MotionEvent

internal open class SDLJoystickHandler {
    /**
     * Handles given MotionEvent.
     *
     * @param event the event to be handled.
     * @return if given event was processed.
     */
    open fun handleMotionEvent(event: MotionEvent?): Boolean {
        return false
    }

    /**
     * Handles adding and removing of input devices.
     */
    open fun pollInputDevices() {}
}
