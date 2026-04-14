package hh.game.mgba_android.tracker.ds.tables

/**
 * Base stats for Gen IV Pokemon (species ID 1-493).
 *
 * Unlike Gen III, Gen IV base stats are NOT read from ROM (DS ROMs use a different
 * file format). This table is populated from static data.
 *
 * TODO: Populate all 493 entries from a verified source (e.g. Bulbapedia, PokeAPI).
 *       Currently only a representative sample is included to get the tracker compiling.
 *       Missing entries return null from [get], which the decoder handles gracefully.
 *
 * Type IDs match Gen III/IV ROM encoding (see GenIVTypeChart).
 * Ability IDs match the Gen IV ability ID space.
 */
data class GenIVBaseStatEntry(
    val hp: Int, val atk: Int, val def: Int,
    val spa: Int, val spd: Int, val spe: Int,
    val type1: Int, val type2: Int,
    val ability1: Int, val ability2: Int,
)

object GenIVBaseStats {
    // Sparse map: species ID → base stat entry
    // Populated incrementally; missing = null (tracker shows type 0/0, ability 0)
    private val DATA: Map<Int, GenIVBaseStatEntry> = mapOf(
        // Gen I starters
        1  to GenIVBaseStatEntry(45, 49, 49, 65, 65, 45,  12,  3,  65, 0),   // Bulbasaur  Grass/Poison, Overgrow
        2  to GenIVBaseStatEntry(60, 62, 63, 80, 80, 60,  12,  3,  65, 0),   // Ivysaur
        3  to GenIVBaseStatEntry(80, 82, 83,100,100, 80,  12,  3,  65, 0),   // Venusaur
        4  to GenIVBaseStatEntry(39, 52, 43, 60, 50, 65,  10,  10, 66, 0),   // Charmander Fire
        5  to GenIVBaseStatEntry(58, 64, 58, 80, 65, 80,  10,  10, 66, 0),   // Charmeleon
        6  to GenIVBaseStatEntry(78, 84, 78,109, 85,100,  10,  2,  66, 0),   // Charizard  Fire/Flying
        7  to GenIVBaseStatEntry(44, 48, 65, 50, 64, 43,  11,  11, 67, 0),   // Squirtle   Water
        8  to GenIVBaseStatEntry(59, 63, 80, 65, 80, 58,  11,  11, 67, 0),   // Wartortle
        9  to GenIVBaseStatEntry(79, 83,100, 85,105, 78,  11,  11, 67, 0),   // Blastoise
        25 to GenIVBaseStatEntry(35, 55, 40, 50, 50, 90,  13,  13, 9,  0),   // Pikachu    Electric, Static
        // Sinnoh starters
        387 to GenIVBaseStatEntry(55, 68, 64, 45, 55, 31, 12, 12, 65, 0),   // Turtwig    Grass
        388 to GenIVBaseStatEntry(75, 89, 85, 55, 65, 36, 12, 12, 65, 0),   // Grotle
        389 to GenIVBaseStatEntry(95,109,105, 75, 85, 56, 12, 4,  65, 0),   // Torterra   Grass/Ground
        390 to GenIVBaseStatEntry(44, 58, 44, 58, 44, 61, 10, 10, 66, 0),   // Chimchar   Fire
        391 to GenIVBaseStatEntry(64, 78, 52, 78, 52, 81, 10,  1, 66, 0),   // Monferno   Fire/Fighting
        392 to GenIVBaseStatEntry(76,104, 71,104, 71,108, 10,  1, 66, 0),   // Infernape  Fire/Fighting
        393 to GenIVBaseStatEntry(53, 51, 53, 61, 56, 40, 11, 11, 67, 0),   // Piplup     Water
        394 to GenIVBaseStatEntry(64, 66, 68, 81, 76, 50, 11, 11, 67, 0),   // Prinplup
        395 to GenIVBaseStatEntry(84, 86, 88,111,101, 60, 11,  8, 67, 0),   // Empoleon   Water/Steel
        // Legendaries
        483 to GenIVBaseStatEntry(100,120,120,150,100, 90, 16,  8, 0,  0),  // Dialga     Dragon/Steel
        484 to GenIVBaseStatEntry(90,120,100,150,120, 80, 11, 16, 0,  0),   // Palkia     Water/Dragon
        487 to GenIVBaseStatEntry(150,100,120,100,120, 90, 7, 16, 0,  0),   // Giratina   Ghost/Dragon
        493 to GenIVBaseStatEntry(120,120,120,120,120,120,  0,  0, 0,  0),  // Arceus     Normal (base form)
    )

    fun get(speciesId: Int): GenIVBaseStatEntry? = DATA[speciesId]
}
