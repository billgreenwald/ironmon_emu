package hh.game.mgba_android.tracker.persistence

import android.content.Context
import com.google.gson.Gson
import java.io.File

object RunRepository {

    private val gson = Gson()

    private fun runFile(context: Context, profileId: String): File =
        File(context.filesDir, "ironmon_run_$profileId.json")

    fun load(context: Context, profileId: String): RunData {
        val file = runFile(context, profileId)
        if (!file.exists()) return RunData()
        return try {
            gson.fromJson(file.readText(), RunData::class.java) ?: RunData()
        } catch (e: Exception) {
            RunData()
        }
    }

    fun save(context: Context, profileId: String, data: RunData) {
        try {
            runFile(context, profileId).writeText(gson.toJson(data))
        } catch (_: Exception) { }
    }

    fun delete(context: Context, profileId: String) {
        runFile(context, profileId).delete()
    }

    fun romCodeMatches(data: RunData, currentCode: String): Boolean =
        data.romCode.isEmpty() || data.romCode == currentCode
}
