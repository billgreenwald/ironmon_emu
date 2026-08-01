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
