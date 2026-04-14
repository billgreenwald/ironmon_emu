package hh.game.mgba_android.tracker.ds.models

data class DSPokemonData(
    val speciesId: Int,
    val level: Int,
    val currentHp: Int,
    val maxHp: Int,
    val nature: Int,           // personality % 25
    val abilityIndex: Int,     // 0 or 1 (slot, not ability ID)
    val abilityId: Int,        // resolved ability ID from species table
    val heldItemId: Int,
    val experience: Long,
    val friendship: Int,

    // Stats (actual, computed from base + IVs + EVs + nature)
    val atk: Int,
    val def: Int,
    val spe: Int,
    val spa: Int,
    val spd: Int,

    // IVs (0-31 each)
    val hpIv: Int,
    val atkIv: Int,
    val defIv: Int,
    val speIv: Int,
    val spaIv: Int,
    val spdIv: Int,

    // EVs (0-255 each)
    val hpEv: Int,
    val atkEv: Int,
    val defEv: Int,
    val speEv: Int,
    val spaEv: Int,
    val spdEv: Int,

    // Moves (species ID 0 = no move)
    val move1Id: Int,
    val move2Id: Int,
    val move3Id: Int,
    val move4Id: Int,
    val move1Pp: Int,
    val move2Pp: Int,
    val move3Pp: Int,
    val move4Pp: Int,

    // Metadata
    val nickname: String,
    val isEgg: Boolean,
    val statusFlags: Int,      // raw status condition word
    val type1: Int,
    val type2: Int,
)
