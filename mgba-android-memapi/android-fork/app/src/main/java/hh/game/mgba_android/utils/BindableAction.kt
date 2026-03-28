package hh.game.mgba_android.utils

enum class BindableAction(val label: String, val prefKey: String) {
    SPEED_HOLD    ("Fast Forward (Hold)",  "pref_bind_speed_hold"),
    QUICK_SAVE    ("Save State",           "pref_bind_quick_save"),
    QUICK_LOAD    ("Load State",           "pref_bind_quick_load"),
    TRACKER_TOGGLE("Tracker Open/Close",   "pref_bind_tracker_toggle"),
    NEXT_RUN      ("Next Run →",           "pref_bind_next_run"),
    MUTE          ("Mute Toggle",          "pref_bind_mute"),
}
