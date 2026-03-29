package hh.game.mgba_android.tracker.quickload

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.os.SharedMemory
import android.system.ErrnoException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import java.io.File

object QuickloadManager {

    private const val PREFS_NAME = "mGBA"
    private const val KEY_FOLDER = "folder_path"
    private fun familyKey(prefix: String) = "family_last_$prefix"
    private fun familyModeKey(prefix: String) = "family_mode_$prefix"

    // Set by GameActivity.onCreate — cleared on GameActivity.onDestroy
    @Volatile var currentFamily: RomFamily? = null
    @Volatile var currentFamilyMode: FamilyMode = FamilyMode.BATCH
    @Volatile private var folderUri: Uri? = null

    // UPR-Android OverwriteService connection (bound at register(), released at unregister())
    private var serviceMessenger: Messenger? = null
    @Volatile private var connected = false
    @Volatile var uprAvailable = false   // package installed (for UI checks)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceMessenger = Messenger(service)
            connected = true
            Log.d("Quickload", "Connected to UPR OverwriteService")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceMessenger = null
            connected = false
            Log.d("Quickload", "Disconnected from UPR OverwriteService")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private val replyHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val sharedMemory = msg.obj as? SharedMemory
            val buffer = sharedMemory?.mapReadOnly()
            try {
                val data = ByteArray(msg.arg1)
                buffer?.get(data)
                replyChannel.trySend(data)
                Log.d("Quickload", "Received ROM data: ${data.size} bytes")
            } catch (e: ErrnoException) {
                Log.e("Quickload", "Unable to map shared memory", e)
            } finally {
                buffer?.let { SharedMemory.unmap(it) }
                sharedMemory?.close()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private val replyMessenger = Messenger(replyHandler)
    private val replyChannel = Channel<ByteArray>()

    /**
     * Call from GameActivity.onCreate after resolving gamepath.
     * Parses the ROM's family, persists the last-played number, and binds to UPR if installed.
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
        currentFamilyMode = getFamilyMode(context, family.prefix)

        // Bind to UPR-Android OverwriteService if installed and API >= 27
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val info = context.packageManager.getPackageInfo(
                    "ly.mens.rndpkmn", PackageManager.GET_SERVICES
                )
                uprAvailable = true
                if (info.services?.any { "OverwriteService" in it.name } == true) {
                    val intent = Intent().apply {
                        component = ComponentName("ly.mens.rndpkmn", "ly.mens.rndpkmn.OverwriteService")
                    }
                    context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                } else {
                    Log.d("Quickload", "UPR version does not support OverwriteService")
                }
            } catch (e: PackageManager.NameNotFoundException) {
                uprAvailable = false
                Log.d("Quickload", "UPR-Android not installed")
            } catch (e: SecurityException) {
                Log.e("Quickload", "Unable to bind to UPR service", e)
            }
        }
    }

    /** Call from GameActivity.onDestroy to avoid stale references. */
    fun unregister(context: Context) {
        if (connected) {
            try { context.unbindService(connection) } catch (_: Exception) {}
            connected = false
        }
        serviceMessenger = null
        currentFamily = null
        folderUri = null
        uprAvailable = false
    }

    /**
     * True when quickload/next-run is available.
     * BATCH: numbered family or UPR connected.
     * UPR: service must be connected.
     */
    fun canQuickload(): Boolean = when (currentFamilyMode) {
        FamilyMode.UPR   -> connected
        FamilyMode.BATCH -> currentFamily?.number != null || connected
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

        val cached = FamilyCache.load(context)
            .find { it.prefix == fam.prefix && it.extension == fam.extension }
        if (cached != null) {
            return cached.allMemberPaths.find { path ->
                RomFamilyUtils.parseFamily(path.substringAfterLast('/'), path).number == nextNumber
            }
        }

        val uri = folderUri ?: return null
        val nextFileName = "${fam.prefix}${nextNumber}.${fam.extension}"
        val folder = DocumentFile.fromTreeUri(context, uri) ?: return null
        val nextFile = folder.findFile(nextFileName) ?: return null
        return nextFile.getAbsolutePath(context)
    }

    /**
     * Advances to the next ROM.
     * UPR mode: always overwrites current ROM via OverwriteService.
     * BATCH mode: advances to next numbered ROM; falls back to OverwriteService if exhausted.
     */
    suspend fun advanceToNext(context: Context): String? {
        if (currentFamilyMode == FamilyMode.UPR) {
            return overwriteWithRandomizer(context)
        }
        val nextPath = getNextRomPath(context)
        if (nextPath != null) {
            val nextFileName = nextPath.substringAfterLast('/')
            val nextFamily = RomFamilyUtils.parseFamily(nextFileName, nextPath)
            if (nextFamily.number != null) {
                saveLastNumber(context, nextFamily.prefix, nextFamily.number)
            }
            return nextPath
        }
        return if (connected) overwriteWithRandomizer(context) else null
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

    /**
     * Sends the current ROM URI to UPR's OverwriteService, receives randomized ROM bytes
     * via SharedMemory, writes them back to the file, and returns the same path.
     */
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private suspend fun overwriteWithRandomizer(context: Context): String? {
        val currentPath = currentFamily?.absolutePath ?: return null
        if (!connected) return null
        val currentFileName = currentPath.substringAfterLast('/')
        val currentFile = folderUri?.let { uri ->
            DocumentFile.fromTreeUri(context, uri)?.findFile(currentFileName)
        } ?: DocumentFile.fromFile(File(currentPath))
        val message = Message.obtain().apply {
            obj = currentFile?.uri
            replyTo = replyMessenger
        }
        return try {
            serviceMessenger!!.send(message)
            withTimeout(10_000L) {
                val data = replyChannel.receive()
                currentFile?.openOutputStream(context, append = false)?.use { it.write(data) }
            }
            val nextNumber = getLastNumber(context, currentFamily!!.prefix) + 1
            setCurrentNumber(context, nextNumber)
            currentPath
        } catch (e: RemoteException) {
            Log.e("Quickload", "Failed to send message to UPR service", e)
            null
        } catch (e: CancellationException) {
            Log.e("Quickload", "UPR randomize timed out", e)
            null
        }
    }

    private fun saveLastNumber(context: Context, prefix: String, number: Int) {
        // commit() (synchronous) required — app restarts immediately after, apply() may not flush in time
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(familyKey(prefix), number).commit()
        if (!saved) Log.e("Quickload", "Failed to save last number for $prefix")
    }
}
