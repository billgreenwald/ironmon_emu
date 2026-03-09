package com.ironmon.tracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironmon.tracker.overlay.OverlayService
import com.ironmon.tracker.ui.theme.IronmonTrackerTheme

/**
 * Entry point. Handles SYSTEM_ALERT_WINDOW permission and starts the overlay.
 *
 * Android 12+ note: The permission intent works the same, but Samsung and MIUI
 * devices may route to a different settings page. If the permission is not
 * granted after returning, show the manual instructions below.
 */
class MainActivity : ComponentActivity() {

    private var canDrawOverlays by mutableStateOf(false)

    // Launcher for the system overlay permission settings page
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-check after returning from system settings
        canDrawOverlays = Settings.canDrawOverlays(this)
        if (canDrawOverlays) startOverlayService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        canDrawOverlays = Settings.canDrawOverlays(this)
        if (canDrawOverlays) startOverlayService()

        setContent {
            IronmonTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text  = "Ironmon Tracker",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = "Overlay for mGBA Android",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(32.dp))

                        if (!canDrawOverlays) {
                            Text(
                                text  = "This app needs \"Draw over other apps\" permission to show the overlay on top of mGBA.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { requestOverlayPermission() }) {
                                Text("Grant Overlay Permission")
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text  = "Samsung / MIUI: If the button doesn't work, go to\nSettings → Apps → Special App Access → Appear on top → Ironmon Tracker",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text  = "Overlay is active. Start mGBA and load a GBA Pokémon ROM.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { stopOverlayService() }) {
                                Text("Stop Overlay")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check in case user granted permission from settings manually
        val hasPermission = Settings.canDrawOverlays(this)
        if (hasPermission && !canDrawOverlays) {
            canDrawOverlays = true
            startOverlayService()
        }
        canDrawOverlays = hasPermission
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun startOverlayService() {
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }
}
