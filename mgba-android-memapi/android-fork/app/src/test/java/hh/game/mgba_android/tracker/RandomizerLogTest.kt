package hh.game.mgba_android.tracker

import hh.game.mgba_android.tracker.data.RandomizerLog
import hh.game.mgba_android.tracker.models.GameVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the RandomizerLog parser against a synthetic FRLG-format log fixture.
 * Exercises every sector: settings, base stats, evolutions, moves, movesets
 * (preamble skip + level moves), TM moves, TM compatibility, trainers (party +
 * held item + reconstructed moves), and wild routes (grass + surfing + rates).
 */
class RandomizerLogTest {

    private val fixture = """
        Randomizer Version: 4.6.1
        Random Seed: 123456789
        Settings String: ABC-DEF-GHI

        Pokemon Base Stats & Types
        NUM|NAME|TYPE|HP|ATK|DEF|SPATK|SPDEF|SPEED|ABILITY 1|ABILITY 2|ITEM
          1|Bulbasaur|GRASS/POISON|45|49|49|65|65|45|Overgrow|Chlorophyll|
          2|Ivysaur|GRASS/POISON|60|62|63|80|80|60|Overgrow|Chlorophyll|
          4|Charmander|FIRE|39|52|43|60|50|65|Blaze||
          5|Charmeleon|FIRE|58|64|58|80|65|80|Blaze||
         16|Pidgey|NORMAL/FLYING|40|45|40|35|35|56|Keen Eye||

        Randomized Evolutions
        Bulbasaur -> Ivysaur
        Charmander -> Charmeleon

        Move Data
        NUM|NAME|TYPE|POWER|ACC|PP
          1|Pound|NORMAL|40|100|35
         33|Tackle|NORMAL|35|95|35
         45|Growl|NORMAL|0|100|40

        Pokemon Movesets
        1 Bulbasaur ->
        Tackle
        Growl
        Level 1 : Tackle
        Level 3 : Growl
        4 Charmander ->
        Scratch
        Level 1 : Pound

        TM Moves
        TM01 Tackle
        TM02 Growl

        TM Compatibility
          1 Bulbasaur|TM01
          4 Charmander|TM01 TM02

        Trainers Pokemon
        #1 (YOUNGSTER Ben) - Pidgey @ Oran Berry Lv5, Pidgey Lv7
        #2 (LASS Amy) - Bulbasaur Lv10

        Wild Pokemon
        Set #147 - Grass/Cave (rate = 25)
        Pidgey Lvs 2-4
        Bulbasaur Lvs 3-3
        Set #199 - Surfing (rate = 4)
        Charmander Lv 5
    """.trimIndent().lines()

    private fun parse() = RandomizerLog(GameVersion.FIRE_RED).parse(fixture)!!

    @Test fun parsesSettings() {
        val d = parse()
        assertEquals("4.6.1", d.settings.version)
        assertEquals("123456789", d.settings.randomSeed)
        assertEquals("ABC-DEF-GHI", d.settings.settingsString)
    }

    @Test fun parsesBaseStatsAndTypes() {
        val d = parse()
        val bulba = d.pokemon[1]!!
        assertEquals("Bulbasaur", bulba.name)
        assertEquals(45, bulba.baseStats.hp)
        assertEquals(45, bulba.baseStats.spe)
        assertEquals(318, bulba.bst)
        // GRASS=12, POISON=3
        assertEquals(12, bulba.type1)
        assertEquals(3, bulba.type2)
        // Charmander is mono-type FIRE=10 with a single ability
        val char = d.pokemon[4]!!
        assertEquals(10, char.type1)
        assertEquals(null, char.type2)
        assertEquals(null, char.ability2)
        assertTrue(char.ability1 > 0)
    }

    @Test fun parsesEvolutionsAndPreEvos() {
        val d = parse()
        // Bulbasaur (1) -> Ivysaur (2)
        assertTrue(d.pokemon[1]!!.evolutions.contains(2))
        assertTrue(d.pokemon[2]!!.preEvolutions.contains(1))
    }

    @Test fun parsesMoves() {
        val d = parse()
        assertEquals("Tackle", d.moves[33]!!.name)
        assertEquals(35, d.moves[33]!!.power)
        assertEquals(95, d.moves[33]!!.acc)
    }

    @Test fun parsesMovesetsSkippingPreamble() {
        val d = parse()
        val bulbaMoves = d.pokemon[1]!!.moveSet
        assertEquals(2, bulbaMoves.size)
        assertEquals(1, bulbaMoves[0].level)
        assertEquals("Tackle", bulbaMoves[0].name)
        assertEquals(3, bulbaMoves[1].level)
        // Charmander has one level-up move
        assertEquals(1, d.pokemon[4]!!.moveSet.size)
    }

    @Test fun parsesTMs() {
        val d = parse()
        assertEquals("Tackle", d.tms[1]!!.name)
        assertTrue(d.pokemon[4]!!.tmMoves.containsAll(listOf(1, 2)))
        assertEquals(listOf(1), d.pokemon[1]!!.tmMoves)
    }

    @Test fun parsesTrainersWithPartyItemAndMoves() {
        val d = parse()
        val ben = d.trainers[1]!!
        assertEquals("ben", ben.name)
        assertEquals("youngster", ben.trainerClass)
        assertEquals(2, ben.party.size)
        assertEquals(5, ben.minLevel)
        assertEquals(7, ben.maxLevel)
        assertEquals("oran berry", ben.party[0].heldItem)
        assertEquals(16, ben.party[0].pokemonId) // Pidgey
    }

    @Test fun parsesWildRoutesWithRates() {
        val d = parse()
        // Set #147 -> mapId 89 (Route 1), grass/cave
        val route = d.routes[89]!!
        assertNotNull(route.encounterAreas["GrassCave"])
        val grass = route.encounterAreas["GrassCave"]!!
        assertTrue(grass.pokemon.containsKey(16)) // Pidgey
        assertTrue(grass.pokemon.containsKey(1))  // Bulbasaur
        assertEquals(2, grass.pokemon[16]!!.levelMin)
        assertEquals(4, grass.pokemon[16]!!.levelMax)
        assertEquals(0.20, grass.pokemon[16]!!.rate, 0.0001) // first slot
        // Set #199 -> mapId 78 (Pallet Town), surfing
        val surf = d.routes[78]!!.encounterAreas["Surfing"]!!
        assertTrue(surf.pokemon.containsKey(4)) // Charmander
    }
}
