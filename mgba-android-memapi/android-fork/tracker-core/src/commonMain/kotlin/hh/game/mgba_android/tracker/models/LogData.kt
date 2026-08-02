package hh.game.mgba_android.tracker.models

/**
 * Parsed contents of a Universal Pokémon Randomizer `.log` spoiler file.
 *
 * Direct port of `RandomizerLog.Data` from the Ironmon Tracker Lua
 * (ironmon_tracker/data/RandomizerLog.lua). All Pokémon/move/trainer/route
 * collections are keyed by the Gen III **internal** species/id numbers so they
 * line up with the app's existing lookup tables (SpeciesNames, MoveNames, etc.).
 *
 * Mutable-friendly: the parser fills these incrementally across log sectors
 * (base stats, then evolutions, movesets, TM compatibility all target the same
 * LogPokemon), matching the Lua's in-place mutation approach.
 */
class LogData {
    val settings = LogSettings()
    val pokemon: MutableMap<Int, LogPokemon> = LinkedHashMap()
    val moves: MutableMap<Int, LogMove> = LinkedHashMap()
    val tms: MutableMap<Int, LogTM> = LinkedHashMap()
    val trainers: MutableMap<Int, LogTrainer> = LinkedHashMap()
    val routes: MutableMap<Int, LogRoute> = LinkedHashMap()
}

class LogSettings {
    var version: String? = null
    var randomSeed: String? = null
    var settingsString: String? = null
    var game: String? = null
}

class LogPokemon(val id: Int) {
    var name: String = ""
    /** Primary type id (TypeChart id), or null if unresolved. */
    var type1: Int? = null
    /** Secondary type id, or null if mono-typed. */
    var type2: Int? = null
    var baseStats: LogBaseStats = LogBaseStats()
    /** Ability slot 1 id (defaults to 0 / None if unresolved). */
    var ability1: Int = 0
    /** Ability slot 2 id, or null if the species has only one ability. */
    var ability2: Int? = null
    var heldItems: String? = null
    val evolutions: MutableList<Int> = mutableListOf()
    val preEvolutions: MutableList<Int> = mutableListOf()
    /** Level-up moves in learn order (level 0 = "learned upon evolution"). */
    val moveSet: MutableList<LogLevelMove> = mutableListOf()
    /** TM numbers this species can learn. */
    val tmMoves: MutableList<Int> = mutableListOf()

    val bst: Int get() = baseStats.total
}

class LogBaseStats(
    var hp: Int = 0,
    var atk: Int = 0,
    var def: Int = 0,
    var spa: Int = 0,
    var spd: Int = 0,
    var spe: Int = 0,
) {
    val total: Int get() = hp + atk + def + spa + spd + spe
}

/** A single level-up move entry; [name] is the (possibly custom) log name. */
class LogLevelMove(val level: Int, val moveId: Int, val name: String)

class LogMove(
    val moveId: Int,
    val name: String,
    val type: Int?,
    val power: Int?,
    val acc: Int?,
    val pp: Int?,
)

class LogTM(val tmNumber: Int, val moveId: Int, val name: String)

class LogTrainer(
    val num: Int,
    val name: String,
    val trainerClass: String,
    val fullname: String,
    val customName: String,
    val customClass: String,
) {
    var minLevel: Int? = null
    var maxLevel: Int? = null
    var avgLevel: Double? = null
    val party: MutableList<LogPartyMon> = mutableListOf()
}

class LogPartyMon(
    val pokemonId: Int,
    val heldItem: String?,
    val level: Int,
) {
    /** Up to 4 moves this mon knows at [level], reconstructed from its level-up set. */
    val moveIds: MutableList<Int> = mutableListOf()
}

class LogRoute(val mapId: Int, var name: String) {
    var numTrainers: Int = 0
    var minTrainerLv: Int? = null
    var maxTrainerLv: Int? = null
    var avgTrainerLv: Double? = null
    var numWilds: Int = 0
    var minWildLv: Int? = null
    var maxWildLv: Int? = null
    /** Keyed by encounter-area key: "Trainers","GrassCave","Surfing","RockSmash","OldRod","GoodRod","SuperRod". */
    val encounterAreas: MutableMap<String, LogEncounterArea> = LinkedHashMap()
}

class LogEncounterArea(val key: String) {
    /** Trainer ids (only for the "Trainers" area). */
    val trainers: MutableList<Int> = mutableListOf()
    /** Wild encounters keyed by internal species id. */
    val pokemon: MutableMap<Int, LogEncounter> = LinkedHashMap()
}

class LogEncounter {
    var index: Int = 0
    var levelMin: Int = 100
    var levelMax: Int = 0
    /** Combined encounter rate 0..1 (summed across duplicate slots). */
    var rate: Double = 0.0
}
