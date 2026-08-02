package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.data.GameOverCondition
import hh.game.mgba_android.tracker.data.GachaMonRuleset
import hh.game.mgba_android.tracker.data.LogSource

/**
 * User-configurable tracker settings, read live each poll. Backed by SharedPreferences on
 * Android (EmulatorPreferences) and NSUserDefaults on iOS.
 */
interface TrackerSettings {
    fun getRuleset(): GachaMonRuleset
    fun getGameOverCondition(): GameOverCondition
}

/**
 * The platform capabilities TrackerPoller depends on, bundled so the poller stays common and
 * free of any Context/Android reference. Each platform constructs one at startup and passes it
 * to [hh.game.mgba_android.tracker.TrackerPoller.start].
 */
class TrackerEnvironment(
    val fileStore: FileStore,
    val assetReader: AssetReader,
    val logSource: LogSource,
    val settings: TrackerSettings,
)
