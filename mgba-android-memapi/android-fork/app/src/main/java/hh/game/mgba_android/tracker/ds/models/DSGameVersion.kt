package hh.game.mgba_android.tracker.ds.models

enum class DSGameVersion(val gameCode: String, val displayName: String) {
    DIAMOND("ADAE", "Pokémon Diamond"),
    PEARL("APAE", "Pokémon Pearl"),    // Note: US Pearl = APAE, Diamond = ADAE
    PLATINUM("CPUE", "Pokémon Platinum"),
    PLATINUM_JP("CPUJ", "Pokémon Platinum (JP)"),
    UNKNOWN("", "Unknown");

    companion object {
        // DS ROM header: game code is 4 ASCII bytes at offset 0x0C within the cartridge header.
        // In DS main RAM, the cartridge header is mirrored at 0x027FFE00.
        // So game code is at 0x027FFE00 + 0x0C = 0x027FFE0C.
        const val HEADER_MIRROR_BASE = 0x027FFE00
        const val GAME_CODE_OFFSET = 0x0C  // offset within header
        const val GAME_CODE_ADDRESS = HEADER_MIRROR_BASE + GAME_CODE_OFFSET  // 0x027FFE0C

        fun fromGameCode(code: String): DSGameVersion =
            entries.firstOrNull { it.gameCode == code } ?: UNKNOWN
    }
}
