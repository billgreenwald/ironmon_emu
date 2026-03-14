package hh.game.mgba_android.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.getStorageId
import hh.game.mgba_android.GameListViewmodel
import hh.game.mgba_android.R
import hh.game.mgba_android.adapter.GameListAdapter
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgameData
import hh.game.mgba_android.utils.Gametype


class GameListActivity : AppCompatActivity() {
    private val viewModel: GameListViewmodel by viewModels<GameListViewmodel>()
    private val storageHelper = SimpleStorageHelper(this)
    private var sharepreferences: SharedPreferences? = null
    private var storageid: String? = null
    private var FOLDER_PATH: String = "folder_path"
    private var STORAGEID: String = "storageid"
    private lateinit var gameListAdapter: GameListAdapter
    private lateinit var gamelistview: RecyclerView
    private var gamelist: ArrayList<DocumentFile>? = null
    private var mgbaTitle: TextView?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_list)
        mgbaTitle = findViewById<TextView>(R.id.mgbaTitle)
        gamelistview = findViewById<RecyclerView>(R.id.gamelistview)
        gameListAdapter = GameListAdapter(this, ArrayList())
        sharepreferences = getSharedPreferences("mGBA", Context.MODE_PRIVATE)
        var permissionlist = contentResolver.persistedUriPermissions

        if (permissionlist.size > 0) {
            storageid = sharepreferences?.getString(STORAGEID, null)
            setupUI()
        } else {
            sharepreferences?.edit()?.putString(FOLDER_PATH, null)?.apply()
            storageHelper.openFolderPicker()
            setupStorageFolder()
        }
    }

    fun setupStorageFolder() {
        storageHelper.onFolderSelected = { requestCode, folder ->
            sharepreferences?.edit()?.putString(FOLDER_PATH, folder.uri.toString())?.apply()
            storageid = folder.getStorageId(this)
            sharepreferences?.edit()?.putString(STORAGEID, storageid)?.apply()
            setupUI()
        }
    }

    fun setupUI() {
        gamelistview.layoutManager = LinearLayoutManager(this)
        var uri = Uri.parse(sharepreferences?.getString(FOLDER_PATH, null))
        var documentfile = DocumentFile.fromTreeUri(this, uri)
        var coverfilefolder = documentfile?.findFile("gbacovers")
        viewModel.familyGroupData.observe(this, { _ -> })
        viewModel.loadFamilies(this, documentfile)
    }
}