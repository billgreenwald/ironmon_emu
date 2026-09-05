package hh.game.mgba_android.tracker.tables

/**
 * NatDex (CyanSMP64/NatDexExtension) repurposes a handful of Gen III item IDs for the evolution
 * items it adds, so the vanilla [ItemTable] name for these IDs is wrong (or missing → "???").
 * Values mirror `NatDexExtension.lua` `self.Data.natDexEvoStones` (v1.2.1). Consult this first when
 * the running ROM is NatDex; fall back to [ItemTable] for everything else.
 */
object NatDexItemNames {
    private val names: Map<Int, String> = mapOf(
        89  to "Dubious Disc",
        90  to "Razor Claw",
        91  to "Razor Fang",
        92  to "Linking Cord",
        99  to "Shiny Stone",
        100 to "Dusk Stone",
        101 to "Dawn Stone",
        102 to "Ice Stone",
        187 to "King's Rock",
        192 to "Deep Sea Tooth",
        193 to "Deep Sea Scale",
        199 to "Metal Coat",
        201 to "Dragon Scale",
        218 to "Up-Grade",
    )

    /** NatDex-specific item name for [itemId], or null if this ID isn't repurposed. */
    fun get(itemId: Int): String? = names[itemId]
}
