package hh.game.mgba_android.utils

enum class GbaButton(val label: String, val prefKey: String, val nativeKeyCode: Int) {
    A      ("GBA A",       "pref_gba_key_a",      GBAKeys.GBA_KEY_A.key),
    B      ("GBA B",       "pref_gba_key_b",      GBAKeys.GBA_KEY_B.key),
    L      ("GBA L",       "pref_gba_key_l",      GBAKeys.GBA_KEY_L.key),
    R      ("GBA R",       "pref_gba_key_r",      GBAKeys.GBA_KEY_R.key),
    START  ("Start",       "pref_gba_key_start",  GBAKeys.GBA_KEY_START.key),
    SELECT ("Select",      "pref_gba_key_select", GBAKeys.GBA_KEY_SELECT.key),
    UP     ("D-Pad Up",    "pref_gba_key_up",     GBAKeys.GBA_KEY_UP.key),
    DOWN   ("D-Pad Down",  "pref_gba_key_down",   GBAKeys.GBA_KEY_DOWN.key),
    LEFT   ("D-Pad Left",  "pref_gba_key_left",   GBAKeys.GBA_KEY_LEFT.key),
    RIGHT  ("D-Pad Right", "pref_gba_key_right",  GBAKeys.GBA_KEY_RIGHT.key),
}
