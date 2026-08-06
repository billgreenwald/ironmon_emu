package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.data.GachaMonRuleset
import hh.game.mgba_android.tracker.data.GameOverCondition

/**
 * Swift access to the settings enums (their case lists + display labels). Swift stores the chosen
 * enum `name` in NSUserDefaults under the same keys IosTrackerSettings reads.
 */
object SettingsSwift {
    fun rulesetOptions(): List<String> = GachaMonRuleset.entries.map { it.name }
    fun rulesetLabel(name: String): String =
        runCatching { GachaMonRuleset.valueOf(name).label }.getOrDefault(name)

    fun gameOverOptions(): List<String> = GameOverCondition.entries.map { it.name }
    fun gameOverLabel(name: String): String =
        runCatching { GameOverCondition.valueOf(name).label }.getOrDefault(name)
}
