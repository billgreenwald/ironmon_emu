package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.models.PokemonData
import hh.game.mgba_android.tracker.tables.AbilityRatingTable
import hh.game.mgba_android.tracker.tables.MoveRatingTable
import hh.game.mgba_android.tracker.tables.NatureTable
import hh.game.mgba_android.tracker.tables.TypeChart
import kotlin.math.floor
import kotlin.math.min

enum class GachaMonRuleset(val label: String) {
    STANDARD("Standard"),
    ULTIMATE("Ultimate"),
    KAIZO("Kaizo"),
    SURVIVAL("Survival"),
    SUPER_KAIZO("Super Kaizo"),
    SUBPAR("Subpar"),
}

/**
 * Ports GachaMonData.calculateRatingScore() and calculateStars() from the Lua tracker.
 * Source of truth: Ironmon-Tracker/ironmon_tracker/data/GachaMonData.lua lines 249–542
 * and GachaMonRatingSystem.json.
 */
object GachaMonRating {

    // ── Ability IDs ────────────────────────────────────────────────────────────
    private const val ABILITY_DRIZZLE       = 2
    private const val ABILITY_COMPOUND_EYES = 14
    private const val ABILITY_FLASH_FIRE    = 18
    private const val ABILITY_LEVITATE      = 26
    private const val ABILITY_SAND_STREAM   = 45
    private const val ABILITY_THICK_FAT     = 47
    private const val ABILITY_ROCK_HEAD     = 69
    private const val ABILITY_DROUGHT       = 70
    private const val ABILITY_VOLT_ABSORB   = 10
    private const val ABILITY_WATER_ABSORB  = 11

    // ── Type IDs (Gen III ROM) ─────────────────────────────────────────────────
    private const val TYPE_GROUND   = 4
    private const val TYPE_FIRE     = 10
    private const val TYPE_WATER    = 11
    private const val TYPE_ELECTRIC = 13
    private const val TYPE_ICE      = 15

    // ── Move ID sets (from Lua MoveData.lua) ──────────────────────────────────
    private val OHKO_MOVES    = setOf(12, 32, 90, 329)       // Guillotine, Horn Drill, Fissure, Sheer Cold
    private val RECOIL_MOVES  = setOf(36, 38, 66, 344)       // Take Down, Double-Edge, Submission, Volt Tackle

