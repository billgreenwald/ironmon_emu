package hh.game.mgba_android.tracker.persistence

import hh.game.mgba_android.tracker.platform.nowMillis
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire format preserved from the previous Gson models: @SerialName values match the old
// @SerializedName values exactly, and every field is defaulted, so existing on-disk
// ironmon_run_*.json files deserialize unchanged.

@Serializable
data class RunData(
    @SerialName("romCode")              val romCode: String = "",
    @SerialName("startTimestamp")       val startTimestamp: Long = nowMillis(),
    @SerialName("encounterLog")         val encounterLog: MutableList<EncounterEntry> = mutableListOf(),
    @SerialName("trainerLog")           val trainerLog: MutableList<TrainerEntry> = mutableListOf(),
    @SerialName("pokemonNotes")         val pokemonNotes: MutableMap<Int, String> = mutableMapOf(),
    @SerialName("routeLog")             val routeLog: MutableList<String> = mutableListOf(),
    @SerialName("stats")                val stats: RunStats = RunStats(),
    @SerialName("routeEncounters")      val routeEncounters: MutableMap<String, MutableList<Int>> = mutableMapOf(),
    @SerialName("visitedRoutes")        val visitedRoutes: MutableList<Int> = mutableListOf(),
    @SerialName("trainerDefeatsByRoute") val trainerDefeatsByRoute: MutableMap<String, Int> = mutableMapOf(),
)

@Serializable
data class RunStats(
    @SerialName("attempts")        var attempts: Int = 0,
    @SerialName("centerVisits")    var centerVisits: Long = 0L,
    @SerialName("trainerBattles")  var trainerBattles: Long = 0L,
    @SerialName("wildEncounters")  var wildEncounters: Long = 0L,
    @SerialName("steps")          var steps: Long = 0L,
    @SerialName("playTimeMs")     var playTimeMs: Long = 0L,
)

@Serializable
data class EncounterEntry(
    @SerialName("speciesId")   val speciesId: Int,
    @SerialName("speciesName") val speciesName: String,
    @SerialName("level")       val level: Int,
    @SerialName("location")    val location: String,
    @SerialName("isWild")      val isWild: Boolean,
    @SerialName("timestamp")   val timestamp: Long = nowMillis(),
)

@Serializable
data class TrainerEntry(
    @SerialName("trainerName")  val trainerName: String,
    @SerialName("location")    val location: String,
    @SerialName("won")         val won: Boolean,
    @SerialName("timestamp")   val timestamp: Long = nowMillis(),
)
