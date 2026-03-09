package org.libsdl.app

import android.view.InputDevice
import android.view.KeyEvent

internal class SDLJoystickHandler_API19 : SDLJoystickHandler_API16() {
    override fun getProductId(joystickDevice: InputDevice?): Int {
        return joystickDevice!!.productId
    }

    override fun getVendorId(joystickDevice: InputDevice?): Int {
        return joystickDevice!!.vendorId
    }

    override fun getButtonMask(joystickDevice: InputDevice?): Int {
        var button_mask = 0
        val keys = intArrayOf(
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_BUTTON_MODE,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_DPAD_CENTER,  // These don't map into any SDL controller buttons directly
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_C,
            KeyEvent.KEYCODE_BUTTON_Z,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_BUTTON_3,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_BUTTON_5,
            KeyEvent.KEYCODE_BUTTON_6,
            KeyEvent.KEYCODE_BUTTON_7,
            KeyEvent.KEYCODE_BUTTON_8,
            KeyEvent.KEYCODE_BUTTON_9,
            KeyEvent.KEYCODE_BUTTON_10,
            KeyEvent.KEYCODE_BUTTON_11,
            KeyEvent.KEYCODE_BUTTON_12,
            KeyEvent.KEYCODE_BUTTON_13,
            KeyEvent.KEYCODE_BUTTON_14,
            KeyEvent.KEYCODE_BUTTON_15,
            KeyEvent.KEYCODE_BUTTON_16
        )
        val masks = intArrayOf(
            1 shl 0,  // A -> A
            1 shl 1,  // B -> B
            1 shl 2,  // X -> X
            1 shl 3,  // Y -> Y
            1 shl 4,  // BACK -> BACK
            1 shl 6,  // MENU -> START
            1 shl 5,  // MODE -> GUIDE
            1 shl 6,  // START -> START
            1 shl 7,  // THUMBL -> LEFTSTICK
            1 shl 8,  // THUMBR -> RIGHTSTICK
            1 shl 9,  // L1 -> LEFTSHOULDER
            1 shl 10,  // R1 -> RIGHTSHOULDER
            1 shl 11,  // DPAD_UP -> DPAD_UP
            1 shl 12,  // DPAD_DOWN -> DPAD_DOWN
            1 shl 13,  // DPAD_LEFT -> DPAD_LEFT
            1 shl 14,  // DPAD_RIGHT -> DPAD_RIGHT
            1 shl 4,  // SELECT -> BACK
            1 shl 0,  // DPAD_CENTER -> A
            1 shl 15,  // L2 -> ??
            1 shl 16,  // R2 -> ??
            1 shl 17,  // C -> ??
            1 shl 18,  // Z -> ??
            1 shl 20,  // 1 -> ??
            1 shl 21,  // 2 -> ??
            1 shl 22,  // 3 -> ??
            1 shl 23,  // 4 -> ??
            1 shl 24,  // 5 -> ??
            1 shl 25,  // 6 -> ??
            1 shl 26,  // 7 -> ??
            1 shl 27,  // 8 -> ??
            1 shl 28,  // 9 -> ??
            1 shl 29,  // 10 -> ??
            1 shl 30,  // 11 -> ??
            1 shl 31,  // 12 -> ??
            // We're out of room...
            -0x1,  // 13 -> ??
            -0x1,  // 14 -> ??
            -0x1,  // 15 -> ??
            -0x1
        )
        val has_keys = joystickDevice!!.hasKeys(*keys)
        for (i in keys.indices) {
            if (has_keys[i]) {
                button_mask = button_mask or masks[i]
            }
        }
        return button_mask
    }
}
