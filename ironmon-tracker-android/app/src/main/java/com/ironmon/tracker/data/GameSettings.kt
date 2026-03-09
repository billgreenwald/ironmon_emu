package com.ironmon.tracker.data

import com.ironmon.tracker.data.models.GameVersion

/**
 * Game detection and ROM header constants.
 * Mirrors GameSettings.lua from the upstream Ironmon-Tracker project.
 *
 * PORTING NOTE: When syncing upstream, check GameSettings.lua for:
 *   - New game codes (CODE_* constants)
 *   - New game variants (e.g. regional/language variants)
 *   - Changes to ROM_GAME_CODE_ADDR
 * See TRACKER_VERSION.md for the pinned upstream commit.
 */
object GameSettings {

    // ── ROM Header ────────────────────────────────────────────────────────────

    /** GBA ROM header: 4 ASCII bytes identifying the game at this address */
    const val ROM_GAME_CODE_ADDR: Long = 0x080000ACL
    const val GAME_CODE_LENGTH: Int    = 4

    // ── Game codes (4 ASCII bytes from ROM header at ROM_GAME_CODE_ADDR) ──────

    // Fire Red variants (international, PAL, French, German, Italian)
    val FIRE_RED_CODES   = setOf("BPRE", "BPRS", "BPRI", "BPRF", "BPRD")
    val LEAF_GREEN_CODES = setOf("BPGE", "BPGS", "BPGI", "BPGF", "BPGD")
    val RUBY_CODES       = setOf("AXVE", "AXVS", "AXVI", "AXVF", "AXVD")
    val SAPPHIRE_CODES   = setOf("AXPE", "AXPS", "AXPI", "AXPF", "AXPD")
    val EMERALD_CODES    = setOf("BPEE", "BPES")

    /**
     * Detect the game from 4 bytes read at ROM_GAME_CODE_ADDR.
     * Bytes are ASCII — decode as ISO-8859-1 to avoid charset issues.
     */
    fun detectGame(codeBytes: ByteArray): GameVersion {
        if (codeBytes.size < 4) return GameVersion.UNKNOWN
        val code = String(codeBytes.take(4).toByteArray(), Charsets.ISO_8859_1)
        return when {
            code in FIRE_RED_CODES   -> GameVersion.FIRE_RED
            code in LEAF_GREEN_CODES -> GameVersion.LEAF_GREEN
            code in RUBY_CODES       -> GameVersion.RUBY
            code in SAPPHIRE_CODES   -> GameVersion.SAPPHIRE
            code in EMERALD_CODES    -> GameVersion.EMERALD
            else                     -> GameVersion.UNKNOWN
        }
    }
}
