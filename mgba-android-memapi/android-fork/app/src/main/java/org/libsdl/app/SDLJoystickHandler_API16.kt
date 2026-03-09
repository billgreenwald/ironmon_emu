package org.libsdl.app

import android.view.InputDevice
import android.view.InputDevice.MotionRange
import android.view.MotionEvent
import org.libsdl.app.SDLControllerManager.isDeviceSDLJoystick
import org.libsdl.app.SDLControllerManager.nativeAddJoystick
import org.libsdl.app.SDLControllerManager.nativeRemoveJoystick
import org.libsdl.app.SDLControllerManager.onNativeHat
import org.libsdl.app.SDLControllerManager.onNativeJoy
import java.util.Collections

/* Actual joystick functionality available for API >= 12 devices */
internal open class SDLJoystickHandler_API16 : SDLJoystickHandler() {
    internal class SDLJoystick {
        var device_id = 0
        var name: String? = null
        var desc: String? = null
        var axes: ArrayList<MotionRange>? = null
        var hats: ArrayList<MotionRange>? = null
    }

    internal class RangeComparator : Comparator<MotionRange> {
        override fun compare(arg0: MotionRange, arg1: MotionRange): Int {
            // Some controllers, like the Moga Pro 2, return AXIS_GAS (22) for right trigger and AXIS_BRAKE (23) for left trigger - swap them so they're sorted in the right order for SDL
            var arg0Axis = arg0.axis
            var arg1Axis = arg1.axis
            if (arg0Axis == MotionEvent.AXIS_GAS) {
                arg0Axis = MotionEvent.AXIS_BRAKE
            } else if (arg0Axis == MotionEvent.AXIS_BRAKE) {
                arg0Axis = MotionEvent.AXIS_GAS
            }
            if (arg1Axis == MotionEvent.AXIS_GAS) {
                arg1Axis = MotionEvent.AXIS_BRAKE
            } else if (arg1Axis == MotionEvent.AXIS_BRAKE) {
                arg1Axis = MotionEvent.AXIS_GAS
            }

            // Make sure the AXIS_Z is sorted between AXIS_RY and AXIS_RZ.
            // This is because the usual pairing are:
            // - AXIS_X + AXIS_Y (left stick).
            // - AXIS_RX, AXIS_RY (sometimes the right stick, sometimes triggers).
            // - AXIS_Z, AXIS_RZ (sometimes the right stick, sometimes triggers).
            // This sorts the axes in the above order, which tends to be correct
            // for Xbox-ish game pads that have the right stick on RX/RY and the
            // triggers on Z/RZ.
            //
            // Gamepads that don't have AXIS_Z/AXIS_RZ but use
            // AXIS_LTRIGGER/AXIS_RTRIGGER are unaffected by this.
            //
            // References:
            // - https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input
            // - https://www.kernel.org/doc/html/latest/input/gamepad.html
            if (arg0Axis == MotionEvent.AXIS_Z) {
                arg0Axis = MotionEvent.AXIS_RZ - 1
            } else if (arg0Axis > MotionEvent.AXIS_Z && arg0Axis < MotionEvent.AXIS_RZ) {
                --arg0Axis
            }
            if (arg1Axis == MotionEvent.AXIS_Z) {
                arg1Axis = MotionEvent.AXIS_RZ - 1
            } else if (arg1Axis > MotionEvent.AXIS_Z && arg1Axis < MotionEvent.AXIS_RZ) {
                --arg1Axis
            }
            return arg0Axis - arg1Axis
        }
    }

    private val mJoysticks: ArrayList<SDLJoystick>

    init {
        mJoysticks = ArrayList()
    }

    override fun pollInputDevices() {
        val deviceIds = InputDevice.getDeviceIds()
        for (device_id in deviceIds) {
            if (isDeviceSDLJoystick(device_id)) {
                var joystick = getJoystick(device_id)
                if (joystick == null) {
                    val joystickDevice = InputDevice.getDevice(device_id) ?: continue
                    joystick = SDLJoystick()
                    joystick.device_id = device_id
                    joystick.name = joystickDevice.name
                    joystick.desc = getJoystickDescriptor(joystickDevice)
                    joystick.axes = ArrayList()
                    joystick.hats = ArrayList()
                    val ranges = joystickDevice.motionRanges
                    Collections.sort(ranges, RangeComparator())
                    for (range in ranges) {
                        if (range.source and InputDevice.SOURCE_CLASS_JOYSTICK != 0) {
                            if (range.axis == MotionEvent.AXIS_HAT_X || range.axis == MotionEvent.AXIS_HAT_Y) {
                                joystick.hats!!.add(range)
                            } else {
                                joystick.axes!!.add(range)
                            }
                        }
                    }
                    mJoysticks.add(joystick)
                    nativeAddJoystick(
                        joystick.device_id,
                        joystick.name,
                        joystick.desc,
                        getVendorId(joystickDevice),
                        getProductId(joystickDevice),
                        false,
                        getButtonMask(joystickDevice),
                        joystick.axes!!.size,
                        joystick.hats!!.size / 2,
                        0
                    )
                }
            }
        }

        /* Check removed devices */
        var removedDevices: ArrayList<Int>? = null
        for (joystick in mJoysticks) {
            val device_id = joystick.device_id
            var i: Int
            i = 0
            while (i < deviceIds.size) {
                if (device_id == deviceIds[i]) break
                i++
            }
            if (i == deviceIds.size) {
                if (removedDevices == null) {
                    removedDevices = ArrayList()
                }
                removedDevices.add(device_id)
            }
        }
        if (removedDevices != null) {
            for (device_id in removedDevices) {
                nativeRemoveJoystick(device_id)
                for (i in mJoysticks.indices) {
                    if (mJoysticks[i].device_id == device_id) {
                        mJoysticks.removeAt(i)
                        break
                    }
                }
            }
        }
    }

    protected fun getJoystick(device_id: Int): SDLJoystick? {
        for (joystick in mJoysticks) {
            if (joystick.device_id == device_id) {
                return joystick
            }
        }
        return null
    }

    override fun handleMotionEvent(event: MotionEvent?): Boolean {
        val actionPointerIndex = event!!.actionIndex
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_MOVE) {
            val joystick = getJoystick(event.deviceId)
            if (joystick != null) {
                for (i in joystick.axes!!.indices) {
                    val range = joystick.axes!![i]
                    /* Normalize the value to -1...1 */
                    val value = (event.getAxisValue(
                        range.axis,
                        actionPointerIndex
                    ) - range.min) / range.range * 2.0f - 1.0f
                    onNativeJoy(joystick.device_id, i, value)
                }
                for (i in 0 until joystick.hats!!.size / 2) {
                    val hatX = Math.round(
                        event.getAxisValue(
                            joystick.hats!![2 * i].axis,
                            actionPointerIndex
                        )
                    )
                    val hatY = Math.round(
                        event.getAxisValue(
                            joystick.hats!![2 * i + 1].axis,
                            actionPointerIndex
                        )
                    )
                    onNativeHat(joystick.device_id, i, hatX, hatY)
                }
            }
        }
        return true
    }

    fun getJoystickDescriptor(joystickDevice: InputDevice): String {
        val desc = joystickDevice.descriptor
        return if (desc != null && !desc.isEmpty()) {
            desc
        } else joystickDevice.name
    }

    open fun getProductId(joystickDevice: InputDevice?): Int {
        return 0
    }

    open fun getVendorId(joystickDevice: InputDevice?): Int {
        return 0
    }

    open fun getButtonMask(joystickDevice: InputDevice?): Int {
        return -1
    }
}
