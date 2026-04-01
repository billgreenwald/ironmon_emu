package hh.game.mgba_android.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.util.TypedValue
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import hh.game.mgba_android.utils.EmulatorPreferences
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import android.widget.RelativeLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import hh.game.mgba_android.R
import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.TrackerPanel
import hh.game.mgba_android.tracker.TrackerPoller
import hh.game.mgba_android.tracker.quickload.QuickloadManager
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.fragment.MemorySearchFragment
import hh.game.mgba_android.fragment.OnAddressClickListener
import hh.game.mgba_android.fragment.OnDialogClickListener
import hh.game.mgba_android.fragment.OnMemSearchListener
import hh.game.mgba_android.fragment.PopDialogFragment
import hh.game.mgba_android.memory.CoreMemoryBlock
import hh.game.mgba_android.utils.CheatUtils
import hh.game.mgba_android.utils.BindableAction
import hh.game.mgba_android.utils.GbaButton
import hh.game.mgba_android.utils.GBAKeys
import hh.game.mgba_android.utils.Gametype
import hh.game.mgba_android.utils.controllerUtil.getDirectionPressed
import hh.game.mgba_android.utils.controllerUtil.lastDirect
import hh.game.mgba_android.utils.getKey
import hh.game.mgba_android.tracker.models.TrackerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLUtils
import org.libsdl.app.SDLUtils.mFullscreenModeActive
import org.libsdl.app.SDLUtils.onNativeKeyDown
import org.libsdl.app.SDLUtils.onNativeKeyUp
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

open class GameActivity : SDLActivity(), InputManager.InputDeviceListener {
    // SDL library list — override SDLActivity default
    override val libraries: Array<String>
        get() = arrayOf("SDL2", "mgba", "mgba_android")

    // Reload ROM in the running emulator core without restarting the Activity.
    // Sets g_pendingRomPath in C++ and signals the emulation thread to stop; SDL thread reloads.
    private external fun loadRomJNI(path: String)

    // Game arguments stored before SDL thread starts so getArguments() is ready
    private var gameArgPath: String? = null
    private var gameCheatPath: String? = null

    override fun getArguments(): Array<String> =
        listOfNotNull(gameArgPath, gameCheatPath).toTypedArray()

    private var runFPS = true
    private var setFPS = 60f
    private var resumePending = false
    private var hasEverBeenPaused = false  // guard: don't call ResumeGame() before mCoreThread is ready
    private var isMute = false
    private var defaultFps = 60f
    private var secondaryFps = 60f
    private var lAsSpeed = false
    private var speedToggleMode = false
    private var speedToggled = false
    private val actionBindings = HashMap<BindableAction, Int>()
    private val gbaKeyBindings = HashMap<Int, Int>() // pressed keycode → native keycode
    // Button drag-across tracking
    private val gbaButtonViews = mutableListOf<Pair<View, Int>>()  // view → key code
    private var dragActiveKey: Int? = null
    // Tracker layout state
    private var splitFraction = 0.7f
    private var trackerCollapsible = false
    private var hideCollapseButton = false
    private var trackerExpanded by mutableStateOf(true)
    private var trackerFontScale by mutableStateOf(1.0f)
    private var effectiveCollapsible by mutableStateOf(false)
    private var screenWidthPx = 0
    private var trackerViewRef: ComposeView? = null
    private var templateResult = ArrayList<Pair<Int, Int>>()
    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val gameNum = intent.getStringExtra("cheat")
                val gamePath = intent.getStringExtra("gamepath")
                
