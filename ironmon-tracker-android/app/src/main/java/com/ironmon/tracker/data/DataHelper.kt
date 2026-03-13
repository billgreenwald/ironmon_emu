package com.ironmon.tracker.data

import com.ironmon.tracker.data.models.GameVersion

/**
 * Per-game memory address tables.
 * Mirrors DataHelper.lua from the upstream Ironmon-Tracker project.
 *
 * PORTING NOTE: When syncing upstream, this is the PRIMARY file to update.
 * Address tables are kept as simple data objects (not maps or databases) so
 * they remain a 1:1 textual equivalent of the Lua tables and diffs are
 * immediately readable. See TRACKER_VERSION.md for the pinned upstream commit.
 *
 * Each address is a Long to hold unsigned 32-bit GBA addresses safely.
 */
data class GameAddresses(
    /** Address of gPlayerPartyCount (u8, value 0–6) */
    val partyCount: Long,
    /** Base address of party array. Each slot is 100 bytes. */
    val partyBase: Long,
    /** ROM address of gBaseStats table (28 bytes per species entry) */
    val baseStatsTable: Long,
    /** ROM address of level-up learnset table */
    val levelUpLearnsets: Long,
    /** ROM address of experience tables */
    val experienceTables: Long,
)

object DataHelper {

    // ── Party Pokémon struct layout ────────────────────────────────────────
    // Full struct is 100 bytes per slot.

    const val POKEMON_STRUCT_SIZE: Int = 100

    // Unencrypted header
    const val OFF_PERSONALITY: Int = 0x00  // u32
    const val OFF_OT_ID:       Int = 0x04  // u32

    // Encrypted substructure block (4 × 12-byte substructs, XOR'd with personality XOR otId)
    const val OFF_ENCRYPTED:   Int = 0x20  // 48 bytes total

    // Unencrypted in-battle stats (after decrypted block)
    const val OFF_LEVEL:       Int = 0x54  // u8
    const val OFF_CURRENT_HP:  Int = 0x56  // u16
    const val OFF_MAX_HP:      Int = 0x58  // u16
    const val OFF_ATTACK:      Int = 0x5A  // u16
    const val OFF_DEFENSE:     Int = 0x5C  // u16
    const val OFF_SPEED:       Int = 0x5E  // u16
    const val OFF_SP_ATK:      Int = 0x60  // u16
    const val OFF_SP_DEF:      Int = 0x62  // u16

    // Growth substructure offsets (within its 12-byte block)
    const val GROWTH_SPECIES:  Int = 0x00  // u16
    const val GROWTH_ITEM:     Int = 0x02  // u16
    const val GROWTH_EXP:      Int = 0x04  // u32

    // Attacks substructure offsets (within its 12-byte block)
    const val ATK_MOVE1: Int = 0x00  // u16
    const val ATK_MOVE2: Int = 0x02  // u16
    const val ATK_MOVE3: Int = 0x04  // u16
    const val ATK_MOVE4: Int = 0x06  // u16
    const val ATK_PP1:   Int = 0x08  // u8
    const val ATK_PP2:   Int = 0x09  // u8
    const val ATK_PP3:   Int = 0x0A  // u8
    const val ATK_PP4:   Int = 0x0B  // u8

    // Base stats entry layout (28 bytes per species):
    // HP(1), Atk(1), Def(1), Spe(1), SpA(1), SpD(1), Type1(1), Type2(1),
    // CatchRate(1), BaseExp(1), EVYield(2), Item1(2), Item2(2), Gender(1),
    // EggCycles(1), Friendship(1), LevelUpType(1), Egg1(1), Egg2(1),
    // Ability1(1), Ability2(1), SafariRate(1), Color(1), padding(4)
    const val BASE_STATS_ENTRY_SIZE: Int = 28
    const val BASE_STATS_HP:     Int = 0
    const val BASE_STATS_ATK:    Int = 1
    const val BASE_STATS_DEF:    Int = 2
    const val BASE_STATS_SPE:    Int = 3
    const val BASE_STATS_SPA:    Int = 4
    const val BASE_STATS_SPD:    Int = 5
    const val BASE_STATS_TYPE1:  Int = 6
    const val BASE_STATS_TYPE2:  Int = 7

    // ── Per-game address tables ────────────────────────────────────────────

    val FIRE_RED = GameAddresses(
        partyCount       = 0x02024029L,
        partyBase        = 0x02024284L,
        baseStatsTable   = 0x08254784L,
        levelUpLearnsets = 0x0825D794L,
        experienceTables = 0x08253AE4L,
    )

    val LEAF_GREEN = GameAddresses(
        partyCount       = 0x02024029L,
        partyBase        = 0x02024284L,
        baseStatsTable   = 0x08254760L,
        levelUpLearnsets = 0x0825D770L,
        experienceTables = 0x08253AC0L,
    )

    val RUBY = GameAddresses(
        partyCount       = 0x03004350L,
        partyBase        = 0x020244ECL,
        baseStatsTable   = 0x081FEC18L,
        levelUpLearnsets = 0x0823B16CL,
        experienceTables = 0x081E8CE4L,
    )

    val SAPPHIRE = GameAddresses(
        partyCount       = 0x03004350L,
        partyBase        = 0x020244ECL,
        baseStatsTable   = 0x081FEC18L,
        levelUpLearnsets = 0x0823B16CL,
        experienceTables = 0x081E8CECL,
    )

    val EMERALD = GameAddresses(
        partyCount       = 0x020244E9L,
        partyBase        = 0x020244ECL,
        baseStatsTable   = 0x083203CCL,
        levelUpLearnsets = 0x0832677CL,
        experienceTables = 0x082E82C4L,
    )

    fun addressesFor(game: GameVersion): GameAddresses? = when (game) {
        GameVersion.FIRE_RED   -> FIRE_RED
        GameVersion.LEAF_GREEN -> LEAF_GREEN
        GameVersion.RUBY       -> RUBY
        GameVersion.SAPPHIRE   -> SAPPHIRE
        GameVersion.EMERALD    -> EMERALD
        GameVersion.UNKNOWN    -> null
    }
}
