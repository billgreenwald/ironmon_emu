package hh.game.mgba_android.utils

import android.content.Context

object EmulatorPreferences {
    private const val PREFS = "mGBA"
    private const val KEY_DEFAULT_FPS   = "pref_default_fps"
    private const val KEY_SECONDARY_FPS = "pref_secondary_fps"
    private const val KEY_SPEED_BUTTON  = "pref_speed_button" // "none"|"L"|"R"|"start"|"select"
    private const val KEY_SHOW_FPS           = "pref_show_fps"
    private const val KEY_MUTED              = "pref_mute"
    private const val KEY_SPLIT_FRACTION     = "pref_split_fraction"
    private const val KEY_ALWAYS_SHOW_CONTROLS = "pref_always_show_controls"
    private const val KEY_TRACKER_COLLAPSIBLE  = "pref_tracker_collapsible"

    val speedOptions = listOf(1 to 60f, 2 to 120f, 3 to 180f, 4 to 240f)
    // All mappable GBA inputs (matches getKey() string names)
    val buttonOptions = listOf("none", "A", "B", "L", "R", "start", "select", "up", "down", "left", "right")

    fun getDefaultFps(ctx: Context): Float = ctx.getSharedPreferences(PREFS, 0)
        .getFloat(KEY_DEFAULT_FPS, 60f)

    fun getSecondaryFps(ctx: Context): Float = ctx.getSharedPreferences(PREFS, 0)
        .getFloat(KEY_SECONDARY_FPS, 60f)

    fun getSpeedButton(ctx: Context): String = ctx.getSharedPreferences(PREFS, 0)
        .getString(KEY_SPEED_BUTTON, "none") ?: "none"

    fun getShowFps(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_SHOW_FPS, true)

    fun getMuted(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_MUTED, false)

    fun setMuted(ctx: Context, muted: Boolean) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_MUTED, muted).apply()
    }

    fun getSplitFraction(ctx: Context): Float = ctx.getSharedPreferences(PREFS, 0)
        .getFloat(KEY_SPLIT_FRACTION, 0.7f)

    fun setSplitFraction(ctx: Context, value: Float) {
        ctx.getSharedPreferences(PREFS, 0).edit().putFloat(KEY_SPLIT_FRACTION, value).apply()
    }

    fun getAlwaysShowControls(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_ALWAYS_SHOW_CONTROLS, false)

    fun getTrackerCollapsible(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_TRACKER_COLLAPSIBLE, false)

    fun getBinding(ctx: Context, action: BindableAction): Int {
        val prefs = ctx.getSharedPreferences(PREFS, 0)
        // One-time migration: convert old pref_speed_button string → raw Android keyCode
        if (action == BindableAction.SPEED_HOLD && !prefs.contains(action.prefKey)) {
            val old = prefs.getString(KEY_SPEED_BUTTON, "none") ?: "none"
            val migrated = getKey(old)
            prefs.edit().putInt(action.prefKey, migrated).apply()
            return migrated
        }
        return prefs.getInt(action.prefKey, -1)
    }

    fun setBinding(ctx: Context, action: BindableAction, keyCode: Int) {
        ctx.getSharedPreferences(PREFS, 0).edit().putInt(action.prefKey, keyCode).apply()
    }

    fun save(
        ctx: Context,
        defaultFps: Float,
        secondaryFps: Float,
        button: String,
        showFps: Boolean,
        splitFraction: Float = getSplitFraction(ctx),
        alwaysShowControls: Boolean = getAlwaysShowControls(ctx),
        trackerCollapsible: Boolean = getTrackerCollapsible(ctx),
    ) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putFloat(KEY_DEFAULT_FPS, defaultFps)
            .putFloat(KEY_SECONDARY_FPS, secondaryFps)
            .putString(KEY_SPEED_BUTTON, button)
            .putBoolean(KEY_SHOW_FPS, showFps)
            .putFloat(KEY_SPLIT_FRACTION, splitFraction)
            .putBoolean(KEY_ALWAYS_SHOW_CONTROLS, alwaysShowControls)
            .putBoolean(KEY_TRACKER_COLLAPSIBLE, trackerCollapsible)
            .apply()
    }
}
