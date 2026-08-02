package hh.game.mgba_android.tracker.persistence

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the Gson → kotlinx.serialization migration. The wire format must stay byte-compatible so
 * existing on-device ironmon_run_*.json files (written by the old Gson code) still deserialize.
 */
class SerializationTest {

    @Test
    fun runData_roundTrips() {
        val data = RunData(
            romCode = "BPRE",
            startTimestamp = 111L,
            encounterLog = mutableListOf(
                EncounterEntry(1, "Bulbasaur", 5, "Route 1", isWild = true, timestamp = 123L),
            ),
            trainerLog = mutableListOf(
                TrainerEntry("Youngster Joey", "Route 2", won = true, timestamp = 124L),
            ),
            pokemonNotes = mutableMapOf(1 to "starter"),
            stats = RunStats(attempts = 5, steps = 100, trainerBattles = 3),
            routeEncounters = mutableMapOf("12" to mutableListOf(1, 2)),
            visitedRoutes = mutableListOf(12),
            trainerDefeatsByRoute = mutableMapOf("12" to 3),
        )
        val json = trackerJson.encodeToString(RunData.serializer(), data)
        val back = trackerJson.decodeFromString(RunData.serializer(), json)
        assertEquals(data, back)
    }

    @Test
    fun readsLegacyGsonJson() {
        // Exactly what the previous Gson implementation wrote (same field names).
        val legacy = """
            {"romCode":"BPRE","startTimestamp":111,
             "encounterLog":[{"speciesId":1,"speciesName":"Bulbasaur","level":5,"location":"Route 1","isWild":true,"timestamp":123}],
             "trainerLog":[],
             "pokemonNotes":{"1":"note"},
             "routeLog":[],
             "stats":{"attempts":5,"centerVisits":0,"trainerBattles":0,"wildEncounters":0,"steps":100,"playTimeMs":0},
             "routeEncounters":{"12":[1,2]},
             "visitedRoutes":[12],
             "trainerDefeatsByRoute":{"12":3}}
        """.trimIndent()
        val data = trackerJson.decodeFromString(RunData.serializer(), legacy)
        assertEquals("BPRE", data.romCode)
        assertEquals(5, data.stats.attempts)
        assertEquals(100L, data.stats.steps)
        assertEquals(1, data.encounterLog.size)
        assertEquals("Bulbasaur", data.encounterLog[0].speciesName)
        assertEquals("note", data.pokemonNotes[1])
        assertEquals(mutableListOf(1, 2), data.routeEncounters["12"])
        assertEquals(3, data.trainerDefeatsByRoute["12"])
    }

    @Test
    fun toleratesMissingAndUnknownFields() {
        // A minimal old save missing later-added fields, plus a hypothetical future field.
        val partial = """{"romCode":"BPEE","futureField":{"nested":true}}"""
        val data = trackerJson.decodeFromString(RunData.serializer(), partial)
        assertEquals("BPEE", data.romCode)
        assertEquals(0, data.stats.attempts)          // defaulted
        assertEquals(0, data.encounterLog.size)       // defaulted
    }
}
