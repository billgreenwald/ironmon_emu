package hh.game.mgba_android.utils

import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent

object controllerUtil {
    var lastDirect = ArrayList<Int>()
    fun getDirectionPressed(event: InputEvent): Int {
        var directionPressed = -1

        if (event is MotionEvent) {
            var xaxis = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            var yaxis = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            // Fall back to left analog stick if hat is centered
            if (xaxis == 0f && yaxis == 0f) {
                val sx = event.getAxisValue(MotionEvent.AXIS_X)
                val sy = event.getAxisValue(MotionEvent.AXIS_Y)
                if (sx < -0.5f) xaxis = -1.0f
                else if (sx > 0.5f) xaxis = 1.0f
                if (sy < -0.5f) yaxis = -1.0f
                else if (sy > 0.5f) yaxis = 1.0f
            }
            Log.d("thedirection::","x:$xaxis y:$yaxis")
            when {
                xaxis == -1.0f -> directionPressed = KeyEvent.KEYCODE_DPAD_LEFT
                xaxis == 1.0f -> directionPressed = KeyEvent.KEYCODE_DPAD_RIGHT
                yaxis == -1.0f -> directionPressed = KeyEvent.KEYCODE_DPAD_UP
                yaxis == 1.0f -> directionPressed = KeyEvent.KEYCODE_DPAD_DOWN
                xaxis == 0f && yaxis == 0f -> directionPressed = 0
            }
            if (xaxis != 0f || yaxis != 0f) {
                lastDirect.add(directionPressed)
            }
        } else if (event is KeyEvent) {
            event.keyCode
        }
        return directionPressed
    }
}