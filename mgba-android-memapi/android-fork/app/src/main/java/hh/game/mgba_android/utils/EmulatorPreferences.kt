package hh.game.mgba_android.utils

import android.content.Context
import hh.game.mgba_android.tracker.data.GachaMonRuleset
import hh.game.mgba_android.tracker.data.GameOverCondition

object EmulatorPreferences {
    private const val PREFS = "mGBA"
    private const val KEY_DEFAULT_FPS   = "pref_default_fps"
    private const val KEY_SECONDARY_FPS = "pref_secondary_fps"
    private const val KEY_SPEED_BUTTON  = "pref_speed_button" // "none"|"L"|"R"|"start"|"select"
    private const val KEY_SHOW_FPS           = "pref_show_fps"
    private const val KEY_MUTED              = "pref_mute"
    private const val KEY_SPLIT_FRACTION     = "pref_split_fraction"
    private const val KEY_ALWAYS_SHOW_CONTROLS    = "pref_always_show_controls"
    private const val KEY_HIDE_ON_SCREEN_CONTROLS = "pref_hide_on_screen_controls"
    private const val KEY_TRACKER_COLLAPSIBLE  = "pref_tracker_collapsible"
    private const val KEY_HIDE_COLLAPSE_BUTTON = "pref_hide_collapse_button"
    private const val KEY_CONTROLS_ALPHA       = "pref_controls_alpha"
    private const val KEY_CONTROLS_SCALE       = "pref_controls_scale"
    private const val KEY_RATING_RULESET       = "pref_rating_ruleset"
    private const val KEY_L_AS_SPEED           = "pref_l_as_speed"
    private const val KEY_SPEED_TOGGLE_MODE    = "pref_speed_toggle_mode"
    private const val KEY_GAME_OVER_CONDITION  = "pref_game_over_condition"

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

    fun getHideOnScreenControls(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_HIDE_ON_SCREEN_CONTROLS, false)

    fun getTrackerCollapsible(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_TRACKER_COLLAPSIBLE, false)

    fun getHideCollapseButton(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_HIDE_COLLAPSE_BUTTON, false)

    fun getControlsAlpha(ctx: Context): Float = ctx.getSharedPreferences(PREFS, 0)
        .getFloat(KEY_CONTROLS_ALPHA, 0.7f)

    fun setControlsAlpha(ctx: Context, value: Float) {
        ctx.getSharedPreferences(PREFS, 0).edit().putFloat(KEY_CONTROLS_ALPHA, value).apply()
    }

    fun getControlsScale(ctx: Context): Float = ctx.getSharedPreferences(PREFS, 0)
        .getFloat(KEY_CONTROLS_SCALE, 1.0f)

    fun setControlsScale(ctx: Context, value: Float) {
        ctx.getSharedPreferences(PREFS, 0).edit().putFloat(KEY_CONTROLS_SCALE, value).apply()
    }

    fun getRuleset(ctx: Context): GachaMonRuleset {
        val name = ctx.getSharedPreferences(PREFS, 0)
            .getString(KEY_RATING_RULESET, GachaMonRuleset.STANDARD.name)
        return try { GachaMonRuleset.valueOf(name ?: GachaMonRuleset.STANDARD.name) }
               catch (_: IllegalArgumentException) { GachaMonRuleset.STANDARD }
    }

    fun setRuleset(ctx: Context, ruleset: GachaMonRuleset) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString(KEY_RATING_RULESET, ruleset.name).apply()
    }

    fun getGameOverCondition(ctx: Context): GameOverCondition {
        val name = ctx.getSharedPreferences(PREFS, 0)
            .getString(KEY_GAME_OVER_CONDITION, GameOverCondition.LEAD_FAINTS.name)
        return try { GameOverCondition.valueOf(name ?: GameOverCondition.LEAD_FAINTS.name) }
               catch (_: IllegalArgumentException) { GameOverCondition.LEAD_FAINTS }
    }

    fun setGameOverCondition(ctx: Context, cond: GameOverCondition) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString(KEY_GAME_OVER_CONDITION, cond.name).apply()
    }

    fun getLAsSpeed(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_L_AS_SPEED, false)

    fun getSpeedToggleMode(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, 0)
        .getBoolean(KEY_SPEED_TOGGLE_MODE, false)

    fun getGbaKeyBinding(ctx: Context, btn: GbaButton): Int {
        val stored = ctx.getSharedPreferences(PREFS, 0).getInt(btn.prefKey, -1)
        return if (stored == -1) btn.nativeKeyCode else stored
    }

    fun setGbaKeyBinding(ctx: Context, btn: GbaButton, keyCode: Int) {
        ctx.getSharedPreferences(PREFS, 0).edit().putInt(btn.prefKey, keyCode).apply()
    }

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
        hideCollapseButton: Boolean = getHideCollapseButton(ctx),
        hideOnScreenControls: Boolean = getHideOnScreenControls(ctx),
        lAsSpeed: Boolean = getLAsSpeed(ctx),
        speedToggleMode: Boolean = getSpeedToggleMode(ctx),
    ) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putFloat(KEY_DEFAULT_FPS, defaultFps)
            .putFloat(KEY_SECONDARY_FPS, secondaryFps)
            .putString(KEY_SPEED_BUTTON, button)
            .putBoolean(KEY_SHOW_FPS, showFps)
            .putFloat(KEY_SPLIT_FRACTION, splitFraction)
            .putBoolean(KEY_ALWAYS_SHOW_CONTROLS, alwaysShowControls)
            .putBoolean(KEY_TRACKER_COLLAPSIBLE, trackerCollapsible)
            .putBoolean(KEY_HIDE_COLLAPSE_BUTTON, hideCollapseButton)
            .putBoolean(KEY_HIDE_ON_SCREEN_CONTROLS, hideOnScreenControls)
            .putBoolean(KEY_L_AS_SPEED, lAsSpeed)
            .putBoolean(KEY_SPEED_TOGGLE_MODE, speedToggleMode)
            .apply()
    }
}
