package org.libsdl.app

import android.os.Build
import android.os.VibrationEffect
import android.util.Log

internal class SDLHapticHandler_API26 : SDLHapticHandler() {
    override fun run(device_id: Int, intensity: Float, length: Int) {
        val haptic = getHaptic(device_id)
        if (haptic != null) {
            Log.d("SDL", "Rtest: Vibe with intensity $intensity for $length")
            if (intensity == 0.0f) {
                stop(device_id)
                return
            }
            var vibeValue = Math.round(intensity * 255)
            if (vibeValue > 255) {
                vibeValue = 255
            }
            if (vibeValue < 1) {
                stop(device_id)
                return
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    haptic.vib!!.vibrate(VibrationEffect.createOneShot(length.toLong(), vibeValue))
                }
            } catch (e: Exception) {
                // Fall back to the generic method, which uses DEFAULT_AMPLITUDE, but works even if
                // something went horribly wrong with the Android 8.0 APIs.
                haptic.vib!!.vibrate(length.toLong())
            }
        }
    }
}
