package hh.game.mgba_android.tracker.persistence

import android.content.Context
import hh.game.mgba_android.tracker.data.LogFileLocator
import hh.game.mgba_android.tracker.data.RandomizerLog
import hh.game.mgba_android.tracker.models.GameVersion
import hh.game.mgba_android.tracker.models.LogData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Locates, parses, and memoizes the randomizer `.log` for the loaded ROM.
 *
 * Mirrors the Lua `RandomizerLog.hasParsedThisLog` behavior: the parse is cached
 * in-memory keyed by ROM filename, so re-opening the log viewer is instant and
 * only re-parses when the ROM (and therefore the log) changes.
 */
object LogRepository {

    @Volatile private var cachedKey: String? = null
    @Volatile private var cached: LogData? = null

    sealed interface Result {
        data class Success(val data: LogData) : Result
        object NotFound : Result
        object ParseFailed : Result
    }

    /**
     * Returns the parsed log for [romFileName], reading + parsing off the main
     * thread on first request and serving the cache thereafter.
     */
    suspend fun getLog(
        context: Context,
        romFileName: String,
        game: GameVersion,
        isMaxFr: Boolean,
        isNatDex: Boolean,
    ): Result = withContext(Dispatchers.Default) {
        cached?.let { if (cachedKey == romFileName) return@withContext Result.Success(it) }

        val lines = LogFileLocator.readLines(context, romFileName) ?: return@withContext Result.NotFound
        val data = RandomizerLog(game, isMaxFr, isNatDex).parse(lines) ?: return@withContext Result.ParseFailed

        cachedKey = romFileName
        cached = data
        Result.Success(data)
    }

    fun clear() {
        cachedKey = null
        cached = null
    }
}
