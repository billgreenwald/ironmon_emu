package com.ironmon.tracker.data.models

/**
 * Fully decoded Pokémon from a party slot.
 * Populated by PokemonDecoder from raw GBA memory bytes.
 */
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
) {
    val isAlive: Boolean get() = currentHp > 0
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
}

data class MoveData(
    val moveId: Int,
    val moveName: String,
    val pp: Int,
    val maxPp: Int,
) {
    val isEmpty: Boolean get() = moveId == 0

    companion object {
        val EMPTY = MoveData(moveId = 0, moveName = "---", pp = 0, maxPp = 0)
    }
}
