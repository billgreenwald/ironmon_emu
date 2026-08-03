package hh.game.mgba_android.tracker.platform

import hh.game.mgba_android.tracker.models.TrackerState

/**
 * Swift-friendly discriminators for the sealed `TrackerState`. Kotlin does the `as?` cast so the
 * SwiftUI layer never has to reference the (interop-mangled) sealed-subclass type names — it just
 * binds the inferred result:  `if let active = TrackerStateSwift.shared.active(state: s) { … }`.
 */
object TrackerStateSwift {
    fun active(state: TrackerState): TrackerState.Active? = state as? TrackerState.Active
    fun isNoGameLoaded(state: TrackerState): Boolean = state is TrackerState.NoGameLoaded
}
