package hh.game.mgba_android.utils

import android.view.KeyEvent

enum class GBAKeys(val key: Int) {
    GBA_KEY_A(KeyEvent.KEYCODE_X),
    GBA_KEY_B(KeyEvent.KEYCODE_Z),
    GBA_KEY_L(KeyEvent.KEYCODE_Q),
    GBA_KEY_R(KeyEvent.KEYCODE_U),
    GBA_KEY_START(KeyEvent.KEYCODE_Y),
    GBA_KEY_SELECT(KeyEvent.KEYCODE_N),
    GBA_KEY_UP(KeyEvent.KEYCODE_DPAD_UP),
    GBA_KEY_DOWN(KeyEvent.KEYCODE_DPAD_DOWN),
    GBA_KEY_LEFT(KeyEvent.KEYCODE_DPAD_LEFT),
    GBA_KEY_RIGHT(KeyEvent.KEYCODE_DPAD_RIGHT),
    // Non-GBA controller buttons — unique IDs so speed detection works; no GBA button is sent
    GBA_KEY_BTN_X(KeyEvent.KEYCODE_BUTTON_X),
    GBA_KEY_BTN_Y(KeyEvent.KEYCODE_BUTTON_Y),
    GBA_KEY_L2(KeyEvent.KEYCODE_BUTTON_L2),
    GBA_KEY_R2(KeyEvent.KEYCODE_BUTTON_R2),
    GBA_KEY_NONE(-1)
}

fun getKey(text: String): Int =
    (when (text) {
        "A" -> GBAKeys.GBA_KEY_A.key
        "B" -> GBAKeys.GBA_KEY_B.key
        "R" -> GBAKeys.GBA_KEY_R.key
        "L" -> GBAKeys.GBA_KEY_L.key
        "select" -> GBAKeys.GBA_KEY_SELECT.key
        "start" -> GBAKeys.GBA_KEY_START.key
        "up" -> GBAKeys.GBA_KEY_UP.key
        "down" -> GBAKeys.GBA_KEY_DOWN.key
        "left" -> GBAKeys.GBA_KEY_LEFT.key
        "right" -> GBAKeys.GBA_KEY_RIGHT.key
        "X" -> GBAKeys.GBA_KEY_BTN_X.key
        "Y" -> GBAKeys.GBA_KEY_BTN_Y.key
        "L2" -> GBAKeys.GBA_KEY_L2.key
        "R2" -> GBAKeys.GBA_KEY_R2.key
        else -> GBAKeys.GBA_KEY_NONE.key
    })

fun getKey(key: Int): Int =
    when (key) {
        KeyEvent.KEYCODE_BUTTON_A -> GBAKeys.GBA_KEY_A
        KeyEvent.KEYCODE_BUTTON_B -> GBAKeys.GBA_KEY_B
        KeyEvent.KEYCODE_BUTTON_L1 -> GBAKeys.GBA_KEY_L
        KeyEvent.KEYCODE_BUTTON_R1 -> GBAKeys.GBA_KEY_R
        KeyEvent.KEYCODE_BUTTON_SELECT -> GBAKeys.GBA_KEY_SELECT
        KeyEvent.KEYCODE_BUTTON_START -> GBAKeys.GBA_KEY_START
        KeyEvent.KEYCODE_DPAD_UP -> GBAKeys.GBA_KEY_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> GBAKeys.GBA_KEY_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> GBAKeys.GBA_KEY_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> GBAKeys.GBA_KEY_RIGHT
        KeyEvent.KEYCODE_BUTTON_X -> GBAKeys.GBA_KEY_BTN_X
        KeyEvent.KEYCODE_BUTTON_Y -> GBAKeys.GBA_KEY_BTN_Y
        KeyEvent.KEYCODE_BUTTON_L2 -> GBAKeys.GBA_KEY_L2
        KeyEvent.KEYCODE_BUTTON_R2 -> GBAKeys.GBA_KEY_R2
        else -> GBAKeys.GBA_KEY_NONE
    }.key

fun getKeyDisplayName(keyCode: Int): String = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_L1     -> "L1"
    KeyEvent.KEYCODE_BUTTON_R1     -> "R1"
    KeyEvent.KEYCODE_BUTTON_L2     -> "L2"
    KeyEvent.KEYCODE_BUTTON_R2     -> "R2"
    KeyEvent.KEYCODE_BUTTON_A      -> "A"
    KeyEvent.KEYCODE_BUTTON_B      -> "B"
    KeyEvent.KEYCODE_BUTTON_X      -> "X"
    KeyEvent.KEYCODE_BUTTON_Y      -> "Y"
    KeyEvent.KEYCODE_BUTTON_START  -> "Start"
    KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
    KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
    KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
    KeyEvent.KEYCODE_DPAD_UP       -> "D-Up"
    KeyEvent.KEYCODE_DPAD_DOWN     -> "D-Down"
    KeyEvent.KEYCODE_DPAD_LEFT     -> "D-Left"
    KeyEvent.KEYCODE_DPAD_RIGHT    -> "D-Right"
    -1                             -> "None"
    else                           -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ')
}
