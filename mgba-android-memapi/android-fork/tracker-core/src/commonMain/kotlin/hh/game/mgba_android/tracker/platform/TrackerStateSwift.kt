package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.models.BattleState
import hh.game.mgba_android.tracker.models.EnemyData
import hh.game.mgba_android.tracker.models.Gender
import hh.game.mgba_android.tracker.models.GameVersion
import hh.game.mgba_android.tracker.models.PokemonData
import hh.game.mgba_android.tracker.models.TrackerState

/**
 * Swift-friendly bridge for the shared tracker models.
 *
 * SwiftUI reads plain data-class properties directly (that works fine for Int/String/Bool/nested
 * objects). This object only smooths over the few things that are awkward across the Kotlin/Native
 * boundary: the sealed-class discrimination, the function-typed resolver fields on `Active`, lookups
 * into `Int`-keyed `Map`s, `Pair`, and `IntArray`. Keep Swift-facing types primitive/simple here.
 */
object TrackerStateSwift {

    // ── Sealed-state discrimination ──────────────────────────────────────────────────────────────
    fun active(state: TrackerState): TrackerState.Active? = state as? TrackerState.Active
    fun isNoGameLoaded(state: TrackerState): Boolean = state is TrackerState.NoGameLoaded
    fun isDisconnected(state: TrackerState): Boolean = state is TrackerState.Disconnected

    // ── Function-typed resolvers on Active (awkward to call as Kotlin lambdas from Swift) ─────────
    fun speciesName(active: TrackerState.Active, speciesId: Int): String = active.speciesName(speciesId)
    fun natDexId(active: TrackerState.Active, speciesId: Int): Int = active.natDexId(speciesId)

    // ── Int-keyed Map / Pair accessors ───────────────────────────────────────────────────────────
    fun encountersForRoute(active: TrackerState.Active, mapId: Int): List<Int> =
        active.routeEncounters[mapId] ?: emptyList()

    fun routeHasTrainers(active: TrackerState.Active, mapId: Int): Boolean =
        active.trainerCounts.containsKey(mapId)

    fun trainerDefeated(active: TrackerState.Active, mapId: Int): Int =
        active.trainerCounts[mapId]?.first ?: 0

    fun trainerTotal(active: TrackerState.Active, mapId: Int): Int =
        active.trainerCounts[mapId]?.second ?: 0

    fun moveCategory(active: TrackerState.Active, moveId: Int): Int =
        active.moveCategories[moveId] ?: 0

    fun note(active: TrackerState.Active, speciesId: Int): String? =
        active.pokemonNotes[speciesId]

    fun isRouteVisited(active: TrackerState.Active, mapId: Int): Boolean =
        mapId in active.visitedRoutes

    /** Hoenn games use a different route-name/encounter-slot table. Avoids Swift enum comparison. */
    fun isHoenn(active: TrackerState.Active): Boolean = active.game == GameVersion.RUBY ||
        active.game == GameVersion.SAPPHIRE || active.game == GameVersion.EMERALD

    fun enemyPp(enemy: EnemyData, moveId: Int): Int = enemy.ppByMoveId[moveId] ?: -1

    // ── Enum / bitfield display helpers (awkward or ambiguous from Swift) ─────────────────────────
    /** "♂" / "♀" / "" — avoids the Swift name-clash on Gender.NONE. */
    fun genderSymbol(mon: PokemonData): String = when (mon.gender) {
        Gender.MALE -> "♂"
        Gender.FEMALE -> "♀"
        Gender.NONE -> ""
    }

    /**
     * Decode the status byte (party or enemy): bits 0-2 = sleep turns, 3=PSN, 4=BRN, 5=FRZ, 6=PAR,
     * 7=TOX. Returns a short label ("SLP"/"PSN"/…) or null when healthy. Sleep shows remaining turns.
     */
    fun statusLabel(status: Int): String? = when {
        status == 0 -> null
        (status and 0x07) != 0 -> "SLP ${status and 0x07}"
        (status and 0x08) != 0 -> "PSN"
        (status and 0x10) != 0 -> "BRN"
        (status and 0x20) != 0 -> "FRZ"
        (status and 0x40) != 0 -> "PAR"
        (status and 0x80) != 0 -> "TOX"
        else -> null
    }

    // ── Stat stages: IntArray → a flat 7-field struct Swift can read by name ─────────────────────
    fun playerStatStages(battle: BattleState): StatStages? =
        battle.playerStatStages?.let { StatStages.of(it) }

    fun enemyStatStages(enemy: EnemyData): StatStages? =
        enemy.statStages?.let { StatStages.of(it) }
}

/** Battle stat stages in display order; 6 = neutral. Mirrors IntArray [Atk,Def,SpA,SpD,Spe,Acc,Eva]. */
data class StatStages(
    val atk: Int,
    val def: Int,
    val spa: Int,
    val spd: Int,
    val spe: Int,
    val acc: Int,
    val eva: Int,
) {
    val anyChanged: Boolean
        get() = atk != 6 || def != 6 || spa != 6 || spd != 6 || spe != 6 || acc != 6 || eva != 6

    /** Ordered [label, delta] pairs for rendering (delta = stage - 6). */
    fun deltas(): List<StatDelta> = listOf(
        StatDelta("Atk", atk - 6), StatDelta("Def", def - 6), StatDelta("SpA", spa - 6),
        StatDelta("SpD", spd - 6), StatDelta("Spe", spe - 6), StatDelta("Acc", acc - 6),
        StatDelta("Eva", eva - 6),
    )

    companion object {
        fun of(a: IntArray): StatStages = StatStages(a[0], a[1], a[2], a[3], a[4], a[5], a[6])
    }
}

data class StatDelta(val label: String, val delta: Int)
