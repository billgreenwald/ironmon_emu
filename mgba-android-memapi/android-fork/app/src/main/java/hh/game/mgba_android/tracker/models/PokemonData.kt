package hh.game.mgba_android.tracker.models

enum class Gender { MALE, FEMALE, NONE }

data class PokemonData(
    val slot: Int,
    val speciesId: Int,
    val speciesName: String,
    val nickname: String,     // decoded from bytes 0x08–0x11; empty if matches species name or blank
    val level: Int,
    val currentHp: Int,
    val maxHp: Int,
    val type1: Int,
    val type2: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val spAtk: Int,
    val spDef: Int,
    val moves: List<MoveData>,
    val heldItemId: Int,
    val experience: Int,
    // Status condition (raw byte at 0x50): bits 0-2=sleep, 3=PSN, 4=BRN, 5=FRZ, 6=PAR, 7=TOX
    val statusCondition: Int,
    // Phase A additions
    val nature: Int,          // 0–24 (personality % 25)
    val abilityIndex: Int,    // 0 or 1 (Misc substructure bit 31)
    val ability1Id: Int,      // from base stats ROM
    val ability2Id: Int,      // from base stats ROM
    val bst: Int,             // Base Stat Total from BstTable (static lookup)
    val expGroup: Int,        // 0–5 growth rate group
    val gender: Gender,
    val isShiny: Boolean,
    val hasPokerus: Boolean,
    // IVs (0–31 each) from Misc substructure ivWord
    val ivHp: Int,
    val ivAtk: Int,
    val ivDef: Int,
    val ivSpe: Int,
    val ivSpA: Int,
    val ivSpD: Int,
    // EVs (0–255 each) from Effort substructure
    val evHp: Int,
    val evAtk: Int,
    val evDef: Int,
    val evSpe: Int,
    val evSpA: Int,
    val evSpD: Int,
    // Friendship (0–255) from Growth substructure byte 9
    val friendship: Int,
    // Hidden Power type (0–17, Gen III type ID) computed from IVs
    val hiddenPowerType: Int,
    // Base stats from ROM (needed for GachaMon rating thresholds; 0 if unavailable)
    val baseHp: Int = 0,
    val baseAtk: Int = 0,
    val baseDef: Int = 0,
    val baseSpa: Int = 0,
    val baseSpd: Int = 0,
    val baseSpe: Int = 0,
    // GachaMon star rating (computed by TrackerPoller for the lead Pokémon; 0 = not rated)
    val ratingScore: Int = 0,
    val starRating: Int = 0,
) {
    val isAlive: Boolean get() = currentHp > 0
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
    val abilityId: Int get() =
        if (abilityIndex == 0 || ability2Id == 0) ability1Id else ability2Id
    val displayName: String get() = nickname.ifEmpty { speciesName }
}

data class MoveData(
    val moveId: Int,
    val moveName: String,
    val pp: Int,
    val maxPp: Int,
    val power: Int,     // 0 = status/variable
    val accuracy: Int,  // 0 = always hits
    val moveType: Int,  // Gen III ROM type ID
) {
    val isEmpty: Boolean get() = moveId == 0

    companion object {
        val EMPTY = MoveData(
            moveId = 0, moveName = "---", pp = 0, maxPp = 0,
            power = 0, accuracy = 0, moveType = 0,
        )
    }
}
