package hh.game.mgba_android.tracker.persistence

import com.google.gson.annotations.SerializedName

data class RunData(
    @SerializedName("romCode")         val romCode: String = "",
    @SerializedName("startTimestamp")  val startTimestamp: Long = System.currentTimeMillis(),
    @SerializedName("encounterLog")    val encounterLog: MutableList<EncounterEntry> = mutableListOf(),
    @SerializedName("trainerLog")      val trainerLog: MutableList<TrainerEntry> = mutableListOf(),
    @SerializedName("pokemonNotes")    val pokemonNotes: MutableMap<Int, String> = mutableMapOf(),
    @SerializedName("routeLog")        val routeLog: MutableList<String> = mutableListOf(),
    @SerializedName("stats")           val stats: RunStats = RunStats(),
    @SerializedName("routeEncounters") val routeEncounters: MutableMap<String, MutableList<Int>> = mutableMapOf(),
    @SerializedName("visitedRoutes")   val visitedRoutes: MutableList<Int> = mutableListOf(),
)

data class RunStats(
    @SerializedName("attempts")        var attempts: Int = 0,
    @SerializedName("centerVisits")    var centerVisits: Long = 0L,
    @SerializedName("trainerBattles")  var trainerBattles: Long = 0L,
    @SerializedName("wildEncounters")  var wildEncounters: Long = 0L,
    @SerializedName("steps")          var steps: Long = 0L,
    @SerializedName("playTimeMs")     var playTimeMs: Long = 0L,
)

data class EncounterEntry(
    @SerializedName("speciesId")   val speciesId: Int,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("level")       val level: Int,
    @SerializedName("location")    val location: String,
    @SerializedName("isWild")      val isWild: Boolean,
    @SerializedName("timestamp")   val timestamp: Long = System.currentTimeMillis(),
)

data class TrainerEntry(
    @SerializedName("trainerName")  val trainerName: String,
    @SerializedName("location")    val location: String,
    @SerializedName("won")         val won: Boolean,
    @SerializedName("timestamp")   val timestamp: Long = System.currentTimeMillis(),
)
