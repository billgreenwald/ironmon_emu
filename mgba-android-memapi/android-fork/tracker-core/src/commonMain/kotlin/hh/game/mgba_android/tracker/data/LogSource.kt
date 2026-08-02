package hh.game.mgba_android.tracker.data

/**
 * Locates and reads the randomizer `.log` spoiler file for a ROM. The Android implementation
 * uses the Storage Access Framework (stays in :app); an iOS implementation reads from the
 * document the user picked. Shared code depends only on this interface.
 */
interface LogSource {
    fun readLines(romFileName: String): List<String>?
    fun exists(romFileName: String): Boolean
}
