package org.libsdl.app

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent

object SDLControllerManager {
    @JvmStatic
    external fun nativeSetupJNI(): Int
    @JvmStatic
    external fun nativeAddJoystick(
        device_id: Int, name: String?, desc: String?,
        vendor_id: Int, product_id: Int,
        is_accelerometer: Boolean, button_mask: Int,
        naxes: Int, nhats: Int, nballs: Int
    ): Int

    @JvmStatic
    external fun nativeRemoveJoystick(device_id: Int): Int
    @JvmStatic
    external fun nativeAddHaptic(device_id: Int, name: String?): Int
    @JvmStatic
    external fun nativeRemoveHaptic(device_id: Int): Int
    @JvmStatic
    external fun onNativePadDown(device_id: Int, keycode: Int): Int
    @JvmStatic
    external fun onNativePadUp(device_id: Int, keycode: Int): Int
    @JvmStatic
    external fun onNativeJoy(
        device_id: Int, axis: Int,
        value: Float
    )

    @JvmStatic
    external fun onNativeHat(
        device_id: Int, hat_id: Int,
        x: Int, y: Int
    )

    internal var mJoystickHandler: SDLJoystickHandler? = null
    internal var mHapticHandler: SDLHapticHandler? = null
    private const val TAG = "SDLControllerManager"
    @JvmStatic
    fun initialize() {
        if (mJoystickHandler == null) {
            if (Build.VERSION.SDK_INT >= 19) {
                mJoystickHandler = SDLJoystickHandler_API19()
            } else {
                mJoystickHandler = SDLJoystickHandler_API16()
            }
        }
        if (mHapticHandler == null) {
            if (Build.VERSION.SDK_INT >= 26) {
                mHapticHandler = SDLHapticHandler_API26()
            } else {
                mHapticHandler = SDLHapticHandler()
            }
        }
    }

    // Joystick glue code, just a series of stubs that redirect to the SDLJoystickHandler instance
    @JvmStatic
    fun handleJoystickMotionEvent(event: MotionEvent?): Boolean {
        return mJoystickHandler!!.handleMotionEvent(event)
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun pollInputDevices() {
        mJoystickHandler!!.pollInputDevices()
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun pollHapticDevices() {
        mHapticHandler!!.pollHapticDevices()
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun hapticRun(device_id: Int, intensity: Float, length: Int) {
        mHapticHandler!!.run(device_id, intensity, length)
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun hapticStop(device_id: Int) {
        mHapticHandler!!.stop(device_id)
    }

    // Check if a given device is considered a possible SDL joystick
    @JvmStatic
    fun isDeviceSDLJoystick(deviceId: Int): Boolean {
        val device = InputDevice.getDevice(deviceId)
        // We cannot use InputDevice.isVirtual before API 16, so let's accept
        // only nonnegative device ids (VIRTUAL_KEYBOARD equals -1)
        if (device == null || deviceId < 0) {
            return false
        }
        val sources = device.sources

        /* This is called for every button press, so let's not spam the logs */
        /*
        if ((sources & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            Log.v(TAG, "Input device " + device.getName() + " has class joystick.");
        }
        if ((sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) {
            Log.v(TAG, "Input device " + device.getName() + " is a dpad.");
        }
        if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            Log.v(TAG, "Input device " + device.getName() + " is a gamepad.");
        }
        */return sources and InputDevice.SOURCE_CLASS_JOYSTICK != 0 || sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD || sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
    }
}
