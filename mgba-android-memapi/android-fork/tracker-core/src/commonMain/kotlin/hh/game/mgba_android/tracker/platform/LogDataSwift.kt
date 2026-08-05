package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.data.RandomizerLog
import hh.game.mgba_android.tracker.models.LogData
import hh.game.mgba_android.tracker.models.LogLevelMove
import hh.game.mgba_android.tracker.models.LogPartyMon
import hh.game.mgba_android.tracker.models.LogPokemon
import hh.game.mgba_android.tracker.models.LogTM
import hh.game.mgba_android.tracker.models.LogTrainer
import hh.game.mgba_android.tracker.models.TrackerState

/**
 * Swift-friendly access to a parsed randomizer log. The shared parser fills `Int`-keyed maps that
 * are awkward to iterate from Swift, so expose them as ordered lists. Parsing is shared
 * (`RandomizerLog`); iOS supplies the lines from a document picker.
 */
object LogDataSwift {

    /** Parse a randomizer .log the user imported. Returns null on parse failure. */
    fun parse(active: TrackerState.Active, lines: List<String>): LogData? =
        RandomizerLog(active.game, active.isMaxFr, active.isNatDex).parse(lines)

    fun pokemon(data: LogData): List<LogPokemon> = data.pokemon.values.toList()
    fun trainers(data: LogData): List<LogTrainer> = data.trainers.values.toList()
    fun routes(data: LogData): List<hh.game.mgba_android.tracker.models.LogRoute> = data.routes.values.toList()
    fun tms(data: LogData): List<LogTM> = data.tms.values.sortedBy { it.tmNumber }

    fun moveSet(p: LogPokemon): List<LogLevelMove> = p.moveSet.toList()
    fun evolutions(p: LogPokemon): List<Int> = p.evolutions.toList()
    fun tmMoves(p: LogPokemon): List<Int> = p.tmMoves.toList()
    fun party(t: LogTrainer): List<LogPartyMon> = t.party.toList()
    fun partyMoveIds(m: LogPartyMon): List<Int> = m.moveIds.toList()
}