                // Priority Load: Game Directory -> Private Directory
                var cheatFileToLoad = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
                if (gamePath != null) {
                    val gameDirCheat = File(File(gamePath).parent, "$gameNum.cheats")
                    // Check .cheats
                    if (gameDirCheat.exists()) {
                         cheatFileToLoad = gameDirCheat.absolutePath
                    } else {
                         // Check .cht
                         val gameDirCht = File(File(gamePath).parent, "$gameNum.cht")
                         if (gameDirCht.exists()) {
                             cheatFileToLoad = gameDirCht.absolutePath
                         }
                    }
                }
                Log.d("GameActivity", "Reloading cheats from: $cheatFileToLoad")
                reCallCheats(processCheatsAndGetNativePath(cheatFileToLoad))
            }
        }

    // Helper to convert Legacy Format (User preferred) to Libretro Format (Native required) on the fly
    private fun isARDSCheat(cheat: hh.game.mgba_android.utils.Cheat): Boolean {
        // Simple Heuristic: Check for AR DS exclusive opcodes in the code lines
        // AR DS Opcodes: D0-DF (Data/Flow), E0 (Patch)
        // Also 30-6F are conditional but we'll focus on the structural ones first to avoid false positives with GBA addresses
        // But the user specifically mentioned "AR DS"
        val lines = cheat.cheatCode.lines()
        for (line in lines) {
            val parts = line.trim().split(" ")
            if (parts.size == 2 && parts[0].length == 8 && parts[1].length == 8) {
               val opStart = parts[0][0].toUpperCase()
               if (opStart == 'D' || opStart == 'E') {
                   return true
               }
            }
        }
        return false
    }

    private fun processCheatsAndGetNativePath(path: String): String {
         val legacyFile = File(path)
         if (!legacyFile.exists()) return path
         
         // Parse using the CheatUtils
         val cheats = CheatUtils.parseUserCheatFile(legacyFile)
         
         // Reset Native AR DS Engine First
         resetARDSCheats()
         var ardsCount = 0
         
         // Generate Libretro content manually for Standard Cheats
         val sb = StringBuilder()
         var stdCount = 0
         
         cheats.forEachIndexed { i, cheat ->
             if (cheat.isSelect) {
                 if (isARDSCheat(cheat)) {
                     // Route to AR DS Engine
                     cheat.cheatCode.lines().forEach { line ->
                         val parts = line.trim().split(" ")
                         if (parts.size >= 2) {
                             try {
                                  val op = parts[0].toLong(16).toInt()
                                  val value = parts[1].toLong(16).toInt()
                                  addARDSCheat(op, value)
                             } catch (e: Exception) {
                                 Log.e("GameActivity", "Error parsing ARDS code: $line")
                             }
                         }
                     }
                     ardsCount++
                     Log.d("GameActivity", "Loaded AR DS Cheat: ${cheat.cheatTitle}")
                 } else {
                     // Route to Standard Engine
                     sb.append("cheat${stdCount}_desc = \"${cheat.cheatTitle}\"\n")
                     val code = cheat.cheatCode.replace("\n", "+")
                     sb.append("cheat${stdCount}_code = \"$code\"\n")
                     sb.append("cheat${stdCount}_enable = true\n\n")
                     stdCount++
                 }
             }
         }
         
         // Add header count for standard cheats
         val finalContent = "cheats = $stdCount\n\n" + sb.toString()
         
         if (ardsCount > 0) {
             Log.d("GameActivity", "Total AR DS Cheats Loaded: $ardsCount")
         }
         
         // Save to a temp location that native core will read
         val nativeFile = File(cacheDir, "running_cheats.cht")
         try {
             val writer = java.io.BufferedWriter(java.io.FileWriter(nativeFile))
             writer.write(finalContent)
             writer.close()
             return nativeFile.absolutePath
         } catch (e: Exception) {
             e.printStackTrace()
             return path // Fallback
         }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val f = File(getExternalFilesDir(null), "crash_log.txt")
                f.appendText("${java.util.Date()}: ${t.name}\n${e.stackTraceToString()}\n\n")
            } catch (_: Exception) {}
            defaultExceptionHandler?.uncaughtException(t, e)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mFullscreenModeActive = false

        // Resolve intent extras BEFORE super.onCreate() so getArguments() is ready
        // when the SDL thread starts (SDL libraries load inside super.onCreate()).
        val gamepath = intent.getStringExtra("gamepath")
        val gameNum = intent.getStringExtra("cheat")
        Log.d("GameActivity", "onCreate: gameNum='$gameNum', gamepath='$gamepath'")
        gameArgPath = gamepath
        if (gamepath != null) QuickloadManager.register(applicationContext, gamepath)

        // super.onCreate() loads native libs, creates SDL surface, calls setContentView(mLayout)
        super.onCreate(savedInstanceState)

        // ── Tracker: resize SDL surface to (splitFraction)% width, attach tracker panel ──
        splitFraction = EmulatorPreferences.getSplitFraction(this)
        trackerCollapsible = EmulatorPreferences.getTrackerCollapsible(this)
        hideCollapseButton = EmulatorPreferences.getHideCollapseButton(this)
        screenWidthPx = resources.displayMetrics.widthPixels
        val isOverlay = splitFraction == 0.0f || splitFraction == 1.0f
        effectiveCollapsible = trackerCollapsible || isOverlay
        trackerFontScale = computeFontScale(splitFraction)
        trackerExpanded = splitFraction != 1.0f  // game-overlay mode starts collapsed
        val arrowPx = if (hideCollapseButton) 0 else (24 * resources.displayMetrics.density).toInt()
        val gameWidth = if (isOverlay) screenWidthPx else (screenWidthPx * splitFraction).toInt()
        val trackerLeft = when {
            isOverlay && !trackerExpanded -> screenWidthPx - arrowPx
            isOverlay -> 0
            else -> gameWidth
        }

        mSurface?.layoutParams = RelativeLayout.LayoutParams(
            gameWidth,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        )

        // Tell SurfaceFlinger the content rate is fixed 59.7275fps (GBA) so it locks the
        // display to a 60Hz multiple and only swaps buffers at those boundaries — eliminates
        // persistent tearing on LTPO displays (Pixel 7 Pro 120Hz).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mSurface?.holder?.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    holder.surface.setFrameRate(
                        59.7275f,
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
                    )
                }
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {}
            })
        }

        MemoryBridge.reader = { addr, len -> getMemoryRange(addr, len) }
        TrackerPoller.start(applicationContext, lifecycleScope)

        val trackerView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by TrackerPoller.state.collectAsState()
                TrackerPanel(
                    state = state,
                    onQuickload = if (QuickloadManager.canQuickload()) {
                        { loadNextRom() }
                    } else null,
                    onReroll = { TrackerPoller.rerollBall() },
                    fontScale = trackerFontScale,
                    isCollapsible = effectiveCollapsible && !hideCollapseButton,
                    isExpanded = trackerExpanded,
                    onToggleExpand = { applyTrackerExpansion(!trackerExpanded) },
                )
            }
        }
        trackerViewRef = trackerView
        mLayout?.addView(
            trackerView,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT,
            ).apply { leftMargin = trackerLeft },
        )
        // ── End tracker setup ──────────────────────────────────────────────────

        // Cheats can only be processed after native libs are loaded (resetARDSCheats is JNI)
        var cheatRefPath = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
        if (gamepath != null) {
            val gameDirCheat = File(File(gamepath).parent, "$gameNum.cheats")
            if (gameDirCheat.exists()) cheatRefPath = gameDirCheat.absolutePath
        }
        if (gamepath != null) CheatUtils.generateCheat(this, gameNum, null)
        gameCheatPath = processCheatsAndGetNativePath(cheatRefPath)

        // Inflate overlay controls (dpad, buttons, FPS text, etc.) on top of SDL surface
        val overlay = layoutInflater.inflate(R.layout.activity_game, null)
        mLayout?.addView(overlay, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        ))
        // Update the gameZoneBoundary guideline to match the selected split fraction
        if (overlay is ConstraintLayout) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(overlay)
            constraintSet.setGuidelinePercent(R.id.gameZoneBoundary, splitFraction)
            constraintSet.applyTo(overlay)
        }

        addGameControler()
        applyControlsStyle()
        (getSystemService(INPUT_SERVICE) as InputManager)
            .registerInputDeviceListener(this, null)
        updateOnscreenControls()
