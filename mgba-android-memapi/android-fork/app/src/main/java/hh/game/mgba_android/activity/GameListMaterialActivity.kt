package hh.game.mgba_android.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hh.game.mgba_android.tracker.TrackerPoller
import hh.game.mgba_android.tracker.persistence.RunRepository
import java.io.File
import java.io.RandomAccessFile
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.getStorageId
import hh.game.mgba_android.GameListViewmodel
import hh.game.mgba_android.R
import hh.game.mgba_android.activity.ui.theme.Mgba_AndroidTheme
import hh.game.mgba_android.tracker.quickload.QuickloadManager
import hh.game.mgba_android.tracker.quickload.RomFamilyGroup
import hh.game.mgba_android.tracker.quickload.RomFamilyUtils


class GameListMaterialActivity : ComponentActivity() {
    private val viewModel: GameListViewmodel by viewModels()
    private val storageHelper = SimpleStorageHelper(this)
    private var sharepreferences: SharedPreferences? = null
    private var storageid: String? = null
    private val FOLDER_PATH = "folder_path"
    private val STORAGEID   = "storageid"

    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _: ActivityResult ->
            checkPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startForResult.launch(Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                } catch (e: Exception) {
                    startForResult.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                sharepreferences = getSharedPreferences("mGBA", Context.MODE_PRIVATE)
                if (contentResolver.persistedUriPermissions.isNotEmpty()) {
                    storageid = sharepreferences?.getString(STORAGEID, null)
                    setupUI()
                } else {
                    sharepreferences?.edit()?.putString(FOLDER_PATH, null)?.apply()
                    storageHelper.openFolderPicker()
                    setupStorageFolder()
                }
            }
        }
    }

    fun setupStorageFolder() {
        storageHelper.onFolderSelected = { _, folder ->
            sharepreferences?.edit()?.putString(FOLDER_PATH, folder.uri.toString())?.apply()
            storageid = folder.getStorageId(this)
            sharepreferences?.edit()?.putString(STORAGEID, storageid)?.apply()
            setupUI()
        }
    }

    fun setupUI() {
        val uri = Uri.parse(sharepreferences?.getString(FOLDER_PATH, null))
        val documentfile = DocumentFile.fromTreeUri(this, uri)

        setContent { FamilyLoadingScreen() }

        var latestFamilies: List<RomFamilyGroup> = emptyList()
        var latestScanning = false

        fun render() {
            setContent {
                Mgba_AndroidTheme {
                    Column {
                        FamilyTopBar(
                            isScanning = latestScanning,
                            onRescan = { viewModel.rescanFamilies(this@GameListMaterialActivity, documentfile) }
                        )
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            if (latestFamilies.isEmpty() && !latestScanning) {
                                EmptyFamilyHint()
                            } else {
                                FamilyList(latestFamilies)
                            }
                        }
                    }
                }
            }
        }

        viewModel.familyGroupData.observe(this) { families ->
            latestFamilies = families
            render()
        }
        viewModel.isScanning.observe(this) { scanning ->
            latestScanning = scanning
            render()
        }
        viewModel.loadFamilies(this, documentfile)
    }
}

fun launchFamily(group: RomFamilyGroup, context: Context) {
    val lastNum = QuickloadManager.getLastNumber(context, group.prefix)
    val targetPath = group.allMemberPaths.find { path ->
        RomFamilyUtils.parseFamily(path.substringAfterLast('/'), path).number == lastNum
    } ?: group.allMemberPaths.firstOrNull() ?: return
    context.startActivity(
        Intent(context, GameActivity::class.java).apply {
            putExtra("gamepath", targetPath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun FamilyLoadingScreen() {
    Mgba_AndroidTheme {
        Column {
            FamilyTopBar()
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EmptyFamilyHint()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTopBar(isScanning: Boolean = false, onRescan: () -> Unit = {}) {
    TopAppBar(
        title = { Text(LocalContext.current.getString(R.string.app_name)) },
        actions = {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 4.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = onRescan) {
                    Text("⟳", fontSize = 18.sp, color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun EmptyFamilyHint() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No ROM families found.", color = Color(0xFFAAAAAA), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap ⟳ to scan your ROM folder.", color = Color(0xFF666666), fontSize = 12.sp)
    }
}

@Composable
fun FamilyList(families: List<RomFamilyGroup>) {
    val context = LocalContext.current
    var settingsGroup by remember { mutableStateOf<RomFamilyGroup?>(null) }

    LazyColumn {
        items(families) { group ->
            FamilyRow(
                group = group,
                onClick = { launchFamily(group, context) },
                onLongClick = { settingsGroup = group },
            )
        }
    }

    settingsGroup?.let { group ->
        FamilySettingsDialog(group = group, onDismiss = { settingsGroup = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FamilyRow(group: RomFamilyGroup, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Card(
        border = BorderStroke(1.dp, Color(0xFF4090FF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2540))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.prefix.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ROM family · ${group.totalCount} ROMs",
                    color = Color(0xFFAAAAAA),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Last: Run ${group.lastPlayedNumber}",
                    color = Color(0xFF4090FF),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = group.extension.uppercase(),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Green, shape = CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun FamilySettingsDialog(group: RomFamilyGroup, onDismiss: () -> Unit) {
    val context = LocalContext.current

    // Read game code from ROM file header (offset 0xAC, 4 bytes)
    val gameCode = remember(group) {
        try {
            val romFile = File(group.allMemberPaths.first())
            RandomAccessFile(romFile, "r").use { raf ->
                raf.seek(0xAC)
                val buf = ByteArray(4)
                raf.read(buf)
                String(buf, Charsets.US_ASCII)
            }
        } catch (_: Exception) { "" }
    }

    val currentRomNum = QuickloadManager.getLastNumber(context, group.prefix)
    val currentRuns = remember(gameCode) {
        if (gameCode.isEmpty()) 0
        else RunRepository.load(context, gameCode).stats.attempts
    }

    var romNumberText by remember { mutableStateOf(currentRomNum.toString()) }
    var totalRunsText by remember { mutableStateOf(currentRuns.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group.prefix.replaceFirstChar { it.uppercase() } + " Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ROM in sequence (current: $currentRomNum)", fontSize = 12.sp)
                OutlinedTextField(
                    value = romNumberText,
                    onValueChange = { romNumberText = it.filter { c -> c.isDigit() } },
                    label = { Text("ROM number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text("Total runs (current: $currentRuns)", fontSize = 12.sp)
                OutlinedTextField(
                    value = totalRunsText,
                    onValueChange = { totalRunsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Run count") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                romNumberText.toIntOrNull()?.let { n ->
                    QuickloadManager.setCurrentNumber(context, n)
                    // Also update the SharedPreferences entry directly for families not currently loaded
                    context.getSharedPreferences("mGBA", Context.MODE_PRIVATE)
                        .edit().putInt("family_last_${group.prefix}", n).apply()
                }
                totalRunsText.toIntOrNull()?.let { n ->
                    TrackerPoller.setRunAttempts(n)
                    if (gameCode.isNotEmpty()) {
                        val data = RunRepository.load(context, gameCode)
                        data.stats.attempts = n
                        RunRepository.save(context, gameCode, data)
                    }
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
