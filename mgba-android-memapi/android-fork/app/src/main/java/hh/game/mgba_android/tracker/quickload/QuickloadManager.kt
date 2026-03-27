package hh.game.mgba_android.tracker.quickload

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.system.ErrnoException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import java.io.File

object QuickloadManager {

    private const val PREFS_NAME = "mGBA"
    private const val KEY_FOLDER = "folder_path"
    private fun familyKey(prefix: String) = "family_last_$prefix"

    // Set by GameActivity.onCreate — cleared on GameActivity.onDestroy
    @Volatile var currentFamily: RomFamily? = null
    @Volatile private var folderUri: Uri? = null

    // Randomizer service connection
    private var serviceMessenger: Messenger? = null
    private var connected = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceMessenger = Messenger(service)
            connected = true
            Log.d("Quickload", "Connected to Randomizer service")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceMessenger = null
            connected = false
            Log.d("Quickload", "Disconnected from Randomizer service")
        }

    }
    private val replyHandler = object : Handler(Looper.getMainLooper()) {
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun handleMessage(msg: Message) {
            val sharedMemory = msg.obj as? SharedMemory
            val buffer = sharedMemory?.mapReadOnly()
            try {
                val data = ByteArray(msg.arg1)
                buffer?.get(data)
                replyChannel.trySend(data)
                Log.d("Quickload", "Received data size ${data.size}")
            } catch (e: ErrnoException) {
                Log.e("Quickload", "Unable to map shared memory", e)
            } finally {
                buffer?.let { SharedMemory.unmap(it) }
                sharedMemory?.close()
            }
        }
    }
    private val replyMessenger = Messenger(replyHandler)
    private val replyChannel = Channel<ByteArray>()

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

        // check to see if Randomizer app is installed
        try {
            val info = context.packageManager.getPackageInfo("ly.mens.rndpkmn", PackageManager.GET_SERVICES)
            if (info.services.any { "OverwriteService" in it.name }) {
                val intent = Intent().apply {
                    component = ComponentName("ly.mens.rndpkmn", "ly.mens.rndpkmn.OverwriteService")
                }
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            } else Log.d("Quickload", "Randomizer version not supported!")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Quickload", "UPR-Android app is not installed on the device!", e)
        } catch (e: SecurityException) {
            Log.e("Quickload", "Unable to bind to service!", e)
        }
    }

    /** Call from GameActivity.onDestroy to avoid stale references. */
    fun unregister(context: Context) {
        currentFamily = null
        folderUri = null
        if (connected) {
            context.unbindService(connection)
            connected = false
        }
    }

    /** True only when the current ROM is part of a numbered family. */
    fun canQuickload(): Boolean = currentFamily?.number != null || connected

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
     * Advances to the next ROM: saves prefs and returns the next ROM's path.
     * Returns null if no next ROM exists. The caller is responsible for loading it via JNI.
     */
    suspend fun advanceToNext(context: Context): String? {
        val nextPath = getNextRomPath(context)
        // does not have next ROM - attempt to overwrite with Randomizer service
        if (nextPath == null) {
            val currentPath = currentFamily?.absolutePath
            if (connected && currentPath != null) {
                val currentFile = DocumentFile.fromFile(File(currentPath))
                val message = Message.obtain().apply {
                    obj = currentFile?.uri
                    replyTo = replyMessenger
                }
                return try {
                    serviceMessenger!!.send(message)
                    withTimeout(10_000L) {
                        val data = replyChannel.receive()
                        currentFile?.openOutputStream(context, append = false)?.use {
                            it.write(data)
                        }
                    }
                    val nextNumber = getLastNumber(context, currentFamily!!.prefix) + 1
                    setCurrentNumber(context, nextNumber)
                    currentPath
                } catch (e: RemoteException) {
                    Log.e("Quickload", "Failed to send message!", e)
                    null
                } catch (e: CancellationException) {
                    Log.e("Quickload", "Request timed out!", e)
                    null
                }
            }
            return null
        }
        val nextFileName = nextPath.substringAfterLast('/')
        val nextFamily = RomFamilyUtils.parseFamily(nextFileName, nextPath)
        if (nextFamily.number != null) {
            saveLastNumber(context, nextFamily.prefix, nextFamily.number)
        }
        return nextPath
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
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(familyKey(prefix), number)
                .commit() // must use commit so that the update is saved before the app restarts
        if (!saved) {
            Log.e("Quickload", "Failed to update last number!")
        }
    }
}
