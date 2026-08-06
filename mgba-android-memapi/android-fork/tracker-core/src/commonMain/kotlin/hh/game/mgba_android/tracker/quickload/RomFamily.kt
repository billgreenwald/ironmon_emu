package hh.game.mgba_android.tracker.quickload

import kotlinx.serialization.Serializable

enum class FamilyMode { BATCH, UPR }

/**
 * Parsed representation of a ROM filename.
 *
 * Examples:
 *   "firered42.gba"      → prefix="firered",   number=42, extension="gba"
 *   "randomized_007.gba" → prefix="randomized_", number=7, extension="gba"
 *   "pokemonemerald.gba" → prefix="pokemonemerald", number=null, extension="gba"
 */
data class RomFamily(
    val prefix: String,
    val number: Int?,         // null when filename has no trailing digits
    val extension: String,    // "gba" or "gb", always lowercased
    val absolutePath: String,
    val fileName: String,
)

/**
 * A group of sequentially numbered ROMs sharing the same prefix.
 * Uses plain paths so it can be serialized to the on-disk cache.
 */
@Serializable
data class RomFamilyGroup(
    val prefix: String,
    val extension: String,
    val totalCount: Int,
    val lastPlayedNumber: Int,
    val allMemberPaths: List<String>,   // absolute paths, sorted ascending by number
)

/** A discovered ROM file (name + full path), fed into [RomFamilyUtils.buildFamilies]. */
data class RomFileEntry(val name: String, val path: String)

object RomFamilyUtils {

    // Matches: <prefix><digits>.<gba|gb>  — prefix is non-greedy so digits are captured last
    private val REGEX = Regex("""^(.*?)(\d+)\.(gba|gb)$""", RegexOption.IGNORE_CASE)

    /**
     * Groups discovered ROM files into families by (prefix, extension), sorted by trailing number.
     * Shared by Android (SAF scan) and iOS (folder enumeration). `lastPlayedNumber` is left 0 for
     * the caller to fill from its own per-prefix store.
     */
    fun buildFamilies(files: List<RomFileEntry>): List<RomFamilyGroup> =
        files.map { parseFamily(it.name, it.path) }
            .groupBy { it.prefix to it.extension }
            .map { (key, members) ->
                val sorted = members.sortedBy { it.number ?: 0 }
                RomFamilyGroup(
                    prefix = key.first,
                    extension = key.second,
                    totalCount = sorted.size,
                    lastPlayedNumber = 0,
                    allMemberPaths = sorted.map { it.absolutePath },
                )
            }
            .sortedBy { it.prefix }

    fun parseFamily(fileName: String, absolutePath: String): RomFamily {
        val m = REGEX.matchEntire(fileName)
        return if (m != null) {
            RomFamily(
                prefix       = m.groupValues[1],
                number       = m.groupValues[2].toIntOrNull(),
                extension    = m.groupValues[3].lowercase(),
                absolutePath = absolutePath,
                fileName     = fileName,
            )
        } else {
            val ext = fileName.substringAfterLast('.', "gba").lowercase()
            RomFamily(
                prefix       = fileName,
                number       = null,
                extension    = ext,
                absolutePath = absolutePath,
                fileName     = fileName,
            )
        }
    }
}
