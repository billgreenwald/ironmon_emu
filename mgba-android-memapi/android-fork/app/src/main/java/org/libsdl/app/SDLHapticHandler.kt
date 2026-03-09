package org.libsdl.app

import android.content.Context
import android.os.Vibrator
import android.view.InputDevice
import org.libsdl.app.SDL.getContext
import org.libsdl.app.SDLControllerManager.nativeAddHaptic
import org.libsdl.app.SDLControllerManager.nativeRemoveHaptic

internal open class SDLHapticHandler {
    internal class SDLHaptic {
        var device_id = 0
        var name: String? = null
        @JvmField
        var vib: Vibrator? = null
    }

    private val mHaptics: ArrayList<SDLHaptic>

    init {
        mHaptics = ArrayList()
    }

    open fun run(device_id: Int, intensity: Float, length: Int) {
        val haptic = getHaptic(device_id)
        if (haptic != null) {
            haptic.vib!!.vibrate(length.toLong())
        }
    }

    fun stop(device_id: Int) {
        val haptic = getHaptic(device_id)
        if (haptic != null) {
            haptic.vib!!.cancel()
        }
    }

    fun pollHapticDevices() {
        val deviceId_VIBRATOR_SERVICE = 999999
        var hasVibratorService = false
        val deviceIds = InputDevice.getDeviceIds()
        // It helps processing the device ids in reverse order
        // For example, in the case of the XBox 360 wireless dongle,
        // so the first controller seen by SDL matches what the receiver
        // considers to be the first controller
        for (i in deviceIds.size - 1 downTo -1 + 1) {
            var haptic = getHaptic(deviceIds[i])
            if (haptic == null) {
                val device = InputDevice.getDevice(deviceIds[i]) ?: continue
                val vib = device.vibrator
                if (vib.hasVibrator()) {
                    haptic = SDLHaptic()
                    haptic.device_id = deviceIds[i]
                    haptic.name = device.name
                    haptic.vib = vib
                    mHaptics.add(haptic)
                    nativeAddHaptic(haptic.device_id, haptic.name)
                }
            }
        }

        /* Check VIBRATOR_SERVICE */
        val vib = getContext()!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vib != null) {
            hasVibratorService = vib.hasVibrator()
            if (hasVibratorService) {
                var haptic = getHaptic(deviceId_VIBRATOR_SERVICE)
                if (haptic == null) {
                    haptic = SDLHaptic()
                    haptic.device_id = deviceId_VIBRATOR_SERVICE
                    haptic.name = "VIBRATOR_SERVICE"
                    haptic.vib = vib
                    mHaptics.add(haptic)
                    nativeAddHaptic(haptic.device_id, haptic.name)
                }
            }
        }

        /* Check removed devices */
        var removedDevices: ArrayList<Int>? = null
        for (haptic in mHaptics) {
            val device_id = haptic.device_id
            var i: Int
            i = 0
            while (i < deviceIds.size) {
                if (device_id == deviceIds[i]) break
                i++
            }
            if (device_id != deviceId_VIBRATOR_SERVICE || !hasVibratorService) {
                if (i == deviceIds.size) {
                    if (removedDevices == null) {
                        removedDevices = ArrayList()
                    }
                    removedDevices.add(device_id)
                }
            } // else: don't remove the vibrator if it is still present
        }
        if (removedDevices != null) {
            for (device_id in removedDevices) {
                nativeRemoveHaptic(device_id)
                for (i in mHaptics.indices) {
                    if (mHaptics[i].device_id == device_id) {
                        mHaptics.removeAt(i)
                        break
                    }
                }
            }
        }
    }

    protected fun getHaptic(device_id: Int): SDLHaptic? {
        for (haptic in mHaptics) {
            if (haptic.device_id == device_id) {
                return haptic
            }
        }
        return null
    }
}
