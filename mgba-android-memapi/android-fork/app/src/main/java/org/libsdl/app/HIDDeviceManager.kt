package org.libsdl.app

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat

class HIDDeviceManager private constructor(val context: Context) {
    private val mDevicesById = HashMap<Int, HIDDevice>()
    private val mBluetoothDevices = HashMap<BluetoothDevice?, HIDDeviceBLESteamController>()
    private var mNextDeviceId = 0
    private var mSharedPreferences: SharedPreferences? = null
    private var mIsChromebook = false
    var uSBManager: UsbManager? = null
        private set
    private var mHandler: Handler? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mLastBluetoothDevices: List<BluetoothDevice>? = null
    private val mUsbBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                handleUsbDeviceAttached(usbDevice)
            } else if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                handleUsbDeviceDetached(usbDevice)
            } else if (action == ACTION_USB_PERMISSION) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                handleUsbDevicePermission(
                    usbDevice,
                    intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                )
            }
        }
    }
    private val mBluetoothBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // Bluetooth device was connected. If it was a Steam Controller, handle it
            if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Bluetooth device connected: $device")
                if (isSteamController(device)) {
                    connectBluetoothDevice(device)
                }
            }

            // Bluetooth device was disconnected, remove from controller manager (if any)
            if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Bluetooth device disconnected: $device")
                disconnectBluetoothDevice(device)
            }
        }
    }

    init {
        HIDDeviceRegisterCallback()
        mSharedPreferences = context.getSharedPreferences("hidapi", Context.MODE_PRIVATE)
        mIsChromebook =
            context.packageManager.hasSystemFeature("org.chromium.arc.device_management")

//        if (shouldClear) {
//            SharedPreferences.Editor spedit = mSharedPreferences.edit();
//            spedit.clear();
//            spedit.commit();
//        }
//        else
        run { mNextDeviceId = mSharedPreferences!!.getInt("next_device_id", 0) }
    }

    fun getDeviceIDForIdentifier(identifier: String?): Int {
        val spedit = mSharedPreferences!!.edit()
        var result = mSharedPreferences!!.getInt(identifier, 0)
        if (result == 0) {
            result = mNextDeviceId++
            spedit.putInt("next_device_id", mNextDeviceId)
        }
        spedit.putInt(identifier, result)
        spedit.commit()
        return result
    }

    private fun initializeUSB() {
        uSBManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        if (uSBManager == null) {
            return
        }

        /*
        // Logging
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            Log.i(TAG,"Path: " + device.getDeviceName());
            Log.i(TAG,"Manufacturer: " + device.getManufacturerName());
            Log.i(TAG,"Product: " + device.getProductName());
            Log.i(TAG,"ID: " + device.getDeviceId());
            Log.i(TAG,"Class: " + device.getDeviceClass());
            Log.i(TAG,"Protocol: " + device.getDeviceProtocol());
            Log.i(TAG,"Vendor ID " + device.getVendorId());
            Log.i(TAG,"Product ID: " + device.getProductId());
            Log.i(TAG,"Interface count: " + device.getInterfaceCount());
            Log.i(TAG,"---------------------------------------");

            // Get interface details
            for (int index = 0; index < device.getInterfaceCount(); index++) {
                UsbInterface mUsbInterface = device.getInterface(index);
                Log.i(TAG,"  *****     *****");
                Log.i(TAG,"  Interface index: " + index);
                Log.i(TAG,"  Interface ID: " + mUsbInterface.getId());
                Log.i(TAG,"  Interface class: " + mUsbInterface.getInterfaceClass());
                Log.i(TAG,"  Interface subclass: " + mUsbInterface.getInterfaceSubclass());
                Log.i(TAG,"  Interface protocol: " + mUsbInterface.getInterfaceProtocol());
                Log.i(TAG,"  Endpoint count: " + mUsbInterface.getEndpointCount());

                // Get endpoint details 
                for (int epi = 0; epi < mUsbInterface.getEndpointCount(); epi++)
                {
                    UsbEndpoint mEndpoint = mUsbInterface.getEndpoint(epi);
                    Log.i(TAG,"    ++++   ++++   ++++");
                    Log.i(TAG,"    Endpoint index: " + epi);
                    Log.i(TAG,"    Attributes: " + mEndpoint.getAttributes());
                    Log.i(TAG,"    Direction: " + mEndpoint.getDirection());
                    Log.i(TAG,"    Number: " + mEndpoint.getEndpointNumber());
                    Log.i(TAG,"    Interval: " + mEndpoint.getInterval());
                    Log.i(TAG,"    Packet size: " + mEndpoint.getMaxPacketSize());
                    Log.i(TAG,"    Type: " + mEndpoint.getType());
                }
            }
        }
        Log.i(TAG," No more devices connected.");
        */

        // Register for USB broadcasts and permission completions
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(ACTION_USB_PERMISSION)
        context.registerReceiver(mUsbBroadcast, filter)
        for (usbDevice in uSBManager!!.deviceList.values) {
            handleUsbDeviceAttached(usbDevice)
        }
    }

    private fun shutdownUSB() {
        try {
            context.unregisterReceiver(mUsbBroadcast)
        } catch (e: Exception) {
            // We may not have registered, that's okay
        }
    }

    private fun isHIDDeviceInterface(usbDevice: UsbDevice?, usbInterface: UsbInterface): Boolean {
        if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID) {
            return true
        }
        return if (isXbox360Controller(usbDevice, usbInterface) || isXboxOneController(
                usbDevice,
                usbInterface
            )
        ) {
            true
        } else false
    }

    private fun isXbox360Controller(usbDevice: UsbDevice?, usbInterface: UsbInterface): Boolean {
        val XB360_IFACE_SUBCLASS = 93
        val XB360_IFACE_PROTOCOL = 1 // Wired
        val XB360W_IFACE_PROTOCOL = 129 // Wireless
        val SUPPORTED_VENDORS = intArrayOf(
            0x0079,  // GPD Win 2
            0x044f,  // Thrustmaster
            0x045e,  // Microsoft
            0x046d,  // Logitech
            0x056e,  // Elecom
            0x06a3,  // Saitek
            0x0738,  // Mad Catz
            0x07ff,  // Mad Catz
            0x0e6f,  // PDP
            0x0f0d,  // Hori
            0x1038,  // SteelSeries
            0x11c9,  // Nacon
            0x12ab,  // Unknown
            0x1430,  // RedOctane
            0x146b,  // BigBen
            0x1532,  // Razer Sabertooth
            0x15e4,  // Numark
            0x162e,  // Joytech
            0x1689,  // Razer Onza
            0x1949,  // Lab126, Inc.
            0x1bad,  // Harmonix
            0x20d6,  // PowerA
            0x24c6,  // PowerA
            0x2c22
        )
        if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC && usbInterface.interfaceSubclass == XB360_IFACE_SUBCLASS &&
            (usbInterface.interfaceProtocol == XB360_IFACE_PROTOCOL ||
                    usbInterface.interfaceProtocol == XB360W_IFACE_PROTOCOL)
        ) {
            val vendor_id = usbDevice!!.vendorId
            for (supportedVid in SUPPORTED_VENDORS) {
                if (vendor_id == supportedVid) {
                    return true
                }
            }
        }
        return false
    }

    private fun isXboxOneController(usbDevice: UsbDevice?, usbInterface: UsbInterface): Boolean {
        val XB1_IFACE_SUBCLASS = 71
        val XB1_IFACE_PROTOCOL = 208
        val SUPPORTED_VENDORS = intArrayOf(
            0x045e,  // Microsoft
            0x0738,  // Mad Catz
            0x0e6f,  // PDP
            0x0f0d,  // Hori
            0x1532,  // Razer Wildcat
            0x20d6,  // PowerA
            0x24c6,  // PowerA
            0x2dc8,  /* 8BitDo */
            0x2e24
        )
        if (usbInterface.id == 0 && usbInterface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC && usbInterface.interfaceSubclass == XB1_IFACE_SUBCLASS && usbInterface.interfaceProtocol == XB1_IFACE_PROTOCOL) {
            val vendor_id = usbDevice!!.vendorId
            for (supportedVid in SUPPORTED_VENDORS) {
                if (vendor_id == supportedVid) {
                    return true
                }
            }
        }
        return false
    }

    private fun handleUsbDeviceAttached(usbDevice: UsbDevice?) {
        connectHIDDeviceUSB(usbDevice)
    }

    private fun handleUsbDeviceDetached(usbDevice: UsbDevice?) {
        val devices: MutableList<Int> = ArrayList()
        for (device in mDevicesById.values) {
            if (usbDevice == device.device) {
                devices.add(device.id)
            }
        }
        for (id in devices) {
            val device = mDevicesById[id]
            mDevicesById.remove(id)
            device!!.shutdown()
            HIDDeviceDisconnected(id)
        }
    }

    private fun handleUsbDevicePermission(usbDevice: UsbDevice?, permission_granted: Boolean) {
        for (device in mDevicesById.values) {
            if (usbDevice == device.device) {
                var opened = false
                if (permission_granted) {
                    opened = device.open()
                }
                HIDDeviceOpenResult(device.id, opened)
            }
        }
    }

    private fun connectHIDDeviceUSB(usbDevice: UsbDevice?) {
        synchronized(this) {
            var interface_mask = 0
            for (interface_index in 0 until usbDevice!!.interfaceCount) {
                val usbInterface = usbDevice.getInterface(interface_index)
                if (isHIDDeviceInterface(usbDevice, usbInterface)) {
                    // Check to see if we've already added this interface
                    // This happens with the Xbox Series X controller which has a duplicate interface 0, which is inactive
                    val interface_id = usbInterface.id
                    if (interface_mask and (1 shl interface_id) != 0) {
                        continue
                    }
                    interface_mask = interface_mask or (1 shl interface_id)
                    val device = HIDDeviceUSB(this, usbDevice, interface_index)
                    val id = device.id
                    mDevicesById[id] = device
                    HIDDeviceConnected(
                        id,
                        device.identifier,
                        device.vendorId,
                        device.productId,
                        device.serialNumber,
                        device.version,
                        device.manufacturerName,
                        device.productName,
                        usbInterface.id,
                        usbInterface.interfaceClass,
                        usbInterface.interfaceSubclass,
                        usbInterface.interfaceProtocol
                    )
                }
            }
        }
    }

    private fun initializeBluetooth() {
        Log.d(TAG, "Initializing Bluetooth")
        if (Build.VERSION.SDK_INT <= 30 &&
            context.packageManager.checkPermission(
                Manifest.permission.BLUETOOTH,
                context.packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Couldn't initialize Bluetooth, missing android.permission.BLUETOOTH")
            return
        }
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) || Build.VERSION.SDK_INT < 18) {
            Log.d(
                TAG,
                "Couldn't initialize Bluetooth, this version of Android does not support Bluetooth LE"
            )
            return
        }

        // Find bonded bluetooth controllers and create SteamControllers for them
        mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (mBluetoothManager == null) {
            // This device doesn't support Bluetooth.
            return
        }
        val btAdapter = mBluetoothManager!!.adapter
            ?: // This device has Bluetooth support in the codebase, but has no available adapters.
            return

        // Get our bonded devices.
        for (device in btAdapter.bondedDevices) {
            Log.d(TAG, "Bluetooth device available: $device")
            if (isSteamController(device)) {
                connectBluetoothDevice(device)
            }
        }

        // NOTE: These don't work on Chromebooks, to my undying dismay.
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        context.registerReceiver(mBluetoothBroadcast, filter)
        if (mIsChromebook) {
            mHandler = Handler(Looper.getMainLooper())
            mLastBluetoothDevices = ArrayList()

            // final HIDDeviceManager finalThis = this;
            // mHandler.postDelayed(new Runnable() {
            //     @Override
            //     public void run() {
            //         finalThis.chromebookConnectionHandler();
            //     }
            // }, 5000);
        }
    }

    private fun shutdownBluetooth() {
        try {
            context.unregisterReceiver(mBluetoothBroadcast)
        } catch (e: Exception) {
            // We may not have registered, that's okay
        }
    }

    // Chromebooks do not pass along ACTION_ACL_CONNECTED / ACTION_ACL_DISCONNECTED properly.
    // This function provides a sort of dummy version of that, watching for changes in the
    // connected devices and attempting to add controllers as things change.
    fun chromebookConnectionHandler() {
        if (!mIsChromebook) {
            return
        }
        val disconnected = ArrayList<BluetoothDevice>()
        val connected = ArrayList<BluetoothDevice>()
         if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val currentConnected =   mBluetoothManager!!.getConnectedDevices(BluetoothProfile.GATT)
        for (bluetoothDevice in currentConnected) {
            if (!mLastBluetoothDevices!!.contains(bluetoothDevice)) {
                connected.add(bluetoothDevice)
            }
        }
        for (bluetoothDevice in mLastBluetoothDevices!!) {
            if (!currentConnected.contains(bluetoothDevice)) {
                disconnected.add(bluetoothDevice)
            }
        }
        mLastBluetoothDevices = currentConnected
        for (bluetoothDevice in disconnected) {
            disconnectBluetoothDevice(bluetoothDevice)
        }
        for (bluetoothDevice in connected) {
            connectBluetoothDevice(bluetoothDevice)
        }
        val finalThis = this
        mHandler!!.postDelayed({ finalThis.chromebookConnectionHandler() }, 10000)
    }

    fun connectBluetoothDevice(bluetoothDevice: BluetoothDevice?): Boolean {
        Log.v(TAG, "connectBluetoothDevice device=$bluetoothDevice")
        synchronized(this) {
            if (mBluetoothDevices.containsKey(bluetoothDevice)) {
                Log.v(
                    TAG,
                    "Steam controller with address $bluetoothDevice already exists, attempting reconnect"
                )
                val device = mBluetoothDevices[bluetoothDevice]
                device!!.reconnect()
                return false
            }
            if(bluetoothDevice!=null) {
                val device = HIDDeviceBLESteamController(this, bluetoothDevice)
                val id = device.id
                mBluetoothDevices[bluetoothDevice] = device
                mDevicesById.put(id, device)
            }
            else return false
        }
        return true
    }

    fun disconnectBluetoothDevice(bluetoothDevice: BluetoothDevice?) {
        synchronized(this) {
            val device = mBluetoothDevices[bluetoothDevice] ?: return
            val id = device.id
            mBluetoothDevices.remove(bluetoothDevice)
            mDevicesById.remove(id)
            device.shutdown()
            HIDDeviceDisconnected(id)
        }
    }

    fun isSteamController(bluetoothDevice: BluetoothDevice?): Boolean {
        // Sanity check.  If you pass in a null device, by definition it is never a Steam Controller.
        if (bluetoothDevice == null) {
            return false
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        // If the device has no local name, we really don't want to try an equality check against it.
        if(bluetoothDevice.name == "SteamController" && bluetoothDevice.type and BluetoothDevice.DEVICE_TYPE_LE != 0)
            return true
        else return false
    }

    private fun close() {
        shutdownUSB()
        shutdownBluetooth()
        synchronized(this) {
            for (device in mDevicesById.values) {
                device.shutdown()
            }
            mDevicesById.clear()
            mBluetoothDevices.clear()
            HIDDeviceReleaseCallback()
        }
    }

    fun setFrozen(frozen: Boolean) {
        synchronized(this) {
            for (device in mDevicesById.values) {
                device.setFrozen(frozen)
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    private fun getDevice(id: Int): HIDDevice? {
        synchronized(this) {
            val result = mDevicesById[id]
            if (result == null) {
                Log.v(TAG, "No device for id: $id")
                Log.v(TAG, "Available devices: " + mDevicesById.keys)
            }
            return result
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////// JNI interface functions
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    fun initialize(usb: Boolean, bluetooth: Boolean): Boolean {
        Log.v(TAG, "initialize($usb, $bluetooth)")
        if (usb) {
            initializeUSB()
        }
        if (bluetooth) {
            initializeBluetooth()
        }
        return true
    }

    fun openDevice(deviceID: Int): Boolean {
        Log.v(TAG, "openDevice deviceID=$deviceID")
        val device = getDevice(deviceID)
        if (device == null) {
            HIDDeviceDisconnected(deviceID)
            return false
        }

        // Look to see if this is a USB device and we have permission to access it
        val usbDevice = device.device
        if (usbDevice != null && !uSBManager!!.hasPermission(usbDevice)) {
            HIDDeviceOpenPending(deviceID)
            try {
                val FLAG_MUTABLE =
                    0x02000000 // PendingIntent.FLAG_MUTABLE, but don't require SDK 31
                val flags: Int
                flags = if (Build.VERSION.SDK_INT >= 31) {
                    FLAG_MUTABLE
                } else {
                    0
                }
                uSBManager!!.requestPermission(
                    usbDevice, PendingIntent.getBroadcast(
                        context, 0, Intent(
                            ACTION_USB_PERMISSION
                        ), flags
                    )
                )
            } catch (e: Exception) {
                Log.v(TAG, "Couldn't request permission for USB device $usbDevice")
                HIDDeviceOpenResult(deviceID, false)
            }
            return false
        }
        try {
            return device.open()
        } catch (e: Exception) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e))
        }
        return false
    }

    fun sendOutputReport(deviceID: Int, report: ByteArray?): Int {
        try {
            //Log.v(TAG, "sendOutputReport deviceID=" + deviceID + " length=" + report.length);
            val device: HIDDevice?
            device = getDevice(deviceID)
            if (device == null) {
                HIDDeviceDisconnected(deviceID)
                return -1
            }
            return device.sendOutputReport(report)
        } catch (e: Exception) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e))
        }
        return -1
    }

    fun sendFeatureReport(deviceID: Int, report: ByteArray?): Int {
        try {
            //Log.v(TAG, "sendFeatureReport deviceID=" + deviceID + " length=" + report.length);
            val device: HIDDevice?
            device = getDevice(deviceID)
            if (device == null) {
                HIDDeviceDisconnected(deviceID)
                return -1
            }
            return device.sendFeatureReport(report)
        } catch (e: Exception) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e))
        }
        return -1
    }

    fun getFeatureReport(deviceID: Int, report: ByteArray?): Boolean {
        try {
            //Log.v(TAG, "getFeatureReport deviceID=" + deviceID);
            val device: HIDDevice?
            device = getDevice(deviceID)
            if (device == null) {
                HIDDeviceDisconnected(deviceID)
                return false
            }
            return device.getFeatureReport(report)
        } catch (e: Exception) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e))
        }
        return false
    }

    fun closeDevice(deviceID: Int) {
        try {
            Log.v(TAG, "closeDevice deviceID=$deviceID")
            val device: HIDDevice?
            device = getDevice(deviceID)
            if (device == null) {
                HIDDeviceDisconnected(deviceID)
                return
            }
            device.close()
        } catch (e: Exception) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e))
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////// Native methods
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    private external fun HIDDeviceRegisterCallback()
    private external fun HIDDeviceReleaseCallback()
    external fun HIDDeviceConnected(
        deviceID: Int,
        identifier: String?,
        vendorId: Int,
        productId: Int,
        serial_number: String?,
        release_number: Int,
        manufacturer_string: String?,
        product_string: String?,
        interface_number: Int,
        interface_class: Int,
        interface_subclass: Int,
        interface_protocol: Int
    )

    external fun HIDDeviceOpenPending(deviceID: Int)
    external fun HIDDeviceOpenResult(deviceID: Int, opened: Boolean)
    external fun HIDDeviceDisconnected(deviceID: Int)
    external fun HIDDeviceInputReport(deviceID: Int, report: ByteArray?)
    external fun HIDDeviceFeatureReport(deviceID: Int, report: ByteArray?)

    companion object {
        private const val TAG = "hidapi"
        private const val ACTION_USB_PERMISSION = "org.libsdl.app.USB_PERMISSION"
        private var sManager: HIDDeviceManager? = null
        private var sManagerRefCount = 0
        @JvmStatic
        fun acquire(context: Context): HIDDeviceManager? {
            if (sManagerRefCount == 0) {
                sManager = HIDDeviceManager(context)
            }
            ++sManagerRefCount
            return sManager
        }

        @JvmStatic
        fun release(manager: HIDDeviceManager) {
            if (manager === sManager) {
                --sManagerRefCount
                if (sManagerRefCount == 0) {
                    sManager!!.close()
                    sManager = null
                }
            }
        }
    }
}
