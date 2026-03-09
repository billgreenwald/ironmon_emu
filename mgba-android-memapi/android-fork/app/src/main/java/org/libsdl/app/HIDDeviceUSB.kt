package org.libsdl.app

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.os.Build
import android.util.Log
import java.util.Arrays

internal class HIDDeviceUSB(manager: HIDDeviceManager, usbDevice: UsbDevice, interface_index: Int) :
    HIDDevice {
    protected var mManager: HIDDeviceManager?
    override lateinit var device: UsbDevice
        protected set
    protected var mInterfaceIndex: Int
    protected var mInterface: Int
    override var id: Int = 0
        protected set
    protected var mConnection: UsbDeviceConnection? = null
    protected var mInputEndpoint: UsbEndpoint? = null
    protected var mOutputEndpoint: UsbEndpoint? = null
    protected var mInputThread: InputThread? = null
    protected var mRunning: Boolean
    protected var mFrozen = false

    init {
        mManager = manager
        device = usbDevice
        mInterfaceIndex = interface_index
        mInterface = device.getInterface(mInterfaceIndex).id
        id = manager.getDeviceIDForIdentifier(identifier)
        mRunning = false
    }

    val identifier: String
        get() = String.format(
            "%s/%x/%x/%d",
            device.deviceName,
            device.vendorId,
            device.productId,
            mInterfaceIndex
        )
    override val vendorId: Int
        get() = device.vendorId
    override val productId: Int
        get() = device.productId
    override val serialNumber: String
        get() {
            var result: String? = null
            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    result = device.serialNumber
                } catch (exception: SecurityException) {
                    //Log.w(TAG, "App permissions mean we cannot get serial number for device " + getDeviceName() + " message: " + exception.getMessage());
                }
            }
            if (result == null) {
                result = ""
            }
            return result
        }
    override val version: Int
        get() = 0
    override val manufacturerName: String
        get() {
            var result: String? = null
            if (Build.VERSION.SDK_INT >= 21) {
                result = device.manufacturerName
            }
            if (result == null) {
                result = String.format("%x", vendorId)
            }
            return result
        }
    override val productName: String
        get() {
            var result: String? = null
            if (Build.VERSION.SDK_INT >= 21) {
                result = device.productName
            }
            if (result == null) {
                result = String.format("%x", productId)
            }
            return result
        }
    val deviceName: String
        get() = manufacturerName + " " + productName + "(0x" + String.format(
            "%x",
            vendorId
        ) + "/0x" + String.format("%x", productId) + ")"

    override fun open(): Boolean {
        mConnection = mManager!!.uSBManager!!.openDevice(device)
        if (mConnection == null) {
            Log.w(TAG, "Unable to open USB device " + deviceName)
            return false
        }

        // Force claim our interface
        val iface = device.getInterface(mInterfaceIndex)
        if (!mConnection!!.claimInterface(iface, true)) {
            Log.w(TAG, "Failed to claim interfaces on USB device " + deviceName)
            close()
            return false
        }

        // Find the endpoints
        for (j in 0 until iface.endpointCount) {
            val endpt = iface.getEndpoint(j)
            when (endpt.direction) {
                UsbConstants.USB_DIR_IN -> if (mInputEndpoint == null) {
                    mInputEndpoint = endpt
                }

                UsbConstants.USB_DIR_OUT -> if (mOutputEndpoint == null) {
                    mOutputEndpoint = endpt
                }
            }
        }

        // Make sure the required endpoints were present
        if (mInputEndpoint == null || mOutputEndpoint == null) {
            Log.w(TAG, "Missing required endpoint on USB device " + deviceName)
            close()
            return false
        }

        // Start listening for input
        mRunning = true
        mInputThread = InputThread()
        mInputThread!!.start()
        return true
    }

    override fun sendFeatureReport(report: ByteArray?): Int {
        var res = -1
        var offset = 0
        var length = report!!.size
        var skipped_report_id = false
        val report_number = report[0]
        if (report_number.toInt() == 0x0) {
            ++offset
            --length
            skipped_report_id = true
        }
        res = mConnection!!.controlTransfer(
            UsbConstants.USB_TYPE_CLASS or 0x01 /*RECIPIENT_INTERFACE*/ or UsbConstants.USB_DIR_OUT,
            0x09 /*HID set_report*/,
            3 /*HID feature*/ shl 8 or report_number.toInt(),
            mInterface,
            report, offset, length,
            1000 /*timeout millis*/
        )
        if (res < 0) {
            Log.w(TAG, "sendFeatureReport() returned " + res + " on device " + deviceName)
            return -1
        }
        if (skipped_report_id) {
            ++length
        }
        return length
    }

    override fun sendOutputReport(report: ByteArray?): Int {
        val r = mConnection!!.bulkTransfer(mOutputEndpoint, report, report!!.size, 1000)
        if (r != report.size) {
            Log.w(TAG, "sendOutputReport() returned " + r + " on device " + deviceName)
        }
        return r
    }

    override fun getFeatureReport(report: ByteArray?): Boolean {
        var res = -1
        var offset = 0
        var length = report!!.size
        var skipped_report_id = false
        val report_number = report[0]
        if (report_number.toInt() == 0x0) {
            /* Offset the return buffer by 1, so that the report ID
               will remain in byte 0. */
            ++offset
            --length
            skipped_report_id = true
        }
        res = mConnection!!.controlTransfer(
            UsbConstants.USB_TYPE_CLASS or 0x01 /*RECIPIENT_INTERFACE*/ or UsbConstants.USB_DIR_IN,
            0x01 /*HID get_report*/,
            3 /*HID feature*/ shl 8 or report_number.toInt(),
            mInterface,
            report, offset, length,
            1000 /*timeout millis*/
        )
        if (res < 0) {
            Log.w(TAG, "getFeatureReport() returned " + res + " on device " + deviceName)
            return false
        }
        if (skipped_report_id) {
            ++res
            ++length
        }
        val data: ByteArray?
        data = if (res == length) {
            report
        } else {
            Arrays.copyOfRange(report, 0, res)
        }
        mManager!!.HIDDeviceFeatureReport(id, data)
        return true
    }

    override fun close() {
        mRunning = false
        if (mInputThread != null) {
            while (mInputThread!!.isAlive) {
                mInputThread!!.interrupt()
                try {
                    mInputThread!!.join()
                } catch (e: InterruptedException) {
                    // Keep trying until we're done
                }
            }
            mInputThread = null
        }
        if (mConnection != null) {
            val iface = device.getInterface(mInterfaceIndex)
            mConnection!!.releaseInterface(iface)
            mConnection!!.close()
            mConnection = null
        }
    }

    override fun shutdown() {
        close()
        mManager = null
    }

    override fun setFrozen(frozen: Boolean) {
        mFrozen = frozen
    }

    protected inner class InputThread : Thread() {
        override fun run() {
            val packetSize = mInputEndpoint!!.maxPacketSize
            val packet = ByteArray(packetSize)
            while (mRunning) {
                var r: Int
                r = try {
                    mConnection!!.bulkTransfer(mInputEndpoint, packet, packetSize, 1000)
                } catch (e: Exception) {
                    Log.v(TAG, "Exception in UsbDeviceConnection bulktransfer: $e")
                    break
                }
                if (r < 0) {
                    // Could be a timeout or an I/O error
                }
                if (r > 0) {
                    var data: ByteArray?
                    data = if (r == packetSize) {
                        packet
                    } else {
                        Arrays.copyOfRange(packet, 0, r)
                    }
                    if (!mFrozen) {
                        mManager!!.HIDDeviceInputReport(id.toInt(), data)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "hidapi"
    }
}
