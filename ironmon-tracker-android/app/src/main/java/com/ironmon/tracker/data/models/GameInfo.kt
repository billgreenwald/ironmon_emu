package com.ironmon.tracker.data.models

/**
 * Detected GBA game version.
 * Mirrors GameSettings.lua game detection logic.
 */
enum class GameVersion(val displayName: String) {
    FIRE_RED("Pokémon Fire Red"),
    LEAF_GREEN("Pokémon Leaf Green"),
    RUBY("Pokémon Ruby"),
    SAPPHIRE("Pokémon Sapphire"),
    EMERALD("Pokémon Emerald"),
    UNKNOWN("Unknown Game"),
}
