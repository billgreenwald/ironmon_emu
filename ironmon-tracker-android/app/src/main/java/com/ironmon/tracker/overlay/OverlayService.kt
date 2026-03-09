package com.ironmon.tracker.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ironmon.tracker.R
import com.ironmon.tracker.data.models.TrackerState
import com.ironmon.tracker.memory.MemoryClient
import com.ironmon.tracker.memory.MemoryPoller
import com.ironmon.tracker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that draws a Compose overlay on top of mGBA using
 * TYPE_APPLICATION_OVERLAY and WindowManager.
 *
 * Lifecycle:
 *   onCreate  → start MemoryPoller, inflate overlay view
 *   onDestroy → stop poller, remove overlay view
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    // ── Lifecycle / SavedState plumbing needed for ComposeView ────────────────
    private val lifecycleRegistry           = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ── Service internals ─────────────────────────────────────────────────────
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var memoryClient: MemoryClient
    private lateinit var memoryPoller: MemoryPoller

    // Overlay drag state
    private var initialX = 0; private var initialY = 0
    private var initialTouchX = 0f; private var initialTouchY = 0f
    private lateinit var layoutParams: WindowManager.LayoutParams

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        memoryClient = MemoryClient()
        memoryPoller = MemoryPoller(memoryClient)
        memoryPoller.start(serviceScope)

        startForeground(NOTIFICATION_ID, buildNotification())
        attachOverlay()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        memoryPoller.stop()
        memoryClient.close()
        serviceScope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Overlay setup ─────────────────────────────────────────────────────────

    private fun attachOverlay() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 100
        }

        val view = ComposeView(this).also { overlayView = it }
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)

        view.setContent {
            val state by memoryPoller.trackerState.collectAsState()
            OverlayContent(state = state)
        }

        // Touch listener for drag-to-reposition
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX      = layoutParams.x
                    initialY      = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, layoutParams)
    }

    // ── Foreground notification ───────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val channelId = "ironmon_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ironmon Tracker Overlay",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Keeps the tracker overlay running" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ironmon Tracker")
            .setContentText("Overlay running — tap to open settings")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP     = "com.ironmon.tracker.STOP_OVERLAY"
    }
}