    // ── Gen III physical/special split by type ────────────────────────────────
    // Physical: Normal(0), Fighting(1), Flying(2), Poison(3), Ground(4), Rock(5), Bug(6), Ghost(7), Steel(8)
    private val PHYSICAL_TYPES = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8)

    // ── Sand-safe types (Rock/Ground/Steel don't take sandstorm damage) ────────
    private val SAND_SAFE_TYPES = setOf(4, 5, 8)  // Ground, Rock, Steel

    // ── Type-defensive abilities: abilityId → set of attacking types improved ─
    // From Lua AbilityData.getTypeDefensiveAbilities()
    private val TYPE_DEFENSIVE_ABILITIES = mapOf(
        ABILITY_DRIZZLE      to setOf(TYPE_FIRE),
        ABILITY_VOLT_ABSORB  to setOf(TYPE_ELECTRIC),
        ABILITY_WATER_ABSORB to setOf(TYPE_WATER),
        ABILITY_FLASH_FIRE   to setOf(TYPE_FIRE),
        ABILITY_LEVITATE     to setOf(TYPE_GROUND),
        ABILITY_THICK_FAT    to setOf(TYPE_FIRE, TYPE_ICE),
        ABILITY_DROUGHT      to setOf(TYPE_WATER),
    )

    // ── Ruleset ban/adjust sets (from GachaMonRatingSystem.json "Rulesets") ───
    private val BANNED_ABILITIES: Map<GachaMonRuleset, Set<Int>> = mapOf(
        GachaMonRuleset.STANDARD    to emptySet(),
        GachaMonRuleset.ULTIMATE    to emptySet(),
        GachaMonRuleset.KAIZO       to setOf(37, 74),
        GachaMonRuleset.SURVIVAL    to setOf(4, 37, 74, 75),
        GachaMonRuleset.SUPER_KAIZO to setOf(4, 37, 53, 74, 75),
        GachaMonRuleset.SUBPAR      to setOf(4, 37, 53, 74, 75),
    )

    private val BANNED_MOVES: Map<GachaMonRuleset, Set<Int>> = mapOf(
        GachaMonRuleset.STANDARD    to emptySet(),
        GachaMonRuleset.ULTIMATE    to setOf(15, 19, 57, 70, 127, 148, 249, 291),
        GachaMonRuleset.KAIZO       to setOf(
            15, 19, 57, 70, 127, 148, 249, 291,
            71, 72, 73, 105, 135, 138, 141, 147, 156, 202, 208,
            215, 220, 226, 234, 235, 236, 256, 273, 274, 275, 287, 303, 312, 356,
        ),
        GachaMonRuleset.SURVIVAL    to setOf(
            19, 57, 70, 127, 148, 249, 291,
            73, 105, 135, 156, 208, 220, 226, 234, 235, 236, 256, 273, 274, 275, 303,
        ),
        GachaMonRuleset.SUPER_KAIZO to setOf(
            15, 19, 57, 70, 127, 148, 249, 291,
            73, 105, 135, 147, 156, 208, 215, 220, 226, 234, 235, 236, 256, 273, 274, 275, 287, 303, 312,
        ),
        GachaMonRuleset.SUBPAR      to setOf(
            15, 19, 57, 70, 127, 148, 249, 291,
            73, 105, 135, 147, 156, 208, 215, 220, 226, 234, 235, 236, 256, 273, 274, 275, 287, 303, 312,
        ),
    )

    private val ADJUSTED_MOVES: Map<GachaMonRuleset, Set<Int>> = mapOf(
        GachaMonRuleset.STANDARD    to emptySet(),
        GachaMonRuleset.ULTIMATE    to emptySet(),
        GachaMonRuleset.KAIZO       to emptySet(),
        GachaMonRuleset.SURVIVAL    to emptySet(),
        GachaMonRuleset.SUPER_KAIZO to setOf(
            14, 74, 96, 104, 106, 107, 110, 111, 112, 116,
            133, 151, 159, 174, 187, 268, 294, 322, 334, 336, 339, 347, 349,
        ),
        GachaMonRuleset.SUBPAR      to setOf(
            14, 71, 72, 74, 96, 104, 106, 107, 110, 111, 112, 116,
            133, 138, 141, 151, 159, 174, 187, 202, 268, 294, 322, 334, 336, 339, 347, 349, 356,
        ),
    )

    // ── Star thresholds (descending — first match wins) ───────────────────────
    // From GachaMonRatingSystem.json "RatingToStars"
    private val RATING_TO_STARS = listOf(80 to 6, 67 to 5, 54 to 4, 40 to 3, 25 to 2, 0 to 1)

    // ── Offensive stat thresholds (from JSON "Stats.Offensive") ───────────────
    private data class StatThreshold(val baseStat: Int, val points: Double)
    private val OFFENSIVE_THRESHOLDS = listOf(
        StatThreshold(110, 15.0),
        StatThreshold(90,  10.0),
        StatThreshold(60,   5.0),
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calculates the GachaMon rating score (0–100+).
     * Ported from GachaMonData.calculateRatingScore() (Lua lines 249–483).
     */
    fun calculateRatingScore(pokemon: PokemonData, ruleset: GachaMonRuleset): Int {
        val bannedAbilities = BANNED_ABILITIES[ruleset] ?: emptySet()
        val bannedMoves     = BANNED_MOVES[ruleset]     ?: emptySet()
        val adjustedMoves   = ADJUSTED_MOVES[ruleset]   ?: emptySet()
        var total = 0.0

        val abilityId = pokemon.abilityId

        // ── ABILITY ─────────────────────────────────────────────────────────
        var abilityRating = AbilityRatingTable.get(abilityId).toDouble()
        if (abilityId in bannedAbilities) abilityRating = 0.0

        if (abilityRating > 0) {
            // Bonus if ability covers a x2 or x4 weakness
            val coveredTypes = TYPE_DEFENSIVE_ABILITIES[abilityId]
            if (coveredTypes != null) {
                val improvesWeakness = coveredTypes.any { attType ->
                    val eff = typeEffectiveness(attType, pokemon.type1, pokemon.type2)
                    eff >= 2.0f
                }
                if (improvesWeakness) abilityRating *= 1.5
            }
        }
        // Sand Stream: +2 if Rock/Ground/Steel type, -2 otherwise
        if (abilityId == ABILITY_SAND_STREAM) {
            if (pokemon.type1 in SAND_SAFE_TYPES || pokemon.type2 in SAND_SAFE_TYPES) {
                abilityRating += 2.0
            } else {
                abilityRating -= 2.0
            }
        }
        abilityRating = min(abilityRating, 999.0)
        total += abilityRating

        // ── Weather / ability modifiers for move scoring ─────────────────────
        val badWeatherTypes: Set<Int> = when (abilityId) {
            ABILITY_DRIZZLE -> setOf(TYPE_FIRE)
            ABILITY_DROUGHT -> setOf(TYPE_WATER)
            else -> emptySet()
        }
        val compoundEyes = abilityId == ABILITY_COMPOUND_EYES
        val rockHead     = abilityId == ABILITY_ROCK_HEAD

        // ── MOVES ────────────────────────────────────────────────────────────
        data class IMove(val moveId: Int, val moveType: Int, val power: Int, val accuracy: Int, var rating: Float)

        var anyPhysical = false
        var anySpecial  = false

        val iMoves = pokemon.moves.filter { !it.isEmpty }.map { move ->
            var rating = MoveRatingTable.get(move.moveId)
            when {
                move.moveId in bannedMoves   -> rating = 0f
                move.moveId in adjustedMoves -> rating *= 0.5f
            }
            if (rating != 0f) {
                val isPhysical = move.moveType in PHYSICAL_TYPES
                val isDamaging = move.power > 0
                if (isDamaging) {
                    if (isPhysical) anyPhysical = true else anySpecial = true
                    if (move.moveType in badWeatherTypes) rating *= 0.65f
                }
                if (compoundEyes && move.moveId !in OHKO_MOVES && move.accuracy in 1 until 100) {
                    rating *= 1.1f
                }
                if (rockHead && move.moveId in RECOIL_MOVES) {
                    rating *= 1.1f
                }
                // STAB bonus
                if (move.moveType == pokemon.type1 || move.moveType == pokemon.type2) {
                    rating *= 1.5f
                }
            }
            IMove(move.moveId, move.moveType, move.power, move.accuracy, rating)
        }.toMutableList()

        // Duplicate type coverage penalty (lower-rated same-type damaging move gets 0.65x)
        for (iMove in iMoves) {
            for (cMove in iMoves) {
                if (iMove !== cMove
                    && iMove.rating < cMove.rating
                    && iMove.moveType == cMove.moveType
                    && iMove.power > 0 && cMove.power > 0
                ) {
                    iMove.rating *= 0.65f
                    break
                }
            }
        }

        var movesRating = iMoves.sumOf { it.rating.toDouble() }
        movesRating = min(movesRating, 999.0)
        total += movesRating

        // ── OFFENSIVE STATS ──────────────────────────────────────────────────
        var offAtk = pokemon.baseAtk
        var offSpa = pokemon.baseSpa
        var offensiveRating = 0.0
        if (offAtk < 50 && offSpa < 50) {
            offensiveRating += -5.0
        } else {
            for ((baseStat, points) in OFFENSIVE_THRESHOLDS) {
                if (offAtk >= baseStat) {
                    offensiveRating += points * (if (anyPhysical) 1.0 else 0.20)
                    offAtk = 0
                }
                if (offSpa >= baseStat) {
                    offensiveRating += points * (if (anySpecial) 1.0 else 0.20)
                    offSpa = 0
                }
            }
        }
        offensiveRating = min(offensiveRating, 20.0)
        total += offensiveRating

        // ── DEFENSIVE STATS ──────────────────────────────────────────────────
        if (pokemon.baseHp + pokemon.baseDef + pokemon.baseSpd >= 240) total += 5.0

        // ── SPEED ────────────────────────────────────────────────────────────
        total += when {
            pokemon.baseSpe >= 90 -> 10.0
            pokemon.baseSpe >= 60 ->  5.0
            else                  ->  0.0
        }

        // ── NATURE ───────────────────────────────────────────────────────────
        // Check nature effect on the dominant offensive stat (uses computed stats,
        // matching Lua's gachamon:getStats() behaviour)
        val nature = NatureTable.get(pokemon.nature)
        val useAtk = pokemon.attack > pokemon.spAtk
        total += when {
            useAtk && nature.boostedStat == 0  ->  2.0   // nature boosts Atk
            useAtk && nature.reducedStat == 0  -> -1.0   // nature reduces Atk
            !useAtk && nature.boostedStat == 2 ->  2.0   // nature boosts SpA
            !useAtk && nature.reducedStat == 2 -> -1.0   // nature reduces SpA
            else                               ->  0.0
        }

        return floor(total + 0.5).toInt()
    }

    /**
     * Converts a rating score to 0–6 stars.
     * Stars == 6 is the "5+" tier. Stars == 0 means no rating (score ≤ 0).
     * Ported from GachaMonData.calculateStars() (Lua lines 529–542).
     */
    fun calculateStars(score: Int): Int {
        if (score <= 0) return 0
        for ((threshold, stars) in RATING_TO_STARS) {
            if (score >= threshold) return stars
        }
        return 0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Combined type effectiveness for a dual-type defender. */
    private fun typeEffectiveness(attType: Int, defType1: Int, defType2: Int): Float {
        val e1 = TypeChart.effectiveness(attType, defType1)
        return if (defType2 == defType1) e1 else e1 * TypeChart.effectiveness(attType, defType2)
    }
}
