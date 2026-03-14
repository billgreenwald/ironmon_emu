package hh.game.mgba_android.utils

import android.content.Context

object EmulatorPreferences {
    private const val PREFS = "mGBA"
    private const val KEY_DEFAULT_FPS   = "pref_default_fps"
    private const val KEY_SECONDARY_FPS = "pref_secondary_fps"
    private const val KEY_SPEED_BUTTON  = "pref_speed_button" // "none"|"L"|"R"|"start"|"select"
    private const val KEY_SHOW_FPS      = "pref_show_fps"

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

    fun save(ctx: Context, defaultFps: Float, secondaryFps: Float, button: String, showFps: Boolean) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putFloat(KEY_DEFAULT_FPS, defaultFps)
            .putFloat(KEY_SECONDARY_FPS, secondaryFps)
            .putString(KEY_SPEED_BUTTON, button)
            .putBoolean(KEY_SHOW_FPS, showFps)
            .apply()
    }
}
