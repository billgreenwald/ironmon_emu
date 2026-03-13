package hh.game.mgba_android.tracker.models

enum class Gender { MALE, FEMALE, NONE }

data class PokemonData(
    val slot: Int,
    val speciesId: Int,
    val speciesName: String,
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
    // Phase A additions
    val nature: Int,          // 0–24 (personality % 25)
    val abilityIndex: Int,    // 0 or 1 (Misc substructure bit 31)
    val ability1Id: Int,      // from base stats ROM
    val ability2Id: Int,      // from base stats ROM
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val baseSpd: Int,
    val baseSpAtk: Int,
    val baseSpDef: Int,
    val expGroup: Int,        // 0–5 growth rate group
    val gender: Gender,
    val isShiny: Boolean,
    val hasPokerus: Boolean,
) {
    val isAlive: Boolean get() = currentHp > 0
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
    val bst: Int get() = baseHp + baseAtk + baseDef + baseSpd + baseSpAtk + baseSpDef
    val abilityId: Int get() =
        if (abilityIndex == 0 || ability2Id == 0) ability1Id else ability2Id
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
