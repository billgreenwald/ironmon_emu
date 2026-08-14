package hh.game.mgba_android.tracker.platform

import kotlin.concurrent.Volatile

/**
 * Platform-agnostic logging seam. Shared code logs through [TrackerLog]; each platform installs
 * an implementation at startup (Android → android.util.Log, iOS → os_log). Defaults to no-op so
 * commonTest and any pre-init logging are silent rather than crashing.
 */
interface Logger {
    fun d(tag: String, msg: String)
    fun w(tag: String, msg: String)
    fun e(tag: String, msg: String, throwable: Throwable?)
}

object NoOpLogger : Logger {
    override fun d(tag: String, msg: String) {}
    override fun w(tag: String, msg: String) {}
    override fun e(tag: String, msg: String, throwable: Throwable?) {}
}

object TrackerLog {
    @Volatile
    var impl: Logger = NoOpLogger

    fun d(tag: String, msg: String) = impl.d(tag, msg)
    fun w(tag: String, msg: String) = impl.w(tag, msg)
    fun e(tag: String, msg: String, throwable: Throwable? = null) = impl.e(tag, msg, throwable)
}

/**
 * Emits a generic, greppable demarcation marker into the log stream. Triggered by the
 * "Add a Flag to Logs" tools-menu button so a user can flag a moment ("I'm here now") for a
 * developer to find in an exported log. Intentionally carries NO game/route context so it stays
 * reusable for any future debugging; correlate it with the surrounding ROUTE_CHANGE lines etc.
 */
fun logUserFlag() {
    TrackerLog.d("USER_FLAG", "===== USER LOG FLAG @ ${nowMillis()} =====")
}
