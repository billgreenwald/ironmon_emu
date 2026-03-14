package hh.game.mgba_android

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.file.getAbsolutePath
import hh.game.mgba_android.tracker.quickload.FamilyCache
import hh.game.mgba_android.tracker.quickload.QuickloadManager
import hh.game.mgba_android.tracker.quickload.RomFamilyGroup
import hh.game.mgba_android.tracker.quickload.RomFamilyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameListViewmodel : ViewModel() {
    val familyGroupData = MutableLiveData<List<RomFamilyGroup>>()
    val isScanning = MutableLiveData<Boolean>(false)

    /**
     * Fast startup: load cached families from disk (no filesystem scan).
     * If no cache exists, immediately scan filenames (no JNI/DB needed).
     */
    fun loadFamilies(context: Context, documentfile: DocumentFile?) {
        viewModelScope.launch {
            documentfile ?: return@launch
            val cached = withContext(Dispatchers.IO) { FamilyCache.load(context) }
            if (cached.isNotEmpty()) {
                familyGroupData.postValue(cached.map {
                    it.copy(lastPlayedNumber = QuickloadManager.getLastNumber(context, it.prefix))
                })
            } else {
                // No cache yet — run a quick filename scan (no JNI or DB)
                scanAndSave(context, documentfile)
            }
        }
    }

    /**
     * Full rescan triggered by the user (⟳ button).
     * Pure filename parsing — fast even with 700+ ROMs.
     */
    fun rescanFamilies(context: Context, documentfile: DocumentFile?) {
        if (isScanning.value == true) return
        viewModelScope.launch {
            isScanning.postValue(true)
            scanAndSave(context, documentfile)
            isScanning.postValue(false)
        }
    }

    private fun collectRomFiles(dir: DocumentFile, context: Context): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        for (child in dir.listFiles() ?: return result) {
            if (child.isDirectory) {
                result += collectRomFiles(child, context)
            } else {
                val name = child.name ?: continue
                if (!name.endsWith(".gba", ignoreCase = true) &&
                    !name.endsWith(".gb", ignoreCase = true)) continue
                val path = child.getAbsolutePath(context) ?: continue
                result += Pair(name, path)
            }
        }
        return result
    }

    private suspend fun scanAndSave(context: Context, documentfile: DocumentFile?) {
        documentfile ?: return
        val groups = withContext(Dispatchers.IO) {
            collectRomFiles(documentfile, context)
                .mapNotNull { (name, path) ->
                    RomFamilyUtils.parseFamily(name, path).takeIf { it.number != null }
                }
                .groupBy { Pair(it.prefix, it.extension) }
                .filter { (_, members) -> members.size >= 2 }
                .map { (key, members) ->
                    val sorted = members.sortedBy { it.number!! }
                    RomFamilyGroup(
                        prefix           = key.first,
                        extension        = key.second,
                        totalCount       = sorted.size,
                        lastPlayedNumber = QuickloadManager.getLastNumber(context, key.first),
                        allMemberPaths   = sorted.map { it.absolutePath },
                    )
                }
                .sortedBy { it.prefix }
        }
        FamilyCache.save(context, groups)
        familyGroupData.postValue(groups)
    }
}
