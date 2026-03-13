package hh.game.mgba_android.tracker.quickload

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object FamilyCache {

    private const val CACHE_FILE = "rom_family_cache.json"
    private val gson = Gson()

    fun save(context: Context, groups: List<RomFamilyGroup>) {
        try {
            val file = File(context.filesDir, CACHE_FILE)
            file.writeText(gson.toJson(groups))
        } catch (_: Exception) {}
    }

    fun load(context: Context): List<RomFamilyGroup> {
        return try {
            val file = File(context.filesDir, CACHE_FILE)
            if (!file.exists()) return emptyList()
            val type = object : TypeToken<List<RomFamilyGroup>>() {}.type
            gson.fromJson<List<RomFamilyGroup>>(file.readText(), type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun exists(context: Context): Boolean =
        File(context.filesDir, CACHE_FILE).exists()

    fun clear(context: Context) {
        File(context.filesDir, CACHE_FILE).delete()
    }
}
