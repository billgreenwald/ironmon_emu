package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.tables.MoveNames

data class LearnsetInfo(
    val learnedCount: Int,       // moves with level <= current level
    val totalCount: Int,         // total moves in learnset
    val nextMoveLevel: Int,      // 0 if all moves already learned
    val nextMoveName: String,    // "" if none remaining
    val allMoveLevels: List<Int> = emptyList(), // levels of all learnset entries, in order
) {
    val allLearned: Boolean get() = nextMoveLevel == 0
    val isNextSoon: Boolean get() = !allLearned  // caller can compare nextMoveLevel to current+1
}

/**
 * Reads level-up learnset from ROM and computes learned/total/next stats.
 *
 * ROM layout (from Lua tracker PokemonData.readLevelUpMoves):
 *   gLevelUpLearnsets = array of 4-byte GBA pointers, one per species (index = speciesId)
 *   Each pointer → array of 2-byte entries until 0xFFFF sentinel:
 *     bits  0–8 : move ID
 *     bits 9–15 : level
 *
 * Display format matches Lua Utils.getMovesLearnedHeader: "Moves X/Y (nextLevel)"
 */
object LearnsetReader {

    private const val MAX_MOVES = 100   // failsafe, matches Lua tracker

    fun read(speciesId: Int, currentLevel: Int, addresses: GameAddresses): LearnsetInfo? {
        if (speciesId !in 1..1283) return null
        val layout = addresses.monLayout

        // Step 1: read the GBA pointer for this species (stride is per-game; vanilla 4 bytes)
        val ptrBytes = MemoryBridge.readBytes(
            addresses.levelUpLearnsets + speciesId.toLong() * layout.learnsetPtrStride.toLong(), 4
        ) ?: return null

        val learnsetPtr =
            (ptrBytes[0].toLong() and 0xFF) or
            ((ptrBytes[1].toLong() and 0xFF) shl 8) or
            ((ptrBytes[2].toLong() and 0xFF) shl 16) or
            ((ptrBytes[3].toLong() and 0xFF) shl 24)

        if (learnsetPtr < 0x08000000L || learnsetPtr > 0x0FFFFFFFL) return null

        // Step 2: iterate entries until the sentinel. Each entry is layout.learnsetEntrySize bytes;
        // moveId/level are bit fields within it (vanilla: 2-byte word, move=bits0-8, level=bits9-15).
        var learnedCount = 0
        var totalCount   = 0
        var nextMoveLevel = 0
        var nextMoveName  = ""
        var foundNext     = false
        val allMoveLevels = mutableListOf<Int>()

        val entrySize  = layout.learnsetEntrySize
        val moveMask   = (1L shl layout.learnsetMoveIdBits) - 1L
        val levelMask  = (1L shl layout.learnsetLevelBits) - 1L

        for (i in 0 until MAX_MOVES) {
            val bytes = MemoryBridge.readBytes(learnsetPtr + i.toLong() * entrySize.toLong(), entrySize) ?: break
            var entry = 0L
            for (b in 0 until entrySize) entry = entry or ((bytes[b].toLong() and 0xFF) shl (8 * b))
            if (entry == layout.learnsetEndFlag) break

            val moveId = ((entry ushr layout.learnsetMoveIdBitOff) and moveMask).toInt()
            val level  = ((entry ushr layout.learnsetLevelBitOff) and levelMask).toInt()
            totalCount++
            allMoveLevels.add(level)

            if (level <= currentLevel) {
                learnedCount++
            } else if (!foundNext) {
                nextMoveLevel = level
                nextMoveName  = MoveNames.get(moveId)
                foundNext = true
            }
        }

        return LearnsetInfo(learnedCount, totalCount, nextMoveLevel, nextMoveName, allMoveLevels)
    }
}
