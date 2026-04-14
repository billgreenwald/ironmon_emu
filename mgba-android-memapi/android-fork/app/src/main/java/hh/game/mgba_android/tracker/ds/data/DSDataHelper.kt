package hh.game.mgba_android.tracker.ds.data

import hh.game.mgba_android.tracker.ds.models.DSGameVersion

/**
 * Memory addresses for DS Pokemon games, derived from PKLuaScript (Platinum).
 *
 * All addresses are physical ARM9 main RAM addresses (0x02xxxxxx range).
 *
 * VERIFIED AGAINST: PKLuaScript (Platinum US only).
 * Diamond/Pearl offsets differ — add when confirmed from a reference source.
 */
object DSDataHelper {

    // ── Party data ────────────────────────────────────────────────────────────
    // Address of the pointer to SaveBlock1. Dereference to get SaveBlock1 base.
    const val PLATINUM_SAVE_BLOCK1_PTR = 0x02101D2C

    // Offset from SaveBlock1 base to the start of party slot 0.
    const val PLATINUM_PARTY_OFFSET    = 0xD094

    // Offset from SaveBlock1 base to the party count (number of Pokemon in party, 0-6).
    const val PLATINUM_PARTY_COUNT_OFFSET = 0xD0E4

    // Bytes per party-slot Pokemon structure.
    const val PARTY_SLOT_STRIDE = 0xEC  // 236 bytes

    // ── Party slot layout (offsets within a single 0xEC-byte slot) ────────────
    // These are the UNENCRYPTED fields (outside the 0x08-0x87 encrypted block).

    /** PID (personality value). u32. Used to derive block shuffle order. */
    const val SLOT_OFF_PID        = 0x00

    /** Checksum of the 4 encrypted blocks. u16. Seed for first decryption pass. */
    const val SLOT_OFF_CHECKSUM   = 0x06

    /** Start of the 4 encrypted/shuffled sub-blocks (128 bytes total). */
    const val SLOT_OFF_BLOCKS_START = 0x08

    /** Each sub-block is 32 bytes. */
    const val BLOCK_SIZE          = 32

    // Party-only fields (after 0x88, not encrypted):
    /** Status condition flags. u32. bit0=sleep counter, bit3=poison, bit4=burn, bit5=freeze, bit6=paralysis */
    const val SLOT_OFF_STATUS     = 0x88

    /** Level. u8. */
    const val SLOT_OFF_LEVEL      = 0x8A

    /** Current HP. u16. */
    const val SLOT_OFF_CUR_HP     = 0x8C

    /** Max HP. u16. */
    const val SLOT_OFF_MAX_HP     = 0x8E

    /** Attack. u16. */
    const val SLOT_OFF_ATK        = 0x90

    /** Defense. u16. */
    const val SLOT_OFF_DEF        = 0x92

    /** Speed. u16. */
    const val SLOT_OFF_SPE        = 0x94

    /** Sp. Atk. u16. */
    const val SLOT_OFF_SPA        = 0x96

    /** Sp. Def. u16. */
    const val SLOT_OFF_SPD        = 0x98

    // ── Block A offsets (within decrypted 32-byte block) ─────────────────────
    const val BLOCK_A_SPECIES     = 0x00  // u16
    const val BLOCK_A_HELD_ITEM   = 0x02  // u16
    const val BLOCK_A_OT_ID       = 0x04  // u16 (visible)
    const val BLOCK_A_SECRET_ID   = 0x06  // u16
    const val BLOCK_A_EXPERIENCE  = 0x08  // u32
    const val BLOCK_A_FRIENDSHIP  = 0x0C  // u8
    const val BLOCK_A_ABILITY     = 0x0D  // u8
    const val BLOCK_A_HP_EV       = 0x10  // u8
    const val BLOCK_A_ATK_EV      = 0x11  // u8
    const val BLOCK_A_DEF_EV      = 0x12  // u8
    const val BLOCK_A_SPE_EV      = 0x13  // u8
    const val BLOCK_A_SPA_EV      = 0x14  // u8
    const val BLOCK_A_SPD_EV      = 0x15  // u8

    // ── Block B offsets ───────────────────────────────────────────────────────
    const val BLOCK_B_MOVE1       = 0x00  // u16
    const val BLOCK_B_MOVE2       = 0x02  // u16
    const val BLOCK_B_MOVE3       = 0x04  // u16
    const val BLOCK_B_MOVE4       = 0x06  // u16
    const val BLOCK_B_PP1         = 0x08  // u8
    const val BLOCK_B_PP2         = 0x09  // u8
    const val BLOCK_B_PP3         = 0x0A  // u8
    const val BLOCK_B_PP4         = 0x0B  // u8
    /** Packed IVs. bits 0-4=HP, 5-9=Atk, 10-14=Def, 15-19=Spd, 20-24=SpAtk, 25-29=SpDef, 30=IsEgg, 31=AbilitySlot */
    const val BLOCK_B_IV_WORD     = 0x10  // u32

    // ── Block C offsets ───────────────────────────────────────────────────────
    /** Nickname: 11 UTF-16LE code units (22 bytes). */
    const val BLOCK_C_NICKNAME    = 0x00
    const val BLOCK_C_NICKNAME_LEN = 11

    // ── Helpers ───────────────────────────────────────────────────────────────
    /**
     * Returns the SaveBlock1 pointer address for the given game version.
     * Only Platinum is verified; Diamond/Pearl are TODO.
     */
    fun saveBlock1PtrAddress(version: DSGameVersion): Int? = when (version) {
        DSGameVersion.PLATINUM, DSGameVersion.PLATINUM_JP -> PLATINUM_SAVE_BLOCK1_PTR
        else -> null  // Diamond/Pearl addresses not yet verified
    }

    fun partyOffset(version: DSGameVersion): Int = when (version) {
        DSGameVersion.PLATINUM, DSGameVersion.PLATINUM_JP -> PLATINUM_PARTY_OFFSET
        else -> PLATINUM_PARTY_OFFSET  // placeholder until Diamond/Pearl verified
    }

    fun partyCountOffset(version: DSGameVersion): Int = when (version) {
        DSGameVersion.PLATINUM, DSGameVersion.PLATINUM_JP -> PLATINUM_PARTY_COUNT_OFFSET
        else -> PLATINUM_PARTY_COUNT_OFFSET
    }
}
