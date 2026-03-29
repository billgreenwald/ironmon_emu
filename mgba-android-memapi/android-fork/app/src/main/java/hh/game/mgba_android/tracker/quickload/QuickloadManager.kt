package hh.game.mgba_android.tracker.quickload

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object QuickloadManager {

    private const val PREFS_NAME = "mGBA"
    private const val KEY_FOLDER = "folder_path"
    private fun familyKey(prefix: String) = "family_last_$prefix"
    private fun familyModeKey(prefix: String) = "family_mode_$prefix"

    private const val UPR_PACKAGE = "ly.mens.rndpkmn"
    private const val UPR_SERVICE = "$UPR_PACKAGE.RandomizerService"
    private const val MSG_RANDOMIZE = 1
    private const val MSG_RANDOMIZE_DONE = 2

    // Set by GameActivity.onCreate — cleared on GameActivity.onDestroy
    @Volatile var currentFamily: RomFamily? = null
    @Volatile var currentFamilyMode: FamilyMode = FamilyMode.BATCH
    @Volatile private var folderUri: Uri? = null
    @Volatile private var uprAvailable: Boolean = false

    /**
     * Call from GameActivity.onCreate after resolving gamepath.
     * Parses the ROM's family and persists the last-played number.
     */
    fun register(context: Context, absolutePath: String) {
        val fileName = absolutePath.substringAfterLast('/')
        val family = RomFamilyUtils.parseFamily(fileName, absolutePath)
        currentFamily = family
        folderUri = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER, null)
            ?.let { Uri.parse(it) }
        if (family.number != null) {
            saveLastNumber(context, family.prefix, family.number)
        }
        uprAvailable = try {
            context.packageManager.getPackageInfo(UPR_PACKAGE, 0)
            true
        } catch (_: Exception) { false }
        currentFamilyMode = getFamilyMode(context, family.prefix)
    }

    /** Call from GameActivity.onDestroy to avoid stale references. */
    fun unregister(context: Context) {
        currentFamily = null
        folderUri = null
        uprAvailable = false
    }

    /**
     * True when quickload/next-run is available.
     * UPR: requires UPR-Android to be installed.
     * BATCH: requires a numbered family or UPR fallback.
     */
    fun canQuickload(): Boolean = when (currentFamilyMode) {
        FamilyMode.UPR -> uprAvailable
        FamilyMode.BATCH   -> currentFamily?.number != null || uprAvailable
    }

    fun getFamilyMode(context: Context, prefix: String): FamilyMode {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(familyModeKey(prefix), FamilyMode.BATCH.name)
        return try { FamilyMode.valueOf(name ?: FamilyMode.BATCH.name) } catch (_: Exception) { FamilyMode.BATCH }
    }

    fun setFamilyMode(context: Context, prefix: String, mode: FamilyMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(familyModeKey(prefix), mode.name).apply()
        if (currentFamily?.prefix == prefix) currentFamilyMode = mode
    }

    /**
     * Returns the absolute path of the next ROM (number+1) in the family.
     * Checks the on-disk family cache first (fast), then falls back to SAF scan.
     */
    fun getNextRomPath(context: Context): String? {
        val fam = currentFamily ?: return null
        val nextNumber = fam.number?.plus(1) ?: return null

        // Fast path: look up next path from cached family member list
        val cached = FamilyCache.load(context)
            .find { it.prefix == fam.prefix && it.extension == fam.extension }
        if (cached != null) {
            return cached.allMemberPaths.find { path ->
                RomFamilyUtils.parseFamily(path.substringAfterLast('/'), path).number == nextNumber
            }
        }

        // Slow fallback: SAF scan (used when no cache exists)
        val uri = folderUri ?: return null
        val nextFileName = "${fam.prefix}${nextNumber}.${fam.extension}"
        val folder = DocumentFile.fromTreeUri(context, uri) ?: return null
        val nextFile = folder.findFile(nextFileName) ?: return null
        return nextFile.getAbsolutePath(context)
    }

    /**
     * Advances to the next ROM. In UPR mode always randomizes in place.
     * In BATCH mode tries the next numbered ROM, then falls back to the randomizer.
     * Returns null if no next ROM is available. Caller loads it via JNI.
     */
    suspend fun advanceToNext(context: Context): String? {
        if (currentFamilyMode == FamilyMode.UPR) {
            return randomizeCurrentRom(context)
        }
        // BATCH: try next numbered ROM first
        val nextPath = getNextRomPath(context)
        if (nextPath != null) {
            val nextFileName = nextPath.substringAfterLast('/')
            val nextFamily = RomFamilyUtils.parseFamily(nextFileName, nextPath)
            if (nextFamily.number != null) {
                saveLastNumber(context, nextFamily.prefix, nextFamily.number)
            }
            return nextPath
        }
        // Batch exhausted — fall back to randomizer if available
        return if (uprAvailable) randomizeCurrentRom(context) else null
    }

    /** Last-played ROM number for a given prefix (defaults to 1 if never played). */
    fun getLastNumber(context: Context, prefix: String): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(familyKey(prefix), 1)

    /** Manually override the current ROM number (e.g. from settings screen). */
    fun setCurrentNumber(context: Context, number: Int) {
        currentFamily = currentFamily?.copy(number = number)
        currentFamily?.let { saveLastNumber(context, it.prefix, number) }
    }

    private fun saveLastNumber(context: Context, prefix: String, number: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(familyKey(prefix), number).apply()
    }

    /**
     * Asks UPR-Android to randomize the current ROM file in-place via its Messenger service.
     * Returns the same ROM path on success (now randomized), or null on failure.
     * Note: The message protocol (MSG_RANDOMIZE/MSG_RANDOMIZE_DONE) must match
     * UPR-Android's RandomizerService API.
     */
    private suspend fun randomizeCurrentRom(context: Context): String? {
        val romPath = currentFamily?.absolutePath ?: return null
        return try {
            val done = CompletableDeferred<Boolean>()
            val replyHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (!done.isCompleted) done.complete(msg.what == MSG_RANDOMIZE_DONE)
                }
            }
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    try {
                        val msg = Message.obtain(null, MSG_RANDOMIZE).apply {
                            replyTo = Messenger(replyHandler)
                            data = Bundle().apply { putString("path", romPath) }
                        }
                        Messenger(binder).send(msg)
                    } catch (e: Exception) {
                        Log.w("QuickloadManager", "UPR send failed", e)
                        if (!done.isCompleted) done.complete(false)
                    }
                }
                override fun onServiceDisconnected(name: ComponentName) {
                    if (!done.isCompleted) done.complete(false)
                }
            }
            val intent = Intent().apply { setClassName(UPR_PACKAGE, UPR_SERVICE) }
            val bound = withContext(Dispatchers.Main) {
                context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            }
            if (!bound) {
                Log.w("QuickloadManager", "Could not bind to UPR service")
                return null
            }
            val ok = withTimeoutOrNull(30_000L) { done.await() } ?: false
            withContext(Dispatchers.Main) {
                try { context.unbindService(conn) } catch (_: Exception) {}
            }
            if (ok) romPath else null
        } catch (e: Exception) {
            Log.w("QuickloadManager", "UPR randomize failed", e)
            null
        }
    }
}
