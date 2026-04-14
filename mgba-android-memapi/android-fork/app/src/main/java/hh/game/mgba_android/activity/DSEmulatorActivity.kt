package hh.game.mgba_android.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.ds.DSTrackerPanel
import hh.game.mgba_android.tracker.ds.DSTrackerPoller
import hh.game.mgba_android.tracker.quickload.QuickloadManager

/**
 * Activity that hosts a Nintendo DS ROM via melonDS.
 *
 * INTEGRATION STATUS: Routing and tracker data layer are wired up.
 * The actual melonDS renderer (SurfaceView + OpenGL) must be embedded here
 * in a follow-up step — see feature/ds-kaizo-tracker plan for details.
 *
 * When the melonDS renderer is integrated:
 *   1. Initialize melonDS JNI (MelonEmulator.setupEmulator / loadRom)
 *   2. Replace the placeholder SurfaceView with EmulatorSurfaceView from melonDS-android
 *   3. Remove the TODO label below
 *   4. Set MemoryBridge.reader = { addr, len -> NativeMemBridge.getMemoryRange(addr, len) }
 *      (currently set to null until core is ready)
 */
class DSEmulatorActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DSEmulatorActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gamepath = intent.getStringExtra("gamepath")
        Log.d(TAG, "onCreate: gamepath=$gamepath")

        if (gamepath != null) {
            QuickloadManager.register(applicationContext, gamepath)
        }

        // ── Layout: game surface (left 70%) + tracker panel (right 30%) ──────────
        val root = RelativeLayout(this)
        root.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        )
        setContentView(root)

        val screenWidthPx = resources.displayMetrics.widthPixels
        val gameWidth = (screenWidthPx * 0.70f).toInt()
        val trackerLeft = gameWidth

        // Placeholder for the melonDS renderer surface
        // TODO: Replace with melonDS EmulatorSurfaceView + renderer integration
        val gameSurface = FrameLayout(this).apply {
            id = View.generateViewId()
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        val placeholderLabel = TextView(this).apply {
            text = "DS Emulator\n(renderer integration pending)\n\n${gamepath?.substringAfterLast('/') ?: ""}"
            setTextColor(android.graphics.Color.WHITE)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(32, 32, 32, 32)
        }
        gameSurface.addView(
            placeholderLabel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER,
            )
        )
        root.addView(
            gameSurface,
            RelativeLayout.LayoutParams(gameWidth, RelativeLayout.LayoutParams.MATCH_PARENT),
        )

        // ── Tracker panel ─────────────────────────────────────────────────────────
        // MemoryBridge.reader will be set here once the melonDS core is running.
        // DSTrackerPoller handles null reader gracefully (emits Disconnected state).
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
            ).apply { leftMargin = trackerLeft },
        )
    }

    override fun onDestroy() {
        DSTrackerPoller.stop()
        MemoryBridge.reader = null
        super.onDestroy()
    }
}
