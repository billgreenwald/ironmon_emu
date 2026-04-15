package hh.game.mgba_android.activity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.ds.DSTrackerPanel
import hh.game.mgba_android.tracker.ds.DSTrackerPoller
import hh.game.mgba_android.tracker.quickload.QuickloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.magnum.melonds.MelonDSAndroidInterface
import me.magnum.melonds.MelonEmulator
import me.magnum.melonds.NativeMemBridge
import me.magnum.melonds.common.UriFileHandler
import me.magnum.melonds.common.uridelegates.CompositeUriHandler
import me.magnum.melonds.domain.model.AudioBitrate
import me.magnum.melonds.domain.model.AudioInterpolation
import me.magnum.melonds.domain.model.AudioLatency
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.EmulatorConfiguration
import me.magnum.melonds.domain.model.FirmwareConfiguration
import me.magnum.melonds.domain.model.MicSource
import me.magnum.melonds.domain.model.Rect
import me.magnum.melonds.domain.model.RendererConfiguration
import me.magnum.melonds.domain.model.RuntimeBackground
import me.magnum.melonds.domain.model.VideoFiltering
import me.magnum.melonds.domain.model.VideoRenderer
import me.magnum.melonds.ui.emulator.DSRenderer
import me.magnum.melonds.ui.emulator.EmulatorSurfaceView
import me.magnum.melonds.ui.emulator.model.RuntimeRendererConfiguration
import me.magnum.melonds.ui.emulator.render.ChoreographerFrameRendererFactory
import me.magnum.melonds.ui.emulator.render.FrameRenderCoordinator
import java.io.File
import java.nio.ByteBuffer

class DSEmulatorActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DSEmulatorActivity"

        init {
            System.loadLibrary("melonDS-android-frontend")
        }
    }

    private var frameRenderCoordinator: FrameRenderCoordinator? = null
    private var choreographerRenderer: me.magnum.melonds.ui.emulator.render.ChoreographerFrameRenderer? = null
    private var emulatorSurface: EmulatorSurfaceView? = null
    private var emulatorStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gamepath = intent.getStringExtra("gamepath")
        Log.d(TAG, "onCreate: gamepath=$gamepath")

        if (gamepath.isNullOrBlank()) {
            Log.e(TAG, "No gamepath provided; finishing")
            finish()
            return
        }

        QuickloadManager.register(applicationContext, gamepath)

        // ── Build layout: DS screens (70%) + tracker (30%) ───────────────────────
        val root = RelativeLayout(this)
        root.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        )
        setContentView(root)

        val screenWidthPx = resources.displayMetrics.widthPixels
        val gameWidth = (screenWidthPx * 0.70f).toInt()

        // ── DS emulator surface ────────────────────────────────────────────────────
        val surface = EmulatorSurfaceView(this)
        emulatorSurface = surface
        root.addView(
            surface,
            RelativeLayout.LayoutParams(gameWidth, RelativeLayout.LayoutParams.MATCH_PARENT),
        )

        // ── Tracker panel ─────────────────────────────────────────────────────────
        DSTrackerPoller.start(applicationContext, lifecycleScope)

        val trackerView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by DSTrackerPoller.state.collectAsState()
                DSTrackerPanel(state = state)
            }
        }
        root.addView(
            trackerView,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT,
            ).apply { leftMargin = gameWidth },
        )

        // ── Start melonDS on background thread ────────────────────────────────────
        lifecycleScope.launch(Dispatchers.Default) {
            startMelonDS(gamepath, gameWidth)
        }
    }

    private fun startMelonDS(gamepath: String, gameWidth: Int) {
        try {
            // Set up the URI file handler so native code can open files
            val uriHandler = UriFileHandler(this, CompositeUriHandler(this))
            MelonDSAndroidInterface.setup(uriHandler)

            // Build minimal EmulatorConfiguration (HLE bios, direct boot, DS mode)
            val config = EmulatorConfiguration(
                useCustomBios        = false,
                showBootScreen       = false,
                dsBios7Uri           = null,
                dsBios9Uri           = null,
                dsFirmwareUri        = null,
                dsiBios7Uri          = null,
                dsiBios9Uri          = null,
                dsiFirmwareUri       = null,
                dsiNandUri           = null,
                internalDirectory    = filesDir.absolutePath,
                fastForwardSpeedMultiplier = 2.0f,
                rewindEnabled        = false,
                rewindPeriodSeconds  = 0,
                rewindWindowSeconds  = 0,
                useJit               = true,
                consoleType          = ConsoleType.DS,
                soundEnabled         = true,
                audioInterpolation   = AudioInterpolation.NONE,
                audioBitrate         = AudioBitrate.AUTO,
                volume               = 256,
                audioLatency         = AudioLatency.MEDIUM,
                micSource            = MicSource.NONE,
                firmwareConfiguration = FirmwareConfiguration(
                    nickname            = "Player",
                    message             = "",
                    language            = 1,
                    favouriteColour     = 7,
                    birthdayMonth       = 1,
                    birthdayDay         = 1,
                    randomizeMacAddress = false,
                    internalMacAddress  = null,
                ),
                rendererConfiguration = RendererConfiguration(
                    renderer              = VideoRenderer.SOFTWARE,
                    videoFiltering        = VideoFiltering.NONE,
                    threadedRendering     = false,
                    internalResolutionScaling = 1,
                ),
            )

            // screenshotBuffer needs 256*384*4 bytes (two DS screens stacked = 256×(192+192) RGBA)
            val screenshotBuffer = ByteBuffer.allocateDirect(256 * 384 * 4)
            MelonEmulator.setupEmulator(config, null, screenshotBuffer)

            // Determine save file path alongside the ROM
            val romFile    = File(gamepath)
            val saveFile   = File(romFile.parent, romFile.nameWithoutExtension + ".sav")
            val romUri     = romFile.toUri()
            val sramUri    = saveFile.toUri()

            val loadResult = MelonEmulator.loadRom(
                romUri, sramUri,
                MelonEmulator.GbaSlotType.NONE, null, null,
            )
            Log.d(TAG, "loadRom result: $loadResult")

            if (loadResult.isTerminal) {
                Log.e(TAG, "Fatal ROM load failure: $loadResult")
                return
            }

            // Set up the DS renderer with two screen areas side by side
            // Game area = gameWidth × deviceHeight; left half = top screen, right half = bottom screen
            val screenHeight = resources.displayMetrics.heightPixels
            val halfGameW = gameWidth / 2

            val renderer = DSRenderer(this)
            renderer.setBackground(RuntimeBackground.None)
            renderer.updateRendererConfiguration(
                RuntimeRendererConfiguration(
                    videoFiltering     = VideoFiltering.NONE,
                    resolutionScaling  = 1,
                )
            )
            renderer.updateScreenAreas(
                topScreenRect    = Rect(0, 0, halfGameW, screenHeight),
                bottomScreenRect = Rect(halfGameW, 0, halfGameW, screenHeight),
                topAlpha    = 1f,
                bottomAlpha = 1f,
                topOnTop    = false,
                bottomOnTop = false,
            )

            emulatorSurface?.setRenderer(renderer)

            // Set up render coordinator + Choreographer loop
            val coordinator  = FrameRenderCoordinator()
            frameRenderCoordinator = coordinator

            emulatorSurface?.let { coordinator.addSurface(it) }

            val choreoRenderer = ChoreographerFrameRendererFactory.createFrameRenderer(coordinator)
            choreographerRenderer = choreoRenderer

            MelonEmulator.startEmulation()
            emulatorStarted = true

            // Wire memory bridge so tracker can poll DS RAM
            MemoryBridge.reader = { addr, len ->
                NativeMemBridge.getMemoryRange(addr.toInt(), len)
            }

            // Start Choreographer on main thread
            runOnUiThread {
                choreoRenderer.startRendering()
            }

            Log.d(TAG, "melonDS started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start melonDS", e)
        }
    }

    override fun onDestroy() {
        choreographerRenderer?.stopRendering()
        choreographerRenderer = null

        frameRenderCoordinator?.let { coordinator ->
            emulatorSurface?.let { coordinator.removeSurface(it) }
            coordinator.stop()
        }
        frameRenderCoordinator = null

        if (emulatorStarted) {
            MelonEmulator.stopEmulation()
            MelonDSAndroidInterface.cleanup()
            emulatorStarted = false
        }

        DSTrackerPoller.stop()
        MemoryBridge.reader = null

        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if (emulatorStarted) MelonEmulator.pauseEmulation()
        choreographerRenderer?.stopRendering()
    }

    override fun onResume() {
        super.onResume()
        if (emulatorStarted) MelonEmulator.resumeEmulation()
        choreographerRenderer?.startRendering()
    }
}
