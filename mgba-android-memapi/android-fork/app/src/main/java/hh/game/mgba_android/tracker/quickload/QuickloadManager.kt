package hh.game.mgba_android.tracker.quickload

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath

object QuickloadManager {

    private const val PREFS_NAME = "mGBA"
    private const val KEY_FOLDER = "folder_path"
    private fun familyKey(prefix: String) = "family_last_$prefix"

    // Set by GameActivity.onCreate — cleared on GameActivity.onDestroy
    @Volatile var currentFamily: RomFamily? = null
    @Volatile private var folderUri: Uri? = null

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
    }

    /** Call from GameActivity.onDestroy to avoid stale references. */
    fun unregister() {
        currentFamily = null
        folderUri = null
    }

    /** True only when the current ROM is part of a numbered family. */
    fun canQuickload(): Boolean = currentFamily?.number != null

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
    fun advanceToNext(context: Context): String? {
        val nextPath = getNextRomPath(context) ?: return null
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

    private fun saveLastNumber(context: Context, prefix: String, number: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(familyKey(prefix), number).apply()
    }
}
