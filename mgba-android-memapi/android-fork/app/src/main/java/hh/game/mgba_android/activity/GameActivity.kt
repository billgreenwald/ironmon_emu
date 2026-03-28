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
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
    private var isMute = false
    private var defaultFps = 60f
    private var secondaryFps = 60f
    private var speedButtonKey = GBAKeys.GBA_KEY_NONE.key
    // Tracker layout state
    private var splitFraction = 0.7f
    private var trackerCollapsible = false
    private var trackerExpanded by mutableStateOf(true)
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
        screenWidthPx = resources.displayMetrics.widthPixels
        val gameWidth = (screenWidthPx * splitFraction).toInt()

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

        val panelFraction = 1f - splitFraction
        val fontScale = panelFraction / 0.3f

        val trackerView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by TrackerPoller.state.collectAsState()
                TrackerPanel(
                    state = state,
                    onQuickload = if (QuickloadManager.canQuickload()) {
                        {
                            Log.d("Quickload", "button tapped, family=${QuickloadManager.currentFamily}")
                            TrackerPoller.manualNextRun()
                            lifecycleScope.launch(Dispatchers.IO) {
                                val nextPath = QuickloadManager.advanceToNext(applicationContext)
                                Log.d("Quickload", "nextPath=$nextPath")
                                if (nextPath != null) {
                                    withContext(Dispatchers.Main) {
                                        Log.d("Quickload", "launching new GameActivity, killing process")
                                        val next = Intent(this@GameActivity, GameActivity::class.java).apply {
                                            putExtra("gamepath", nextPath)
                                            val cheat = this@GameActivity.intent.getStringExtra("cheat")
                                            if (cheat != null) putExtra("cheat", cheat)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        }
                                        startActivity(next)
                                        android.os.Process.killProcess(android.os.Process.myPid())
                                    }
                                } else {
                                    Log.d("Quickload", "nextPath null — no next ROM found")
                                }
                            }
                        }
                    } else null,
                    fontScale = fontScale,
                    isCollapsible = trackerCollapsible,
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
            ).apply { leftMargin = gameWidth },
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
        speedButtonKey = getKey(EmulatorPreferences.getSpeedButton(this))
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
            val options = arrayOf("Shaders", "Memory Tools", "Save State", "Load State", "Cheats", "Sound", "Next Run →")
            AlertDialog.Builder(this)
                .setTitle("Tools")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showShaderMenu()
                        1 -> openHexEditor()
                        2 -> {
                            PauseGame()
                            PopDialogFragment(getString(R.string.savestatetitle))
                                .also {
                                    it.setOnDialogClickListener(object : OnDialogClickListener {
                                        override fun onPostive() {
                                            Toast.makeText(this@GameActivity,
                                                if (QuickSaveState()) getString(R.string.state_saved)
                                                else getString(R.string.state_save_fail),
                                                Toast.LENGTH_SHORT).show()
                                            ResumeGame()
                                        }
                                        override fun onNegative() { ResumeGame() }
                                        override fun onDismiss() { ResumeGame() }
                                    })
                                }
                                .show(supportFragmentManager, "savestate")
                        }
                        3 -> {
                            PauseGame()
                            PopDialogFragment(getString(R.string.loadstatetitle))
                                .also {
                                    it.setOnDialogClickListener(object : OnDialogClickListener {
                                        override fun onPostive() {
                                            Toast.makeText(this@GameActivity,
                                                if (QuickLoadState()) getString(R.string.state_loaded)
                                                else getString(R.string.state_load_fail),
                                                Toast.LENGTH_SHORT).show()
                                            ResumeGame()
                                        }
                                        override fun onNegative() { ResumeGame() }
                                        override fun onDismiss() { ResumeGame() }
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
                        6 -> {
                            if (QuickloadManager.canQuickload()) {
                                TrackerPoller.manualNextRun()
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val nextPath = QuickloadManager.advanceToNext(applicationContext)
                                    if (nextPath != null) {
                                        withContext(Dispatchers.Main) {
                                            val next = Intent(this@GameActivity, GameActivity::class.java).apply {
                                                putExtra("gamepath", nextPath)
                                                val cheat = this@GameActivity.intent.getStringExtra("cheat")
                                                if (cheat != null) putExtra("cheat", cheat)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            }
                                            startActivity(next)
                                            android.os.Process.killProcess(android.os.Process.myPid())
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@GameActivity, "No next ROM found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Quickload not available", Toast.LENGTH_SHORT).show()
                            }
                        }
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
        val src = device.sources
        return (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
               (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }

    private fun hasGamepadConnected(): Boolean =
        InputDevice.getDeviceIds().any { isGamepad(it) }

    private fun updateOnscreenControls() {
        val alwaysShow = EmulatorPreferences.getAlwaysShowControls(this)
        val visible = if (alwaysShow || !hasGamepadConnected()) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.padboardInclude)?.visibility = visible
        findViewById<View>(R.id.tools_btn)?.visibility = visible
    }

    private fun applyTrackerExpansion(expanded: Boolean) {
        trackerExpanded = expanded
        // When collapsed, leave 24dp for the arrow strip so it stays on-screen
        val arrowPx = (24 * resources.displayMetrics.density).toInt()
        val newGameWidth = if (expanded) (screenWidthPx * splitFraction).toInt() else screenWidthPx - arrowPx
        mSurface?.layoutParams?.width = newGameWidth
        mSurface?.requestLayout()
        val tv = trackerViewRef ?: return
        tv.layoutParams = (tv.layoutParams as RelativeLayout.LayoutParams).apply {
            leftMargin = newGameWidth
        }
        tv.requestLayout()
    }

    override fun onInputDeviceAdded(deviceId: Int) { updateOnscreenControls() }
    override fun onInputDeviceRemoved(deviceId: Int) { updateOnscreenControls() }
    override fun onInputDeviceChanged(deviceId: Int) { updateOnscreenControls() }
    // ── End gamepad detection ──────────────────────────────────────────────────

    override fun onDestroy() {
        (getSystemService(INPUT_SERVICE) as InputManager).unregisterInputDeviceListener(this)
        TrackerPoller.stop()
        MemoryBridge.reader = null
        QuickloadManager.unregister()
        super.onDestroy()
        runFPS = false
    }

    override fun onPause() {
        super.onPause()
        resumePending = false
        PauseGame()
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
        // ResumeGame() is deferred to onWindowFocusChanged to avoid racing SDL surface readiness.
        // SDL requires mHasFocus=true (set on focus grant) before nativeResume() unblocks the
        // render thread. Calling ResumeGame() here would wake the mGBA core before SDL is ready.
        resumePending = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && resumePending) {
            resumePending = false
            ResumeGame()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
//        Game controler
        var handled = false
        var gbaKey = getKey(event.keyCode)
        if (gbaKey != GBAKeys.GBA_KEY_NONE.key) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    onNativeKeyDown(gbaKey)
                    if (gbaKey == speedButtonKey && secondaryFps != defaultFps) { setFPS = secondaryFps; Forward(secondaryFps) }
                    handled = true
                }

                KeyEvent.ACTION_UP -> {
                    onNativeKeyUp(gbaKey)
                    if (gbaKey == speedButtonKey && secondaryFps != defaultFps) { setFPS = defaultFps; Forward(defaultFps) }
                    handled = true
                }

            }
        }
        return handled || super.dispatchKeyEvent(event)
    }

    private fun addGameControler() {
        findViewById<ImageView>(R.id.rBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.lBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.aBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.bBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.selectBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.startBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.upBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.downBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.leftBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.rightBtn).setGBAKeyListener()
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

    private fun View.setGBAKeyListener() {
        var keyText = getKey(
            when (this.id) {
                R.id.upBtn -> "up"
                R.id.downBtn -> "down"
                R.id.leftBtn -> "left"
                R.id.rightBtn -> "right"
                R.id.rBtn -> "R"
                R.id.lBtn -> "L"
                R.id.aBtn -> "A"
                R.id.bBtn -> "B"
                R.id.selectBtn -> "select"
                R.id.startBtn -> "start"
                else -> ""
            }
        )
        this.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onNativeKeyDown(keyText)
                    if (keyText == speedButtonKey && secondaryFps != defaultFps) { setFPS = secondaryFps; Forward(secondaryFps) }
                }

                MotionEvent.ACTION_UP -> {
                    onNativeKeyUp(keyText)
                    if (keyText == speedButtonKey && secondaryFps != defaultFps) { setFPS = defaultFps; Forward(defaultFps) }
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


