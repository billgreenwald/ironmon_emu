package hh.game.mgba_android.tracker.platform

import android.content.Context
import android.util.Log
import hh.game.mgba_android.tracker.data.GachaMonRuleset
import hh.game.mgba_android.tracker.data.GameOverCondition
import hh.game.mgba_android.tracker.data.LogFileLocator
import hh.game.mgba_android.tracker.data.LogSource
import hh.game.mgba_android.utils.EmulatorPreferences
import java.io.File

/** Android implementations of the tracker-core platform seams. */

class AndroidLogger : Logger {
    override fun d(tag: String, msg: String) { Log.d(tag, msg) }
    override fun w(tag: String, msg: String) { Log.w(tag, msg) }
    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, msg, throwable) else Log.e(tag, msg)
    }
}

class PrefsKeyValueStore(context: Context, private val prefsName: String) : KeyValueStore {
    private val app = context.applicationContext
    override fun getString(key: String, default: String?): String? =
        app.getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(key, default)
    override fun putString(key: String, value: String) {
        app.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }
}

class FilesDirFileStore(context: Context) : FileStore {
    private val dir = context.applicationContext.filesDir
    override fun read(name: String): String? =
        File(dir, name).let { if (it.exists()) it.readText() else null }
    override fun write(name: String, content: String) { File(dir, name).writeText(content) }
    override fun delete(name: String) { File(dir, name).delete() }
    override fun exists(name: String): Boolean = File(dir, name).exists()
}

class AssetsAssetReader(context: Context) : AssetReader {
    private val assets = context.applicationContext.assets
    override fun readText(path: String): String? =
        try { assets.open(path).bufferedReader().use { it.readText() } } catch (e: Exception) { null }
}

/** Adapts the SAF-based [LogFileLocator] (which stays Android-side) to the common [LogSource]. */
class AndroidLogSource(context: Context) : LogSource {
    private val app = context.applicationContext
    override fun readLines(romFileName: String): List<String>? = LogFileLocator.readLines(app, romFileName)
    override fun exists(romFileName: String): Boolean = LogFileLocator.exists(app, romFileName)
}

class AndroidTrackerSettings(context: Context) : TrackerSettings {
    private val app = context.applicationContext
    override fun getRuleset(): GachaMonRuleset = EmulatorPreferences.getRuleset(app)
    override fun getGameOverCondition(): GameOverCondition = EmulatorPreferences.getGameOverCondition(app)
}

/**
 * Central factory: installs the logger and builds the [TrackerEnvironment] the poller needs.
 * Also exposes single seams for the few UI call sites that persist run/family data directly.
 */
object AndroidTrackerPlatform {

    private const val PROFILE_PREFS = "ironmon_profiles"

    /** Installs the Android logger and returns a fully-wired environment. */
    fun install(context: Context): TrackerEnvironment {
        val app = context.applicationContext
        TrackerLog.impl = AndroidLogger()
        return TrackerEnvironment(
            fileStore = FilesDirFileStore(app),
            assetReader = AssetsAssetReader(app),
            logSource = AndroidLogSource(app),
            settings = AndroidTrackerSettings(app),
        )
    }

    fun fileStore(context: Context): FileStore = FilesDirFileStore(context)
    fun keyValueStore(context: Context): KeyValueStore = PrefsKeyValueStore(context, PROFILE_PREFS)
    fun logSource(context: Context): LogSource = AndroidLogSource(context)
}
