package hh.game.mgba_android.tracker.data

/**
 * Global store for Gen4/Gen5 ability and move data loaded from Lua assets.
 * Set by TrackerPoller when a MaxFR variant is detected; cleared on game unload.
 * Read by AbilityTable, MoveNames, and MoveDescTable as a first-priority lookup.
 */
object MaxExtDataStore {
    @Volatile var abilityMap: Map<Int, MaxExtAbility> = emptyMap()
    @Volatile var moveMap:    Map<Int, MaxExtMove>    = emptyMap()

    fun clear() {
        abilityMap = emptyMap()
        moveMap    = emptyMap()
    }
}
