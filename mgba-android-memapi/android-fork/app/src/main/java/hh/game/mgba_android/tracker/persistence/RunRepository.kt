package hh.game.mgba_android.tracker.persistence

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object RunRepository {

    private val gson = Gson()
    private const val TAG = "RunRepository"

    private fun runFile(context: Context, profileId: String): File =
        File(context.filesDir, "ironmon_run_$profileId.json")

    fun load(context: Context, profileId: String): RunData {
        val file = runFile(context, profileId)
        Log.d(TAG, "load profileId=$profileId file=${file.absolutePath} exists=${file.exists()}")
        if (!file.exists()) return RunData()
        return try {
            val text = file.readText()
            Log.d(TAG, "load raw JSON=$text")
            val data = gson.fromJson(text, RunData::class.java) ?: RunData()
            Log.d(TAG, "load routeEncounters=${data.routeEncounters}")
            data
        } catch (e: Exception) {
            Log.e(TAG, "load failed: $e")
            RunData()
        }
    }

    fun save(context: Context, profileId: String, data: RunData) {
        try {
            val file = runFile(context, profileId)
            val json = gson.toJson(data)
            file.writeText(json)
            Log.d(TAG, "save profileId=$profileId routeEncounters=${data.routeEncounters} file=${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "save failed: $e")
        }
    }

    fun delete(context: Context, profileId: String) {
        runFile(context, profileId).delete()
    }

    fun romCodeMatches(data: RunData, currentCode: String): Boolean =
        data.romCode.isEmpty() || data.romCode == currentCode
}
