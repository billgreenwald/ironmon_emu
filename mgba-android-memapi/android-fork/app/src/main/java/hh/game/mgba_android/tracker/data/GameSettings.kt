package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.models.GameVersion

object GameSettings {

    const val ROM_GAME_CODE_ADDR: Long    = 0x080000ACL
    const val ROM_VERSION_BYTE_ADDR: Long = 0x080000BCL
    const val GAME_CODE_LENGTH: Int       = 4

    val FIRE_RED_CODES   = setOf("BPRE", "BPRS", "BPRI", "BPRF", "BPRD", "BPRJ")
    val LEAF_GREEN_CODES = setOf("BPGE", "BPGS", "BPGI", "BPGF", "BPGD")
    val RUBY_CODES       = setOf("AXVE", "AXVS", "AXVI", "AXVF", "AXVD")
    val SAPPHIRE_CODES   = setOf("AXPE", "AXPS", "AXPI", "AXPF", "AXPD")
    val EMERALD_CODES    = setOf("BPEE", "BPES")

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

    /**
     * Detects whether this is a non-English ROM variant (Japanese, Spanish, Italian, French, German).
     * Non-English FRLG ROMs use different ROM addresses for base stats.
     */
    fun isNonEnglish(codeBytes: ByteArray): Boolean {
        if (codeBytes.size < 4) return false
        val lastChar = codeBytes[3].toInt().and(0xFF).toChar()
        return lastChar != 'E'  // English = 'E', others = 'J','S','I','F','D'
    }

    /** Reads the ROM version byte (0=v1.0, 1=v1.1, 2=v1.2) from the ROM header. */
    fun readVersionByte(reader: (Long, Int) -> ByteArray?): Int {
        val b = reader(ROM_VERSION_BYTE_ADDR, 1) ?: return 0
        return b[0].toInt() and 0xFF
    }

    // NatDex ROM hack detection (CyanSMP64/NatDexExtension)
    // The NatDex ROM stores the total Pokémon count (1210) as a u32 at 0x08000170.
    const val NATDEX_MON_COUNT_ADDR: Long = 0x08000170L
    const val NATDEX_MON_COUNT: Int = 1210

    fun isNatDex(reader: (Long, Int) -> ByteArray?): Boolean {
        val b = reader(NATDEX_MON_COUNT_ADDR, 4) ?: return false
        val count = (b[0].toInt() and 0xFF) or
                    ((b[1].toInt() and 0xFF) shl 8) or
                    ((b[2].toInt() and 0xFF) shl 16) or
                    ((b[3].toInt() and 0xFF) shl 24)
        return count == NATDEX_MON_COUNT
    }
}
