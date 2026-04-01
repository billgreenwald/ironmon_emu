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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import android.content.pm.PackageManager
import hh.game.mgba_android.tracker.quickload.FamilyMode
import hh.game.mgba_android.tracker.quickload.QuickloadManager
import hh.game.mgba_android.tracker.quickload.RomFamilyGroup
import hh.game.mgba_android.tracker.quickload.RomFamilyUtils
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Divider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Dialog
import hh.game.mgba_android.tracker.data.GachaMonRuleset
import hh.game.mgba_android.utils.BindableAction
import hh.game.mgba_android.utils.EmulatorPreferences
import hh.game.mgba_android.utils.GbaButton
import hh.game.mgba_android.utils.getKeyDisplayName
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope


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

        // Update check: load cached result from prefs (shown immediately if valid)
        @Suppress("DEPRECATION")
        val appVersionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        val updatePrefs = getSharedPreferences("mGBA", Context.MODE_PRIVATE)
        val cachedTag = updatePrefs.getString("update_tag", null)
        var latestUpdateTag: String? = cachedTag?.takeIf { it != "v$appVersionName" }
        var latestUpdateUrl: String? = if (latestUpdateTag != null) updatePrefs.getString("update_url", null) else null

        fun render() {
            setContent {
                Mgba_AndroidTheme {
                    val ctx = this@GameListMaterialActivity
                    var showSpeedSettings by remember { mutableStateOf(false) }
                    var isMuted by remember { mutableStateOf(EmulatorPreferences.getMuted(ctx)) }
                    Column {
                        FamilyTopBar(
                            isScanning = latestScanning,
                            onRescan = { viewModel.rescanFamilies(this@GameListMaterialActivity, documentfile) },
                            onFolderPick = {
                                setupStorageFolder()
                                storageHelper.openFolderPicker()
                            },
                            onSettings = { showSpeedSettings = true },
                            isMuted = isMuted,
                            onMuteToggle = {
                                isMuted = !isMuted
                                EmulatorPreferences.setMuted(ctx, isMuted)
                            },
                        )
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (latestFamilies.isEmpty() && !latestScanning) {
                                    EmptyFamilyHint()
                                } else {
                                    Box(modifier = Modifier.weight(1f)) {
                                        FamilyList(latestFamilies)
                                    }
                                }
                                val ctx2 = this@GameListMaterialActivity
                                val randomizerInstalled = remember {
                                    try {
                                        ctx2.packageManager.getPackageInfo("ly.mens.rndpkmn", 0)
                                        true
                                    } catch (_: PackageManager.NameNotFoundException) { false }
                                }
                                Text(
                                    text = if (randomizerInstalled) "✓ Randomizer installed" else "✗ Randomizer not installed — only premade ROMs supported",
                                    color = if (randomizerInstalled) Color(0xFF44BB44) else Color(0xFF888888),
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 2.dp),
                                )
                                TextButton(
                                    onClick = { exportLogs(ctx2) },
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
                                ) { Text("⬇ Export Debug Logs", color = Color(0xFF888888), fontSize = 12.sp) }
                                if (latestUpdateTag != null && latestUpdateUrl != null) {
                                    TextButton(
                                        onClick = {
                                            ctx2.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestUpdateUrl)))
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 2.dp),
                                    ) { Text("⬆ Update available: $latestUpdateTag — tap to download", color = Color(0xFF44BB44), fontSize = 12.sp) }
                                }
                                Text(
                                    text = "v$appVersionName",
                                    color = Color(0xFF555555),
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp),
                                )
                            }
                        }
                    }
                    if (showSpeedSettings) {
                        SpeedSettingsDialog(onDismiss = { showSpeedSettings = false })
                    }
                }
            }
        }

        // Fire-and-forget GitHub release check on every open
        lifecycleScope.launch {
            try {
                val (tag, url) = withContext(Dispatchers.IO) {
                    val conn = java.net.URL(
                        "https://api.github.com/repos/billgreenwald/ironmon_emu/releases/latest"
                    ).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    val body = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val t = Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                    val u = t?.let { "https://github.com/billgreenwald/ironmon_emu/releases/tag/$it" }
                    Pair(t, u)
                }
                if (tag != null && tag != "v$appVersionName") {
                    latestUpdateTag = tag
                    latestUpdateUrl = url
                    updatePrefs.edit().putString("update_tag", tag).putString("update_url", url).apply()
                } else {
                    latestUpdateTag = null
                    latestUpdateUrl = null
                    updatePrefs.edit().remove("update_tag").remove("update_url").apply()
                }
                render()
            } catch (_: Exception) { /* no network — keep cached state, don't re-render */ }
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

fun exportLogs(context: Context) {
    try {
        val logLines = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time"))
            .inputStream.bufferedReader().readLines()
        val outDir = context.getExternalFilesDir("logs") ?: return
        outDir.mkdirs()
        val file = java.io.File(outDir, "mgba_log_${System.currentTimeMillis()}.txt")
        file.writeText(logLines.joinToString("\n"))
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share logs"))
    } catch (e: Exception) {
        android.util.Log.e("exportLogs", "Failed to export logs", e)
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
fun FamilyTopBar(
    isScanning: Boolean = false,
    onRescan: () -> Unit = {},
    onFolderPick: () -> Unit = {},
    onSettings: () -> Unit = {},
    isMuted: Boolean = false,
    onMuteToggle: () -> Unit = {},
) {
    TopAppBar(
        title = { Text(LocalContext.current.getString(R.string.app_name), color = Color.White) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A2540)),
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
            IconButton(onClick = onFolderPick) {
                Text("📁", fontSize = 16.sp)
            }
            IconButton(onClick = onSettings) {
                Text("⚙", fontSize = 18.sp, color = Color.White)
            }
            IconButton(onClick = onMuteToggle) {
                Text(if (isMuted) "🔇" else "🔊", fontSize = 18.sp)
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
    var settingsVersion by remember { mutableStateOf(0) }

    LazyColumn {
        items(families) { group ->
            FamilyRow(
                group = group,
                settingsVersion = settingsVersion,
                onClick = { launchFamily(group, context) },
                onLongClick = { settingsGroup = group },
            )
        }
    }

    settingsGroup?.let { group ->
        FamilySettingsDialog(
            group = group,
            onDismiss = { settingsGroup = null },
            onSettingsChanged = { settingsVersion++ },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FamilyRow(group: RomFamilyGroup, settingsVersion: Int = 0, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val context = LocalContext.current
    val mode = remember(group.prefix, settingsVersion) { QuickloadManager.getFamilyMode(context, group.prefix) }
    val modeColor = if (mode == FamilyMode.UPR) Color(0xFFCC6600) else Color(0xFF4090FF)
    val modeLabel = if (mode == FamilyMode.UPR) "UPR" else "BATCH"
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
                    text = "ROM family · ${group.totalCount} ROM${if (group.totalCount == 1) "" else "s"}",
                    color = Color(0xFFAAAAAA),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (mode == FamilyMode.UPR) {
                    Text(
                        text = "UPR · re-randomized each run",
                        color = Color(0xFFFFAA44),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = "Last: Run ${group.lastPlayedNumber}",
                        color = Color(0xFF4090FF),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = group.extension.uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Green, shape = CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text(
                    text = modeLabel,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(modeColor, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SpeedSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var defaultFps by remember { mutableStateOf(EmulatorPreferences.getDefaultFps(context)) }
    var secondaryFps by remember { mutableStateOf(EmulatorPreferences.getSecondaryFps(context)) }
    var showFps by remember { mutableStateOf(EmulatorPreferences.getShowFps(context)) }
    var splitFraction by remember { mutableStateOf(EmulatorPreferences.getSplitFraction(context)) }
    var alwaysShowControls by remember { mutableStateOf(EmulatorPreferences.getAlwaysShowControls(context)) }
    var hideOnScreenControls by remember { mutableStateOf(EmulatorPreferences.getHideOnScreenControls(context)) }
    var trackerCollapsible by remember { mutableStateOf(EmulatorPreferences.getTrackerCollapsible(context)) }
    var hideCollapseButton by remember { mutableStateOf(EmulatorPreferences.getHideCollapseButton(context)) }
    var controlsAlpha by remember { mutableStateOf(EmulatorPreferences.getControlsAlpha(context)) }
    var controlsScale by remember { mutableStateOf(EmulatorPreferences.getControlsScale(context)) }
    var selectedRuleset by remember { mutableStateOf(EmulatorPreferences.getRuleset(context)) }
    val labelColor = Color(0xFF111111)
    val unselectedColor = Color(0xFF444444)
    val selectedColor = Color(0xFF4090FF)

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = { Text("Emulator Settings", color = labelColor) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Default Speed", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EmulatorPreferences.speedOptions.forEach { (mult, fps) ->
                        val selected = defaultFps == fps
                        TextButton(
                            onClick = { defaultFps = fps },
                            border = if (selected) BorderStroke(1.dp, selectedColor) else null,
                        ) { Text("${mult}x", color = if (selected) selectedColor else unselectedColor) }
                    }
                }
                Text("Hold-Button Speed", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EmulatorPreferences.speedOptions.forEach { (mult, fps) ->
                        val selected = secondaryFps == fps
                        TextButton(
                            onClick = { secondaryFps = fps },
                            border = if (selected) BorderStroke(1.dp, selectedColor) else null,
                        ) { Text("${mult}x", color = if (selected) selectedColor else unselectedColor) }
                    }
                }
                // ── Keybindings ───────────────────────────────────────────────
                var showKeyBindings by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showKeyBindings = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, selectedColor),
                ) {
                    Text("Set Keybindings", color = selectedColor, fontSize = 13.sp)
                }
                if (showKeyBindings) {
                    KeyBindingsDialog(onDismiss = { showKeyBindings = false })
                }
                // ── Game / Tracker split ──────────────────────────────────────
                val splitFractions = listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f)
                val splitLabels = listOf(
                    "100% / 0% (Game Overlay)", "90% / 10%", "80% / 20%", "70% / 30%",
                    "60% / 40%", "50% / 50%", "40% / 60%", "30% / 70%", "20% / 80%", "10% / 90%",
                    "0% / 100% (Tracker Overlay)"
                )
                var splitExpanded by remember { mutableStateOf(false) }
                val currentSplitIdx = splitFractions.indexOfFirst { abs(it - splitFraction) < 0.01f }.let { if (it < 0) 3 else it }
                ExposedDropdownMenuBox(expanded = splitExpanded, onExpandedChange = { splitExpanded = it }) {
                    OutlinedTextField(
                        value = splitLabels[currentSplitIdx],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Game % / Tracker %", color = labelColor) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = splitExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = splitExpanded, onDismissRequest = { splitExpanded = false }) {
                        splitFractions.forEachIndexed { i, f ->
                            DropdownMenuItem(
                                text = { Text(splitLabels[i]) },
                                onClick = { splitFraction = f; splitExpanded = false }
                            )
                        }
                    }
                }
                // ── Rating Ruleset ────────────────────────────────────────────
                var rulesetExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = rulesetExpanded,
                    onExpandedChange = { rulesetExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedRuleset.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rating Ruleset", color = labelColor) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rulesetExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = rulesetExpanded,
                        onDismissRequest = { rulesetExpanded = false },
                    ) {
                        GachaMonRuleset.entries.forEach { ruleset ->
                            DropdownMenuItem(
                                text = { Text(ruleset.label) },
                                onClick = { selectedRuleset = ruleset; rulesetExpanded = false },
                            )
                        }
                    }
                }
                // ── Always show on-screen controls ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Always show on-screen controls", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor, modifier = Modifier.weight(1f))
                    Switch(checked = alwaysShowControls, onCheckedChange = { alwaysShowControls = it })
                }
                // ── Always hide on-screen controls (keyboard/gamepad users) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Always hide on-screen controls", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor)
                        Text("For keyboard or gamepad-only users", fontSize = 11.sp, color = Color(0xFF888888))
                    }
                    Switch(checked = hideOnScreenControls, onCheckedChange = { hideOnScreenControls = it })
                }
                // ── Collapsible tracker panel (forced on in overlay modes) ────
                val isOverlaySplit = splitFraction == 0.0f || splitFraction == 1.0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Collapsible tracker panel",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = if (isOverlaySplit) Color(0xFFAAAAAA) else labelColor,
                    )
                    Switch(
                        checked = if (isOverlaySplit) true else trackerCollapsible,
                        onCheckedChange = { if (!isOverlaySplit) trackerCollapsible = it },
                        enabled = !isOverlaySplit,
                    )
                }
                // ── Hide on-screen collapse button (only when collapsible is on) ──
                if (trackerCollapsible || isOverlaySplit) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Hide on-screen collapse button",
                                fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor,
                            )
                            Text(
                                "Bind \"Tracker Open/Close\" in Key Bindings to collapse/expand",
                                fontSize = 11.sp, color = Color(0xFF888888),
                            )
                        }
                        Switch(
                            checked = hideCollapseButton,
                            onCheckedChange = { hideCollapseButton = it },
                        )
                    }
                }
                // ── Controls Opacity ──────────────────────────────────────────
                Text(
                    "Controls Opacity: ${(controlsAlpha * 100).toInt()}%",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor,
                )
                Slider(
                    value = controlsAlpha,
                    onValueChange = { controlsAlpha = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                )
                // ── Controls Scale ────────────────────────────────────────────
                Text(
                    "Controls Scale: ${((controlsScale - 0.5f) * 100).toInt() + 50}%",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor,
                )
                Slider(
                    value = controlsScale,
                    onValueChange = { controlsScale = it },
                    valueRange = 0.5f..1.5f,
                    modifier = Modifier.fillMaxWidth(),
                )
                // ── Show FPS ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Show FPS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor)
                    Switch(checked = showFps, onCheckedChange = { showFps = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                EmulatorPreferences.save(
                    context, defaultFps, secondaryFps, "none", showFps,
                    splitFraction, alwaysShowControls, trackerCollapsible, hideCollapseButton,
                    hideOnScreenControls = hideOnScreenControls,
                )
                EmulatorPreferences.setControlsAlpha(context, controlsAlpha)
                EmulatorPreferences.setControlsScale(context, controlsScale)
                EmulatorPreferences.setRuleset(context, selectedRuleset)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyBindingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bindings = remember {
        mutableStateMapOf<BindableAction, Int>().also { map ->
            BindableAction.entries.forEach { a -> map[a] = EmulatorPreferences.getBinding(context, a) }
        }
    }
    val gbaBindings = remember {
        mutableStateMapOf<GbaButton, Int>().also { map ->
            GbaButton.entries.forEach { b -> map[b] = EmulatorPreferences.getGbaKeyBinding(context, b) }
        }
    }
    var capturingAction by remember { mutableStateOf<BindableAction?>(null) }
    var capturingGba by remember { mutableStateOf<GbaButton?>(null) }
    val focusRequester = remember { FocusRequester() }

    val labelColor  = Color(0xFF111111)
    val accentColor = Color(0xFF4090FF)
    val mutedColor  = Color(0xFF888888)
    val sectionColor = Color(0xFF555555)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .onKeyEvent { keyEvent ->
                    val capA = capturingAction
                    val capG = capturingGba
                    if (capA == null && capG == null) return@onKeyEvent false
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val kc = keyEvent.key.nativeKeyCode
                        when {
                            kc == KeyEvent.KEYCODE_BACK -> { capturingAction = null; capturingGba = null }
                            kc != KeyEvent.KEYCODE_VOLUME_UP &&
                            kc != KeyEvent.KEYCODE_VOLUME_DOWN &&
                            kc != KeyEvent.KEYCODE_VOLUME_MUTE -> {
                                capA?.let { bindings[it] = kc; capturingAction = null }
                                capG?.let { gbaBindings[it] = kc; capturingGba = null }
                            }
                        }
                        true
                    } else false
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
                Column(modifier = Modifier.width(320.dp)) {
                    Text(
                        "Button Bindings",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = labelColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Divider()
                    val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
                    Column(
                        modifier = Modifier
                            .heightIn(max = maxListHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // ── Actions ───────────────────────────────────────────
                        Text("Actions", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionColor,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
                        BindableAction.entries.forEach { action ->
                            val keyCode = bindings[action] ?: -1
                            val isCapturing = capturingAction == action
                            BindingRow(
                                label = action.label,
                                keyCode = keyCode,
                                isCapturing = isCapturing,
                                labelColor = labelColor, accentColor = accentColor, mutedColor = mutedColor,
                                onStartCapture = { capturingAction = action; capturingGba = null },
                                onClear = { bindings[action] = -1 },
                            )
                        }
                        // ── GBA Controls ──────────────────────────────────────
                        Text("GBA Controls", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = sectionColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        GbaButton.entries.forEach { btn ->
                            val keyCode = gbaBindings[btn] ?: btn.nativeKeyCode
                            val isCapturing = capturingGba == btn
                            BindingRow(
                                label = btn.label,
                                keyCode = keyCode,
                                isCapturing = isCapturing,
                                labelColor = labelColor, accentColor = accentColor, mutedColor = mutedColor,
                                onStartCapture = { capturingGba = btn; capturingAction = null },
                                onClear = { gbaBindings[btn] = btn.nativeKeyCode },
                            )
                        }
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        TextButton(onClick = {
                            bindings.forEach { (action, kc) -> EmulatorPreferences.setBinding(context, action, kc) }
                            gbaBindings.forEach { (btn, kc) -> EmulatorPreferences.setGbaKeyBinding(context, btn, kc) }
                            onDismiss()
                        }) { Text("Save") }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun BindingRow(
    label: String,
    keyCode: Int,
    isCapturing: Boolean,
    labelColor: Color,
    accentColor: Color,
    mutedColor: Color,
    onStartCapture: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = labelColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
        when {
            isCapturing -> {
                Text("Press a button...", color = accentColor, fontSize = 12.sp)
            }
            keyCode != -1 -> {
                TextButton(
                    onClick = onStartCapture,
                    border = BorderStroke(1.dp, accentColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(getKeyDisplayName(keyCode), color = accentColor, fontSize = 11.sp)
                }
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Text("✕", color = mutedColor, fontSize = 12.sp)
                }
            }
            else -> {
                TextButton(
                    onClick = onStartCapture,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("Tap to bind", color = mutedColor, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun FamilySettingsDialog(group: RomFamilyGroup, onDismiss: () -> Unit, onSettingsChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val selectedColor = Color(0xFF4090FF)
    val unselectedColor = Color(0xFF888888)

    val uprInstalled = remember {
        try { context.packageManager.getPackageInfo("ly.mens.rndpkmn", 0); true }
        catch (_: PackageManager.NameNotFoundException) { false }
    }

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

    var selectedMode by remember { mutableStateOf(QuickloadManager.getFamilyMode(context, group.prefix)) }
    var romNumberText by remember { mutableStateOf(currentRomNum.toString()) }
    var totalRunsText by remember { mutableStateOf(currentRuns.toString()) }

    val saveBlocked = selectedMode == FamilyMode.UPR && !uprInstalled

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group.prefix.replaceFirstChar { it.uppercase() } + " Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ── Family Mode ───────────────────────────────────────────────
                Text("Family Mode", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FamilyMode.entries.forEach { mode ->
                        val selected = selectedMode == mode
                        TextButton(
                            onClick = { selectedMode = mode },
                            border = if (selected) BorderStroke(1.dp, selectedColor) else BorderStroke(1.dp, Color(0xFFDDDDDD)),
                        ) {
                            Text(
                                text = when (mode) {
                                    FamilyMode.BATCH -> "Batch"
                                    FamilyMode.UPR   -> "UPR"
                                },
                                color = if (selected) selectedColor else unselectedColor,
                            )
                        }
                    }
                }
                // ── UPR not installed warning ─────────────────────────────────
                if (selectedMode == FamilyMode.UPR && !uprInstalled) {
                    Text(
                        text = "✗ UPR-Android not installed — cannot use UPR mode",
                        color = Color(0xFFCC4444),
                        fontSize = 11.sp,
                    )
                }
                // ── ROM number — only relevant in BATCH mode ──────────────────
                if (selectedMode == FamilyMode.BATCH) {
                    Text("ROM in sequence (current: $currentRomNum)", fontSize = 12.sp)
                    OutlinedTextField(
                        value = romNumberText,
                        onValueChange = { romNumberText = it.filter { c -> c.isDigit() } },
                        label = { Text("ROM number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                // ── Run count ─────────────────────────────────────────────────
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
            TextButton(
                enabled = !saveBlocked,
                onClick = {
                    QuickloadManager.setFamilyMode(context, group.prefix, selectedMode)
                    if (selectedMode == FamilyMode.BATCH) {
                        romNumberText.toIntOrNull()?.let { n ->
                            QuickloadManager.setCurrentNumber(context, n)
                            // Also update SharedPreferences directly for families not currently loaded
                            context.getSharedPreferences("mGBA", Context.MODE_PRIVATE)
                                .edit().putInt("family_last_${group.prefix}", n).apply()
                        }
                    }
                    totalRunsText.toIntOrNull()?.let { n ->
                        TrackerPoller.setRunAttempts(n)
                        if (gameCode.isNotEmpty()) {
                            val data = RunRepository.load(context, gameCode)
                            data.stats.attempts = n
                            RunRepository.save(context, gameCode, data)
                        }
                    }
                    onSettingsChanged()
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
