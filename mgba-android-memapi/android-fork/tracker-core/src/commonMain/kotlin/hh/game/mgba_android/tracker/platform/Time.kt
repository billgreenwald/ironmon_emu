package hh.game.mgba_android.tracker.platform

import kotlinx.datetime.Clock

/** Common wall-clock epoch millis, replacing JVM-only System.currentTimeMillis(). */
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
