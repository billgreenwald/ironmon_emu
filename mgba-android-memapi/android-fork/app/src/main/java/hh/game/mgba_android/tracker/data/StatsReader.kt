package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.models.GameVersion

data class GameStats(
    val steps: Long,
    val totalBattles: Long,
    val pokemonCenterVisits: Long,
)

object StatsReader {
    private fun statsOffset(game: GameVersion): Int? = when (game) {
        GameVersion.FIRE_RED, GameVersion.LEAF_GREEN -> 0x1000
        GameVersion.EMERALD -> 0x159C
        GameVersion.RUBY, GameVersion.SAPPHIRE -> 0x1040
        else -> null
    }

    private fun saveBlock1PtrAddr(game: GameVersion): Long? = when (game) {
        GameVersion.FIRE_RED, GameVersion.LEAF_GREEN -> 0x03005008L
        GameVersion.EMERALD -> 0x03005D8CL
        GameVersion.RUBY, GameVersion.SAPPHIRE -> 0x03005D8CL
        else -> null
    }

    fun read(game: GameVersion): GameStats? {
        val ptrAddr = saveBlock1PtrAddr(game) ?: return null
        val ptrBytes = MemoryBridge.readBytes(ptrAddr, 4) ?: return null
        val saveBlock1Addr = ((ptrBytes[0].toLong() and 0xFF) or
            ((ptrBytes[1].toLong() and 0xFF) shl 8) or
            ((ptrBytes[2].toLong() and 0xFF) shl 16) or
            ((ptrBytes[3].toLong() and 0xFF) shl 24))
        if (saveBlock1Addr == 0L) return null
        val statsOff = statsOffset(game)?.toLong() ?: return null
        // steps at index 5 (offset +20), battles at index 7 (offset +28), center at index 15 (offset +60)
        fun readStat(idx: Int): Long {
            val bytes = MemoryBridge.readBytes(saveBlock1Addr + statsOff + idx * 4, 4) ?: return 0L
            return (bytes[0].toLong() and 0xFF) or
                ((bytes[1].toLong() and 0xFF) shl 8) or
                ((bytes[2].toLong() and 0xFF) shl 16) or
                ((bytes[3].toLong() and 0xFF) shl 24)
        }
        return GameStats(
            steps = readStat(5),
            totalBattles = readStat(7),
            pokemonCenterVisits = readStat(15),
        )
    }
}
