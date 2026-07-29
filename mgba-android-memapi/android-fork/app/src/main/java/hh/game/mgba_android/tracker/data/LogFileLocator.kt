package hh.game.mgba_android.tracker.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Locates and reads the Universal Pokémon Randomizer `.log` spoiler file that
 * sits next to the loaded ROM in the user's SAF folder.
 *
 * The randomizer names the log by appending `.log` to the full ROM filename
 * (e.g. `Seed12.gba` → `Seed12.gba.log`); some setups drop the ROM extension
 * (`Seed12.log`). Both are tried. The ROM folder is the SAF tree URI cached by
 * the game-list flow in SharedPreferences `"mGBA"` → `"folder_path"` (same key
 * QuickloadManager uses).
 */
object LogFileLocator {

    private const val PREFS_NAME = "mGBA"
    private const val KEY_FOLDER = "folder_path"
    private const val TAG = "LogFileLocator"

    // Cache the last resolved log document to avoid re-enumerating the SAF folder.
    @Volatile private var cachedRomName: String? = null
    @Volatile private var cachedDoc: DocumentFile? = null

    private fun folderUri(context: Context): Uri? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER, null) ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    /** Candidate log display names for a given ROM filename. */
    private fun candidateNames(romFileName: String): List<String> {
        val base = romFileName.substringBeforeLast('.', romFileName)
        return listOf("$romFileName.log", "$base.log")
    }

    /** Finds the `.log` DocumentFile for [romFileName], or null if none exists. */
    fun findLogFile(context: Context, romFileName: String): DocumentFile? {
        if (romFileName.isBlank()) return null
        cachedDoc?.let { if (cachedRomName == romFileName && it.exists()) return it }

        val uri = folderUri(context) ?: return null
        val folder = runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull() ?: return null
        for (name in candidateNames(romFileName)) {
            val doc = folder.findFile(name)
            if (doc != null && doc.isFile) {
                cachedRomName = romFileName
                cachedDoc = doc
                return doc
            }
        }
        return null
    }

    /** Cheap existence probe used to gate the "Review Logs" banner. */
    fun exists(context: Context, romFileName: String): Boolean =
        findLogFile(context, romFileName) != null

    /** Reads the log's lines (UTF-8, split on CR/LF), or null if not found/unreadable. */
    fun readLines(context: Context, romFileName: String): List<String>? {
        val doc = findLogFile(context, romFileName) ?: return null
        return try {
            context.contentResolver.openInputStream(doc.uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText().split(Regex("\r\n|\r|\n"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log for $romFileName", e)
            null
        }
    }
}
