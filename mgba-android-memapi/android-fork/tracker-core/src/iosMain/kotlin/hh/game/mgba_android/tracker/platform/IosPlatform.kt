package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.data.GachaMonRuleset
import hh.game.mgba_android.tracker.data.GameOverCondition
import hh.game.mgba_android.tracker.data.LogSource
import hh.game.mgba_android.tracker.models.TrackerState
import hh.game.mgba_android.tracker.TrackerPoller
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile

/**
 * iOS (Kotlin/Native) implementations of the tracker-core platform seams. Mirrors
 * `AndroidPlatform.kt` on the Android side. Wire these up from Swift via [IosTracker].
 */

// ── Logger ───────────────────────────────────────────────────────────────────────────────────
// println() goes to the Xcode console / device logs. Can be upgraded to os_log later if wanted.
class IosLogger : Logger {
    override fun d(tag: String, msg: String) { println("D/$tag: $msg") }
    override fun w(tag: String, msg: String) { println("W/$tag: $msg") }
    override fun e(tag: String, msg: String, throwable: Throwable?) {
        println("E/$tag: $msg${throwable?.let { " :: $it" } ?: ""}")
    }
}

// ── Key/value store (NSUserDefaults) ──────────────────────────────────────────────────────────
class NsUserDefaultsKeyValueStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : KeyValueStore {
    override fun getString(key: String, default: String?): String? =
        defaults.stringForKey(key) ?: default
    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}

// ── File store (app Documents dir) ────────────────────────────────────────────────────────────
@OptIn(ExperimentalForeignApi::class)
class DocumentsFileStore : FileStore {
    private val documentsDir: String by lazy {
        (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String) ?: NSFileManager.defaultManager.currentDirectoryPath
    }

    private fun path(name: String): String =
        (documentsDir as NSString).stringByAppendingPathComponent(name)

    override fun read(name: String): String? {
        val data = NSFileManager.defaultManager.contentsAtPath(path(name)) ?: return null
        return NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
    }

    override fun write(name: String, content: String) {
        val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        (data as NSData).writeToFile(path(name), atomically = true)
    }

    override fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath(path(name), null)
    }

    override fun exists(name: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path(name))
}

// ── Asset reader (app bundle resources) ───────────────────────────────────────────────────────
// The maxData JSON + Lua files (app/src/main/assets/maxdata/* on Android) must be added to the iOS
// app target — ideally as a BLUE FOLDER REFERENCE named "maxdata" so the "maxdata/" path prefix is
// preserved. This reader tries the sub-directory form first, then a flattened fallback.
class BundleAssetReader(private val bundle: NSBundle = NSBundle.mainBundle) : AssetReader {
    override fun readText(path: String): String? {
        val fileName = path.substringAfterLast('/')
        val subdir = if ('/' in path) path.substringBeforeLast('/') else null
        val ext = fileName.substringAfterLast('.', "")
        val base = fileName.substringBeforeLast('.')
        val resolved = (subdir?.let { bundle.pathForResource(base, ext, it) }
            ?: bundle.pathForResource(base, ext))
            ?: return null
        val data = NSFileManager.defaultManager.contentsAtPath(resolved) ?: return null
        return NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
    }
}

// ── Log source (randomizer .log) ──────────────────────────────────────────────────────────────
// Deferred for the iOS MVP (Android uses SAF; iOS will pick a file). No-op for now so the log
// viewer simply reports "not found" instead of crashing.
object NoopLogSource : LogSource {
    override fun readLines(romFileName: String): List<String>? = null
    override fun exists(romFileName: String): Boolean = false
}

// ── Tracker settings (NSUserDefaults-backed) ──────────────────────────────────────────────────
class IosTrackerSettings(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : TrackerSettings {
    override fun getRuleset(): GachaMonRuleset =
        defaults.stringForKey(KEY_RULESET)
            ?.let { runCatching { GachaMonRuleset.valueOf(it) }.getOrNull() }
            ?: GachaMonRuleset.STANDARD

    override fun getGameOverCondition(): GameOverCondition =
        defaults.stringForKey(KEY_GAME_OVER)
            ?.let { runCatching { GameOverCondition.valueOf(it) }.getOrNull() }
            ?: GameOverCondition.LEAD_FAINTS

    private companion object {
        const val KEY_RULESET = "rating_ruleset"
        const val KEY_GAME_OVER = "game_over_condition"
    }
}

/**
 * Swift-facing facade. Usage from Swift:
 *
 *   let env = IosTracker.shared.install()
 *   IosTracker.shared.setMemoryReader { addr, len in provider.read(addr, len) }  // returns KotlinByteArray?
 *   IosTracker.shared.start(environment: env, romPath: path)
 *   IosTracker.shared.observeState { state in viewModel.update(state) }
 */
object IosTracker {
    // UI-thread scope: state is collected on Main so SwiftUI updates happen on the main thread.
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Installs the iOS logger and returns a fully-wired environment. */
    fun install(): TrackerEnvironment {
        TrackerLog.impl = IosLogger()
        val defaults = NSUserDefaults.standardUserDefaults
        return TrackerEnvironment(
            fileStore = DocumentsFileStore(),
            assetReader = BundleAssetReader(),
            logSource = NoopLogSource,
            settings = IosTrackerSettings(defaults),
        )
    }

    /** Set the native memory reader (Swift wraps the mGBA core's rawRead8/busRead8). */
    fun setMemoryReader(reader: ((Int, Int) -> ByteArray?)?) {
        MemoryBridge.reader = reader
    }

    fun start(environment: TrackerEnvironment, romPath: String?) {
        TrackerPoller.start(environment, scope, romPath)
    }

    /** Collect the tracker state on the main thread. Returns a Job the caller can cancel. */
    fun observeState(onState: (TrackerState) -> Unit): Job =
        scope.launch { TrackerPoller.state.collect { onState(it) } }

    fun stop() {
        TrackerPoller.stop()
    }

    // Passthroughs for UI actions the SwiftUI layer may trigger.
    fun rerollBall() = TrackerPoller.rerollBall()
    fun manualNextRun() = TrackerPoller.manualNextRun()
    fun romFileName(): String = TrackerPoller.getRomFileName()
}