//        GlobalScope.launch {
//            Gameutils.getFPS().toString()
//        }
        initSwappy()

        // Load speed preferences — apply default FPS only after emulator core is live
        defaultFps = EmulatorPreferences.getDefaultFps(this)
        secondaryFps = EmulatorPreferences.getSecondaryFps(this)
        BindableAction.entries.forEach { action ->
            actionBindings[action] = EmulatorPreferences.getBinding(this, action)
        }
        reloadGbaKeyBindings()
        lAsSpeed = EmulatorPreferences.getLAsSpeed(this)
        speedToggleMode = EmulatorPreferences.getSpeedToggleMode(this)
        setFPS = defaultFps
        if (defaultFps != 60f) {
            lifecycleScope.launch {
                TrackerPoller.state.first { it is TrackerState.Active }
                setFPS = defaultFps
                Forward(defaultFps)
            }
        }
        isMute = EmulatorPreferences.getMuted(this)
        if (isMute) {
            lifecycleScope.launch {
                TrackerPoller.state.first { it is TrackerState.Active }
                Mute(true)
            }
        }

        // Copy shaders from assets to files dir
        // Always copy to ensure we have the latest shaders (e.g. after app update)
        val shaderDir = File(filesDir, "shaders")
        copyAssets("shaders", shaderDir.absolutePath)
        
        // Initialize Tools Button
        findViewById<View>(R.id.tools_btn).setOnClickListener {
            val options = arrayOf("Shaders", "Memory Tools", "Save State", "Load State", "Cheats", "Sound", "Next Run →", "Tracker Size", "Settings", "Close ROM")
            AlertDialog.Builder(this)
                .setTitle("Tools")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showShaderMenu()
                        1 -> openHexEditor()
                        2 -> {
                            PauseGame()
                            var resumed = false
                            PopDialogFragment(getString(R.string.savestatetitle))
                                .also {
                                    it.setOnDialogClickListener(object : OnDialogClickListener {
                                        override fun onPostive() {
                                            Toast.makeText(this@GameActivity,
                                                if (QuickSaveState()) getString(R.string.state_saved)
                                                else getString(R.string.state_save_fail),
                                                Toast.LENGTH_SHORT).show()
                                            resumed = true; ResumeGame()
                                        }
                                        override fun onNegative() { resumed = true; ResumeGame() }
                                        override fun onDismiss() { if (!resumed) ResumeGame() }
                                    })
                                }
                                .show(supportFragmentManager, "savestate")
                        }
                        3 -> {
                            PauseGame()
                            var resumed = false
                            PopDialogFragment(getString(R.string.loadstatetitle))
                                .also {
                                    it.setOnDialogClickListener(object : OnDialogClickListener {
                                        override fun onPostive() {
                                            Toast.makeText(this@GameActivity,
                                                if (QuickLoadState()) getString(R.string.state_loaded)
                                                else getString(R.string.state_load_fail),
                                                Toast.LENGTH_SHORT).show()
                                            resumed = true; ResumeGame()
                                        }
                                        override fun onNegative() { resumed = true; ResumeGame() }
                                        override fun onDismiss() { if (!resumed) ResumeGame() }
                                    })
                                }
                                .show(supportFragmentManager, "loadstate")
                        }
                        4 -> {
                            startForResult.launch(Intent(this, CheatsActivity::class.java).also {
                                it.putExtra("gamepath", gamepath)
                                when (intent.getStringExtra("gametype")) {
                                    "GBA" -> intent.getParcelableExtra<hh.game.mgba_android.database.GBA.GBAgame>("gamedetail").let { game ->
                                        it.putExtra("gamedetail", game as hh.game.mgba_android.database.GBA.GBAgame)
                                        it.putExtra("gametype", hh.game.mgba_android.utils.Gametype.GBA.name)
                                        it.putExtra("cheat", game?.GameNum)
                                    }
                                    else -> intent.getParcelableExtra<hh.game.mgba_android.database.GB.GBgame>("gamedetail").let { game ->
                                        it.putExtra("gamedetail", game as hh.game.mgba_android.database.GB.GBgame)
                                        it.putExtra("gametype", hh.game.mgba_android.utils.Gametype.GB.name)
                                    }
                                }
                            })
                        }
                        5 -> {
                            Mute(!isMute)
                            isMute = !isMute
                            EmulatorPreferences.setMuted(this, isMute)
                            Toast.makeText(this, if (isMute) "Sound Off" else "Sound On", Toast.LENGTH_SHORT).show()
                        }
                        6 -> doNextRun()
                        7 -> showTrackerSizeDialog()
                        8 -> showSettingsDialog()
                        9 -> AlertDialog.Builder(this)
                            .setTitle("Close ROM")
                            .setMessage("Return to game list?")
                            .setPositiveButton("Close") { _, _ ->
                                finish()
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                .show()
        }

        val fpsText = findViewById<TextView>(R.id.fps_text)
        fpsText.visibility = if (EmulatorPreferences.getShowFps(this)) View.VISIBLE else View.GONE
        lifecycleScope.launch(Dispatchers.Main) {
            while (runFPS) {
                val actual = getFPS()
                fpsText.text = "FPS: %.1f".format(actual)
                val target = setFPS
                if (target > 60f && actual < target * 0.8f) {
                    Log.w("mGBA_Perf", "FPS below target: actual=%.1f target=%.1f stalls=%d".format(actual, target, getStallCount()))
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    val pm = getSystemService(PowerManager::class.java)
                    val headroom = pm.getThermalHeadroom(1)
                    if (headroom < 0.5f) {
                        Log.w("mGBA_Perf", "Thermal throttle imminent: headroom=%.2f".format(headroom))
                    }
                }
                delay(500)
            }
        }
    }

    private fun showShaderMenu() {
        val shaderDir = File(filesDir, "shaders")
        if (!shaderDir.exists()) shaderDir.mkdirs()
        
        val shaderFiles = shaderDir.listFiles { file -> 
            file.isDirectory || file.extension == "shader" 
        }?.map { it.name }?.toMutableList() ?: mutableListOf()
        
        shaderFiles.add(0, "Clear")
        
        AlertDialog.Builder(this)
            .setTitle("Select Shader")
            .setItems(shaderFiles.toTypedArray()) { _, which ->
                val selectedName = shaderFiles[which]
                if (selectedName == "Clear") {
                    setShader("")
                    // Toast.makeText(this, "Shader Cleared", Toast.LENGTH_SHORT).show()
                } else {
                    val selectedFile = File(shaderDir, selectedName)
                    setShader(selectedFile.absolutePath)
                    // Toast.makeText(this, "Applied: $selectedName", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openHexEditor() {
        PauseGame()
        hh.game.mgba_android.fragment.HexEditorFragment().apply {
            // Dismiss listener handles ResumeGame
        }
        .show(supportFragmentManager, "hex_editor")
    }

    private fun copyAssets(assetPath: String, destPath: String) {
        val assetManager = assets
        var files: Array<String>? = null
        try {
            files = assetManager.list(assetPath)
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
        if (files != null) {
            if (files.isEmpty()) {
                // It's a file
                try {
                    val `in` = assetManager.open(assetPath)
                    val out = java.io.FileOutputStream(destPath)
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (`in`.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    `in`.close()
                    out.flush()
                    out.close()
                } catch (e: java.io.IOException) {
                    e.printStackTrace()
                }
            } else {
                // It's a directory
                val dir = File(destPath)
                if (!dir.exists()) dir.mkdirs()
                for (fileName in files) {
                    copyAssets(
                        if (assetPath == "") fileName else "$assetPath/$fileName",
                        "$destPath/$fileName"
                    )
                }
            }
        }
    }

    // ── Gamepad detection ──────────────────────────────────────────────────────
    private fun isGamepad(deviceId: Int): Boolean {
        val device = InputDevice.getDevice(deviceId) ?: return false
        if (device.isVirtual) return false
        val src = device.sources
        return (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
               (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }

    private fun hasGamepadConnected(): Boolean =
        InputDevice.getDeviceIds().any { isGamepad(it) }

    private fun reloadGbaKeyBindings() {
        gbaKeyBindings.clear()
        GbaButton.entries.forEach { btn ->
            val pressedKey = EmulatorPreferences.getGbaKeyBinding(this, btn)
            gbaKeyBindings[pressedKey] = btn.nativeKeyCode
        }
    }

    private fun updateOnscreenControls() {
        val alwaysShow = EmulatorPreferences.getAlwaysShowControls(this)
        val hideControls = EmulatorPreferences.getHideOnScreenControls(this)
        val visible = when {
            hideControls -> View.GONE
            alwaysShow || !hasGamepadConnected() -> View.VISIBLE
            else -> View.INVISIBLE
        }
        findViewById<View>(R.id.padboardInclude)?.visibility = visible
        findViewById<View>(R.id.tools_btn)?.visibility = visible
    }

    private fun applyTrackerExpansion(expanded: Boolean) {
        trackerExpanded = expanded
        val arrowPx = if (hideCollapseButton) 0 else (24 * resources.displayMetrics.density).toInt()
        val isOverlay = splitFraction == 0.0f || splitFraction == 1.0f
        val newGameWidth = when {
            isOverlay -> screenWidthPx
            expanded -> (screenWidthPx * splitFraction).toInt()
            else -> screenWidthPx - arrowPx
        }
        val newTrackerLeft = when {
            isOverlay && expanded -> 0
            isOverlay -> screenWidthPx - arrowPx
            else -> newGameWidth
        }
        mSurface?.layoutParams?.width = newGameWidth
        mSurface?.requestLayout()
        val tv = trackerViewRef ?: return
        tv.layoutParams = (tv.layoutParams as RelativeLayout.LayoutParams).apply {
            leftMargin = newTrackerLeft
        }
        tv.requestLayout()
    }

    private fun computeFontScale(fraction: Float): Float {
        val trackerFrac = 1f - fraction.coerceIn(0f, 1f)
        return if (trackerFrac < 0.05f) 1.0f else trackerFrac / 0.3f
    }

    private fun applyTrackerSize(newFraction: Float) {
        splitFraction = newFraction
        EmulatorPreferences.setSplitFraction(this, newFraction)
        val isOverlay = newFraction == 0.0f || newFraction == 1.0f
        effectiveCollapsible = trackerCollapsible || isOverlay
        trackerFontScale = computeFontScale(newFraction)
        trackerExpanded = newFraction != 1.0f  // game-overlay mode starts collapsed

        val arrowPx = if (hideCollapseButton) 0 else (24 * resources.displayMetrics.density).toInt()
        val gameWidth = if (isOverlay) screenWidthPx else (screenWidthPx * newFraction).toInt()
        val trackerLeft = when {
            isOverlay && !trackerExpanded -> screenWidthPx - arrowPx
            isOverlay -> 0
            else -> gameWidth
        }
        mSurface?.layoutParams?.width = gameWidth
        mSurface?.requestLayout()
        val tv = trackerViewRef ?: return
        tv.layoutParams = (tv.layoutParams as RelativeLayout.LayoutParams).apply { leftMargin = trackerLeft }
        tv.requestLayout()
        // Update gameZoneBoundary guideline so dpad/tools_btn stay in game zone
        val overlayView = mLayout?.let { it.getChildAt(it.childCount - 1) }
        if (overlayView is ConstraintLayout) {
            val cs = ConstraintSet()
            cs.clone(overlayView)
            cs.setGuidelinePercent(R.id.gameZoneBoundary, if (isOverlay) 1.0f else newFraction)
            cs.applyTo(overlayView)
        }
    }

    private fun showTrackerSizeDialog() {
        val fractions = floatArrayOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f)
        val labels = arrayOf(
            "100% / 0% (Game Overlay)", "90% / 10%", "80% / 20%", "70% / 30% (Default)",
            "60% / 40%", "50% / 50%", "40% / 60%", "30% / 70%", "20% / 80%", "10% / 90%",
            "0% / 100% (Tracker Overlay)"
        )
        val currentIdx = fractions.indexOfFirst { Math.abs(it - splitFraction) < 0.01f }.let { if (it < 0) 3 else it }
        AlertDialog.Builder(this)
            .setTitle("Tracker Size (Game% / Tracker%)")
            .setSingleChoiceItems(labels, currentIdx) { dlg, which ->
                dlg.dismiss()
                applyTrackerSize(fractions[which])
            }
            .show()
    }

    private fun applyControlsStyle() {
        val padboard = findViewById<View>(R.id.padboardInclude)
        padboard?.alpha = EmulatorPreferences.getControlsAlpha(this)
        val scale = EmulatorPreferences.getControlsScale(this)
        padboard?.scaleX = scale
        padboard?.scaleY = scale
    }

    private fun showSettingsDialog() {
        val ctx = this
        val scrollView = ScrollView(ctx)
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        scrollView.addView(container)

        val opacityLabel = TextView(ctx)
        container.addView(opacityLabel)
        val opacityBar = SeekBar(ctx).apply {
            max = 100
            progress = (EmulatorPreferences.getControlsAlpha(ctx) * 100).toInt()
        }
        opacityLabel.text = "Controls Opacity: ${opacityBar.progress}%"
        opacityBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                opacityLabel.text = "Controls Opacity: $p%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        container.addView(opacityBar)

        val scaleLabel = TextView(ctx)
        container.addView(scaleLabel)
        val scaleBar = SeekBar(ctx).apply {
            max = 100  // 0 → 50%, 100 → 150%
            progress = ((EmulatorPreferences.getControlsScale(ctx) - 0.5f) * 100).toInt()
        }
        scaleLabel.text = "Controls Scale: ${scaleBar.progress + 50}%"
        scaleBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                scaleLabel.text = "Controls Scale: ${p + 50}%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        container.addView(scaleBar)

        val showFpsCheck = CheckBox(ctx).apply {
            text = "Show FPS"
            isChecked = EmulatorPreferences.getShowFps(ctx)
        }
        container.addView(showFpsCheck)

        val lAsSpeedCheck = CheckBox(ctx).apply {
            text = "L button = Fast Forward (disables GBA L)"
            isChecked = EmulatorPreferences.getLAsSpeed(ctx)
        }
        container.addView(lAsSpeedCheck)

        val speedToggleCheck = CheckBox(ctx).apply {
            text = "Fast Forward: Toggle (instead of Hold)"
            isChecked = EmulatorPreferences.getSpeedToggleMode(ctx)
        }
        container.addView(speedToggleCheck)

        val speedLabel = TextView(ctx).apply { text = "Speed Button:" }
        container.addView(speedLabel)
        val speedSpinner = Spinner(ctx)
        val speedAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, EmulatorPreferences.buttonOptions)
        speedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speedSpinner.adapter = speedAdapter
        val currentSpeed = EmulatorPreferences.getSpeedButton(ctx)
        val speedIdx = EmulatorPreferences.buttonOptions.indexOf(currentSpeed).let { if (it < 0) 0 else it }
        speedSpinner.setSelection(speedIdx)
        container.addView(speedSpinner)

        AlertDialog.Builder(ctx)
            .setTitle("Settings")
            .setView(scrollView)
            .setPositiveButton("OK") { _, _ ->
                val alpha = opacityBar.progress / 100f
                val scale = (scaleBar.progress + 50) / 100f
                EmulatorPreferences.setControlsAlpha(ctx, alpha)
                EmulatorPreferences.setControlsScale(ctx, scale)
                EmulatorPreferences.save(
                    ctx,
                    defaultFps = EmulatorPreferences.getDefaultFps(ctx),
                    secondaryFps = EmulatorPreferences.getSecondaryFps(ctx),
                    button = speedSpinner.selectedItem as String,
                    showFps = showFpsCheck.isChecked,
                    lAsSpeed = lAsSpeedCheck.isChecked,
                    speedToggleMode = speedToggleCheck.isChecked,
                )
                lAsSpeed = lAsSpeedCheck.isChecked
                speedToggleMode = speedToggleCheck.isChecked
                speedToggled = false
                applyControlsStyle()
                findViewById<TextView>(R.id.fps_text)?.visibility =
                    if (showFpsCheck.isChecked) View.VISIBLE else View.GONE
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onInputDeviceAdded(deviceId: Int) { updateOnscreenControls() }
    override fun onInputDeviceRemoved(deviceId: Int) { updateOnscreenControls() }
    override fun onInputDeviceChanged(deviceId: Int) { updateOnscreenControls() }
    // ── End gamepad detection ──────────────────────────────────────────────────

    override fun onDestroy() {
        (getSystemService(INPUT_SERVICE) as InputManager).unregisterInputDeviceListener(this)
        TrackerPoller.stop()
        MemoryBridge.reader = null
        QuickloadManager.unregister(applicationContext)
        super.onDestroy()
        runFPS = false
    }

    override fun onPause() {
        super.onPause()
        hasEverBeenPaused = true
        resumePending = false
        PauseGame()
        Mute(true)          // silence audio whenever app backgrounds
    }

    override fun onResume() {
        super.onResume()
        // Reload cheats on resume in case they were edited in CheatsActivity
         if (intent.getStringExtra("gamepath") != null) {
            val gamePath = intent.getStringExtra("gamepath")
            val parentDir = File(gamePath).parentFile
            // Use the same gameNum logic as onCreate
            val gameNum = intent.getStringExtra("cheat") ?: File(gamePath).nameWithoutExtension

            // Unified Cheat Reloading
            var cheatRefPath = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
            val gameDirCheat = File(parentDir, "$gameNum.cheats") // Priority to game dir
            if (gameDirCheat.exists()) cheatRefPath = gameDirCheat.absolutePath

            // Process (Reload AR DS, Update .cht file)
            // Note: Updated .cht file might not be re-read by core without restart,
            // but AR DS cheats are updated immediately via JNI.
            processCheatsAndGetNativePath(cheatRefPath)
         }
        // Reload action bindings in case user changed them in settings while paused
        BindableAction.entries.forEach { action ->
            actionBindings[action] = EmulatorPreferences.getBinding(this, action)
        }
        reloadGbaKeyBindings()
        // ResumeGame() is deferred to onWindowFocusChanged to avoid racing SDL surface readiness.
        // SDL requires mHasFocus=true (set on focus grant) before nativeResume() unblocks the
        // render thread. Calling ResumeGame() here would wake the mGBA core before SDL is ready.
        // Only schedule ResumeGame() if the core has been started and paused at least once —
        // on initial launch mCoreThread is not yet initialized and calling mCoreThreadContinue
        // on a null pointer causes SIGSEGV (fault addr 0x14 = mutex offset in mCoreThread).
        if (hasEverBeenPaused) resumePending = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && resumePending) {
            resumePending = false
            Mute(isMute)    // restore user's mute preference before game resumes
            ResumeGame()
        }
    }

    private fun loadNextRom() {
        Log.d("Quickload", "loadNextRom: family=${QuickloadManager.currentFamily}")
        TrackerPoller.manualNextRun()
        lifecycleScope.launch(Dispatchers.IO) {
            val nextPath = QuickloadManager.advanceToNext(applicationContext)
            Log.d("Quickload", "nextPath=$nextPath")
            withContext(Dispatchers.Main) {
                if (nextPath != null) {
                    val next = Intent(this@GameActivity, GameActivity::class.java).apply {
                        putExtra("gamepath", nextPath)
                        val cheat = intent.getStringExtra("cheat")
                        if (cheat != null) putExtra("cheat", cheat)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(next)
                    android.os.Process.killProcess(android.os.Process.myPid())
                } else {
                    Toast.makeText(this@GameActivity, "No next ROM found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun doNextRun() {
        if (QuickloadManager.canQuickload()) {
            loadNextRom()
        } else {
            Toast.makeText(this, "Quickload not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    private fun isLButtonEvent(keyCode: Int): Boolean =
        getKey(keyCode) == GBAKeys.GBA_KEY_L.key || gbaKeyBindings[keyCode] == GBAKeys.GBA_KEY_L.key

    private fun applySpeed(fast: Boolean) {
        val fps = if (fast) secondaryFps else defaultFps
        setFPS = fps; Forward(fps)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        var handled = false

        // L-as-speed intercept — always consume L before it reaches the GBA emulator
        if (lAsSpeed && isLButtonEvent(event.keyCode)) {
            if (secondaryFps != defaultFps) {
                when {
                    !speedToggleMode -> when (event.action) {
                        KeyEvent.ACTION_DOWN -> { applySpeed(true);  handled = true }
                        KeyEvent.ACTION_UP   -> { applySpeed(false); handled = true }
                    }
                    event.action == KeyEvent.ACTION_DOWN -> {
                        speedToggled = !speedToggled; applySpeed(speedToggled); handled = true
                    }
                }
            }
            return true  // always swallow — never send L to GBA
        }

        // GBA game controls — gamepad/dpad path (hardcoded, always works)
        val gbaKey = getKey(event.keyCode)
        if (gbaKey != GBAKeys.GBA_KEY_NONE.key) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> { onNativeKeyDown(gbaKey); handled = true }
                KeyEvent.ACTION_UP   -> { onNativeKeyUp(gbaKey);   handled = true }
            }
        }

        // User-configured keyboard → GBA button bindings
        if (!handled) {
            val nativeCode = gbaKeyBindings[event.keyCode]
            if (nativeCode != null) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> { onNativeKeyDown(nativeCode); handled = true }
                    KeyEvent.ACTION_UP   -> { onNativeKeyUp(nativeCode);   handled = true }
                }
            }
        }

        // Action bindings (raw keyCode comparison)
        val speedKey = actionBindings[BindableAction.SPEED_HOLD] ?: -1
        if (speedKey != -1 && event.keyCode == speedKey && secondaryFps != defaultFps) {
            when {
                !speedToggleMode -> when (event.action) {
                    KeyEvent.ACTION_DOWN -> { applySpeed(true);  handled = true }
                    KeyEvent.ACTION_UP   -> { applySpeed(false); handled = true }
                }
                event.action == KeyEvent.ACTION_DOWN -> {
                    speedToggled = !speedToggled; applySpeed(speedToggled); handled = true
                }
                else -> handled = true  // swallow ACTION_UP in toggle mode
            }
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            val kc = event.keyCode
            when {
                kc != -1 && kc == (actionBindings[BindableAction.QUICK_SAVE] ?: -1) -> {
                    PauseGame()
                    QuickSaveState()
                    ResumeGame()
                    Toast.makeText(this, getString(R.string.state_saved), Toast.LENGTH_SHORT).show()
                    handled = true
                }
                kc != -1 && kc == (actionBindings[BindableAction.QUICK_LOAD] ?: -1) -> {
                    PauseGame()
                    QuickLoadState()
                    ResumeGame()
                    Toast.makeText(this, getString(R.string.state_loaded), Toast.LENGTH_SHORT).show()
                    handled = true
                }
                kc != -1 && kc == (actionBindings[BindableAction.TRACKER_TOGGLE] ?: -1) -> {
                    applyTrackerExpansion(!trackerExpanded); handled = true
                }
                kc != -1 && kc == (actionBindings[BindableAction.NEXT_RUN] ?: -1) -> {
                    doNextRun(); handled = true
                }
                kc != -1 && kc == (actionBindings[BindableAction.MUTE] ?: -1) -> {
                    isMute = !isMute
                    Mute(isMute)
                    EmulatorPreferences.setMuted(this, isMute)
                    Toast.makeText(this, if (isMute) "Sound Off" else "Sound On", Toast.LENGTH_SHORT).show()
                    handled = true
                }
                kc != -1 && kc == (actionBindings[BindableAction.TOOLS_MENU] ?: -1) -> {
                    findViewById<View>(R.id.tools_btn)?.performClick(); handled = true
                }
            }
        }
        return handled || super.dispatchKeyEvent(event)
    }

    private fun addGameControler() {
        // Action buttons: simple independent per-finger press/release (supports multi-touch)
        listOf(R.id.rBtn to "R", R.id.lBtn to "L", R.id.aBtn to "A", R.id.bBtn to "B",
               R.id.selectBtn to "select", R.id.startBtn to "start")
            .forEach { (id, name) -> findViewById<View>(id).setActionKeyListener(getKey(name)) }

        // D-pad: drag-across tracking (slide between directions)
        val dpadDefs = listOf(R.id.upBtn to "up", R.id.downBtn to "down",
                              R.id.leftBtn to "left", R.id.rightBtn to "right")
        gbaButtonViews.clear()
        dpadDefs.forEach { (id, name) ->
            val v: View = findViewById(id)
            gbaButtonViews.add(v to getKey(name))
        }
        gbaButtonViews.forEach { (v, _) -> v.setDpadKeyListener() }
    }

    private fun findDpadKeyAt(absX: Float, absY: Float): Int? {
        val loc = IntArray(2)
        return gbaButtonViews.firstOrNull { (btn, _) ->
            btn.getLocationOnScreen(loc)
            absX >= loc[0] && absX < loc[0] + btn.width &&
            absY >= loc[1] && absY < loc[1] + btn.height
        }?.second
    }


    private fun searchMemory(value: Int, isNewSearch: Boolean = false) {
        var mem = ArrayList<Pair<Int, Int>>()
        getMemoryBlock().filter {
            it.id == 2.toLong() || it.id == 3.toLong()
        }.forEach {
            mem += it.valuearray
        }
        if (isNewSearch) {
            templateResult = ArrayList(mem.filter {
                it.second == value
            })
        } else {
            templateResult = ArrayList(findMatchingPairs(templateResult, mem).filter {
                it.second == value
            })
        }
    }

    private fun findMatchingPairs(
        a: ArrayList<Pair<Int, Int>>,
        b: ArrayList<Pair<Int, Int>>
    ): ArrayList<Pair<Int, Int>> {
        val set = HashSet<Int>()
        val aFirstValues = a.map { it.first }.toSet()
        val result = b.filter { it.first in aFirstValues } as ArrayList<Pair<Int, Int>>
        return result
    }

    private fun getScreenShot() {
        intent.getStringExtra("gamepath")?.replace(".gba", ".jpg")?.apply {
            var screenshotfile = File(this)
            if (!screenshotfile.exists()) screenshotfile.createNewFile()
            TakeScreenshot(this)
        }
    }

    private fun setForward(view: TextView, times: Int): Float {
        view.text = getString(R.string.forwarding, times.toString())
        return 60f * times
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        var handled = false
        ev?.let {
            getDirectionPressed(ev).let {
                if (it == 0) {
                    val toRelease = ArrayList(lastDirect)
                    lastDirect.clear()
                    toRelease.forEach { onNativeKeyUp(it) }
                    handled = true
                } else {
                    // Release any previously held directions that differ from the new one
                    val toRelease = lastDirect.filter { held -> held != it }
                    lastDirect.removeAll(toRelease.toSet())
                    toRelease.forEach { held -> onNativeKeyUp(held) }
                    var gbaKey = getKey(it)
                    if (gbaKey != GBAKeys.GBA_KEY_NONE.key) {
                        onNativeKeyDown(gbaKey)
                        handled = true
                    }
                }
            }
        }
        return handled || super.dispatchGenericMotionEvent(ev)
    }

    // Simple press/release — each finger is independent (multi-touch safe)
    private fun View.setActionKeyListener(key: Int) {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> onNativeKeyDown(key)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onNativeKeyUp(key)
            }
            true
        }
    }

    // D-pad drag — one touch slides between directions; dragActiveKey scoped to d-pad only
    private fun View.setDpadKeyListener() {
        val myKey = gbaButtonViews.firstOrNull { it.first === this }?.second ?: return
        val screenLoc = IntArray(2)
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragActiveKey?.let { onNativeKeyUp(it) }
                    dragActiveKey = myKey
                    onNativeKeyDown(myKey)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragActiveKey == myKey) {
                        dragActiveKey = null
                        onNativeKeyUp(myKey)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    v.getLocationOnScreen(screenLoc)
                    val absX = screenLoc[0] + event.x
                    val absY = screenLoc[1] + event.y
                    val newKey = findDpadKeyAt(absX, absY)
                    if (newKey != dragActiveKey) {
                        dragActiveKey?.let { onNativeKeyUp(it) }
                        dragActiveKey = newKey
                        newKey?.let { onNativeKeyDown(it) }
                    }
                }
            }
            true
        }
    }

    //    override fun getArguments(): Array<String> {
//        var gamepath = intent.getStringExtra("gamepath")
//        val gameNum = intent.getStringExtra("cheat")
//        var cheatpath = gamepath?.replace(".gba", ".cheats")
//        if (!File(cheatpath).exists()) cheatpath = null
//        var internalCheatFile = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
//
//        var fragmentShader = "uniform sampler2D tex;\n" +
//                "uniform vec2 texSize;\n" +
//                "varying vec2 texCoord;\n" +
//                "\n" +
//                "uniform float boundBrightness;\n" +
//                "\n" +
//                "void main()\n" +
//                "{\n" +
//                "\tvec4 color = texture2D(tex, texCoord);\n" +
//                "\n" +
//                "\tif (int(mod(texCoord.s * texSize.x * 3.0, 3.0)) == 0 ||\n" +
//                "\t\tint(mod(texCoord.t * texSize.y * 3.0, 3.0)) == 0)\n" +
//                "\t{\n" +
//                "\t\tcolor.rgb *= vec3(1.0, 1.0, 1.0) * boundBrightness;\n" +
//                "\t}\n" +
//                "\n" +
//                "\tgl_FragColor = color;\n" +
//                "}"
//        return if (gamepath != null) {
//            CheatUtils.generateCheat(this, gameNum, cheatpath)
//            arrayOf(
//                gamepath,
//                internalCheatFile,
//                fragmentShader
//            )
//        } else emptyArray<String>()
//
//    }
    external fun reCallCheats(cheatfile: String)
    external fun QuickSaveState(): Boolean
    external fun QuickLoadState(): Boolean
    external fun PauseGame()
    external fun ResumeGame()
    external fun TakeScreenshot(path: String)
    external fun Forward(speed: Float)
    external fun getStallCount(): Int
    external fun Mute(mute: Boolean)
    external fun getMemoryBlock(): ArrayList<CoreMemoryBlock>
    external fun writeMem(address: Int, value: Int)
    external fun writeMem8(address: Int, value: Int)
    external fun initSwappy()
    external fun setShader(path: String): Boolean
    external fun getFPS(): Float
    external fun getMemoryRange(address: Int, length: Int): ByteArray?
    external fun nativeMemorySearch(value: Int, size: Int): IntArray?
    external fun resetARDSCheats()
    external fun addARDSCheat(op: Int, valVal: Int)
}


