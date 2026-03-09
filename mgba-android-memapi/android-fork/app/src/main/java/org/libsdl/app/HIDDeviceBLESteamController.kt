package org.libsdl.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Arrays
import java.util.LinkedList
import java.util.UUID

//import com.android.internal.util.HexDump;
internal class HIDDeviceBLESteamController(
    private var mManager: HIDDeviceManager?,
    private val mDevice: BluetoothDevice
) : BluetoothGattCallback(), HIDDevice {
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////// Public API
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    override val id: Int
    var gatt: BluetoothGatt?
        private set
    private var isRegistered = false
    private var mIsConnected = false
    private var mIsChromebook = false
    private var mIsReconnecting = false
    private var mFrozen = false
    private val mOperations: LinkedList<GattOperation>
    var mCurrentOperation: GattOperation? = null
    private val mHandler: Handler

    internal class GattOperation {
        enum class Operation {
            CHR_READ,
            CHR_WRITE,
            ENABLE_NOTIFICATION
        }

        var mOp: Operation
        var mUuid: UUID
        var mValue: ByteArray? = null
        var mGatt: BluetoothGatt?
        var mResult = true

        private constructor(gatt: BluetoothGatt?, operation: Operation, uuid: UUID) {
            mGatt = gatt
            mOp = operation
            mUuid = uuid
        }

        private constructor(
            gatt: BluetoothGatt?,
            operation: Operation,
            uuid: UUID,
            value: ByteArray?
        ) {
            mGatt = gatt
            mOp = operation
            mUuid = uuid
            mValue = value
        }

        @SuppressLint("MissingPermission")
        fun run() {
            // This is executed in main thread
            val chr: BluetoothGattCharacteristic?
            when (mOp) {
                Operation.CHR_READ -> {
                    chr = getCharacteristic(mUuid)
                    //Log.v(TAG, "Reading characteristic " + chr.getUuid());
                    if (!mGatt!!.readCharacteristic(chr)) {
                        Log.e(TAG, "Unable to read characteristic $mUuid")
                        mResult = false
                    }
                    mResult = true
                }

                Operation.CHR_WRITE -> {
                    chr = getCharacteristic(mUuid)
                    //Log.v(TAG, "Writing characteristic " + chr.getUuid() + " value=" + HexDump.toHexString(value));
                    chr!!.setValue(mValue)
                    if (!mGatt!!.writeCharacteristic(chr)) {
                        Log.e(TAG, "Unable to write characteristic $mUuid")
                        mResult = false
                    }
                    mResult = true
                }

                Operation.ENABLE_NOTIFICATION -> {
                    chr = getCharacteristic(mUuid)
                    //Log.v(TAG, "Writing descriptor of " + chr.getUuid());
                    if (chr != null) {
                        val cccd =
                            chr.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (cccd != null) {
                            val properties = chr.properties
                            val value: ByteArray
                            if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            } else if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
                                value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                            } else {
                                Log.e(TAG, "Unable to start notifications on input characteristic")
                                mResult = false
                                return
                            }
                            mGatt!!.setCharacteristicNotification(chr, true)
                            cccd.setValue(value)
                            if (!mGatt!!.writeDescriptor(cccd)) {
                                Log.e(TAG, "Unable to write descriptor $mUuid")
                                mResult = false
                                return
                            }
                            mResult = true
                        }
                    }
                }
            }
        }

        fun finish(): Boolean {
            return mResult
        }

        private fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
            val valveService = mGatt!!.getService(steamControllerService)
                ?: return null
            return valveService.getCharacteristic(uuid)
        }

        companion object {
            fun readCharacteristic(gatt: BluetoothGatt?, uuid: UUID): GattOperation {
                return GattOperation(gatt, Operation.CHR_READ, uuid)
            }

            fun writeCharacteristic(
                gatt: BluetoothGatt?,
                uuid: UUID,
                value: ByteArray?
            ): GattOperation {
                return GattOperation(gatt, Operation.CHR_WRITE, uuid, value)
            }

            fun enableNotification(gatt: BluetoothGatt?, uuid: UUID): GattOperation {
                return GattOperation(gatt, Operation.ENABLE_NOTIFICATION, uuid)
            }
        }
    }

    init {
        id = mManager!!.getDeviceIDForIdentifier(identifier)
        isRegistered = false
        mIsChromebook =
            mManager!!.context.packageManager.hasSystemFeature("org.chromium.arc.device_management")
        mOperations = LinkedList()
        mHandler = Handler(Looper.getMainLooper())
        gatt = connectGatt()
        // final HIDDeviceBLESteamController finalThis = this;
        // mHandler.postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        //         finalThis.checkConnectionForChromebookIssue();
        //     }
        // }, CHROMEBOOK_CONNECTION_CHECK_INTERVAL);
    }

    val identifier: String
        get() = String.format("SteamController.%s", mDevice.address)

    // Because on Chromebooks we show up as a dual-mode device, it will attempt to connect TRANSPORT_AUTO, which will use TRANSPORT_BREDR instead
    // of TRANSPORT_LE.  Let's force ourselves to connect low energy.
    @SuppressLint("MissingPermission")
    private fun connectGatt(managed: Boolean = false): BluetoothGatt {
        return if (Build.VERSION.SDK_INT >= 23) {
            try {
                mDevice.connectGatt(mManager!!.context, managed, this, TRANSPORT_LE)
            } catch (e: Exception) {
                mDevice.connectGatt(mManager!!.context, managed, this)
            }
        } else {
            mDevice.connectGatt(mManager!!.context, managed, this)
        }
    }

    protected val connectionState: Int
        @SuppressLint("MissingPermission") protected get() {
            val context = mManager!!.context
                ?: // We are lacking any context to get our Bluetooth information.  We'll just assume disconnected.
                return BluetoothProfile.STATE_DISCONNECTED
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                ?: // This device doesn't support Bluetooth.  We should never be here, because how did
// we instantiate a device to start with?
                return BluetoothProfile.STATE_DISCONNECTED
            return btManager.getConnectionState(mDevice, BluetoothProfile.GATT)
        }

    @SuppressLint("MissingPermission")
    fun reconnect() {
        if (connectionState != BluetoothProfile.STATE_CONNECTED) {
            gatt!!.disconnect()
            gatt = connectGatt()
        }
    }

    @SuppressLint("MissingPermission")
    protected fun checkConnectionForChromebookIssue() {
        if (!mIsChromebook) {
            // We only do this on Chromebooks, because otherwise it's really annoying to just attempt
            // over and over.
            return
        }
        val connectionState = connectionState
        when (connectionState) {
            BluetoothProfile.STATE_CONNECTED -> if (!mIsConnected) {
                // We are in the Bad Chromebook Place.  We can force a disconnect
                // to try to recover.
                Log.v(
                    TAG,
                    "Chromebook: We are in a very bad state; the controller shows as connected in the underlying Bluetooth layer, but we never received a callback.  Forcing a reconnect."
                )
                mIsReconnecting = true
                gatt!!.disconnect()
                gatt = connectGatt(false)
            } else if (!isRegistered) {
                if (gatt!!.services.size > 0) {
                    Log.v(
                        TAG,
                        "Chromebook: We are connected to a controller, but never got our registration.  Trying to recover."
                    )
                    probeService(this)
                } else {
                    Log.v(
                        TAG,
                        "Chromebook: We are connected to a controller, but never discovered services.  Trying to recover."
                    )
                    mIsReconnecting = true
                    gatt!!.disconnect()
                    gatt = connectGatt(false)
                }
            } else {
                Log.v(TAG, "Chromebook: We are connected, and registered.  Everything's good!")
                return
            }

            BluetoothProfile.STATE_DISCONNECTED -> {
                Log.v(
                    TAG,
                    "Chromebook: We have either been disconnected, or the Chromebook BtGatt.ContextMap bug has bitten us.  Attempting a disconnect/reconnect, but we may not be able to recover."
                )
                mIsReconnecting = true
                gatt!!.disconnect()
                gatt = connectGatt(false)
            }

            BluetoothProfile.STATE_CONNECTING -> Log.v(
                TAG,
                "Chromebook: We're still trying to connect.  Waiting a bit longer."
            )
        }
        val finalThis = this
        mHandler.postDelayed(
            { finalThis.checkConnectionForChromebookIssue() },
            CHROMEBOOK_CONNECTION_CHECK_INTERVAL.toLong()
        )
    }

    private fun setRegistered() {
        isRegistered = true
    }

    @SuppressLint("MissingPermission")
    private fun probeService(controller: HIDDeviceBLESteamController): Boolean {
        if (isRegistered) {
            return true
        }
        if (!mIsConnected) {
            return false
        }
        Log.v(TAG, "probeService controller=$controller")
        for (service in gatt!!.services) {
            if (service.uuid == steamControllerService) {
                Log.v(TAG, "Found Valve steam controller service " + service.uuid)
                for (chr in service.characteristics) {
                    if (chr.uuid == inputCharacteristic) {
                        Log.v(TAG, "Found input characteristic")
                        // Start notifications
                        val cccd =
                            chr.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (cccd != null) {
                            enableNotification(chr.uuid)
                        }
                    }
                }
                return true
            }
        }
        if (gatt!!.services.size == 0 && mIsChromebook && !mIsReconnecting) {
            Log.e(
                TAG,
                "Chromebook: Discovered services were empty; this almost certainly means the BtGatt.ContextMap bug has bitten us."
            )
            mIsConnected = false
            mIsReconnecting = true
            gatt!!.disconnect()
            gatt = connectGatt(false)
        }
        return false
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    private fun finishCurrentGattOperation() {
        var op: GattOperation? = null
        synchronized(mOperations) {
            if (mCurrentOperation != null) {
                op = mCurrentOperation
                mCurrentOperation = null
            }
        }
        if (op != null) {
            val result = op!!.finish() // TODO: Maybe in main thread as well?

            // Our operation failed, let's add it back to the beginning of our queue.
            if (!result) {
                mOperations.addFirst(op)
            }
        }
        executeNextGattOperation()
    }

    private fun executeNextGattOperation() {
        synchronized(mOperations) {
            if (mCurrentOperation != null) return
            if (mOperations.isEmpty()) return
            mCurrentOperation = mOperations.removeFirst()
        }

        // Run in main thread
        mHandler.post(Runnable {
            synchronized(mOperations) {
                if (mCurrentOperation == null) {
                    Log.e(TAG, "Current operation null in executor?")
                    return@Runnable
                }
                mCurrentOperation!!.run()
            }
        })
    }

    private fun queueGattOperation(op: GattOperation) {
        synchronized(mOperations) { mOperations.add(op) }
        executeNextGattOperation()
    }

    private fun enableNotification(chrUuid: UUID) {
        val op = GattOperation.enableNotification(gatt, chrUuid)
        queueGattOperation(op)
    }

    fun writeCharacteristic(uuid: UUID, value: ByteArray?) {
        val op = GattOperation.writeCharacteristic(gatt, uuid, value)
        queueGattOperation(op)
    }

    fun readCharacteristic(uuid: UUID) {
        val op = GattOperation.readCharacteristic(gatt, uuid)
        queueGattOperation(op)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////  BluetoothGattCallback overridden methods
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
        //Log.v(TAG, "onConnectionStateChange status=" + status + " newState=" + newState);
        mIsReconnecting = false
        if (newState == 2) {
            mIsConnected = true
            // Run directly, without GattOperation
            if (!isRegistered) {
                mHandler.post { gatt!!.discoverServices() }
            }
        } else if (newState == 0) {
            mIsConnected = false
        }

        // Disconnection is handled in SteamLink using the ACTION_ACL_DISCONNECTED Intent.
    }

    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        //Log.v(TAG, "onServicesDiscovered status=" + status);
        if (status == 0) {
            if (gatt.services.size == 0) {
                Log.v(
                    TAG,
                    "onServicesDiscovered returned zero services; something has gone horribly wrong down in Android's Bluetooth stack."
                )
                mIsReconnecting = true
                mIsConnected = false
                gatt.disconnect()
                this.gatt = connectGatt(false)
            } else {
                probeService(this)
            }
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        //Log.v(TAG, "onCharacteristicRead status=" + status + " uuid=" + characteristic.getUuid());
        if (characteristic.uuid == reportCharacteristic && !mFrozen) {
            mManager!!.HIDDeviceFeatureReport(id, characteristic.value)
        }
        finishCurrentGattOperation()
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        //Log.v(TAG, "onCharacteristicWrite status=" + status + " uuid=" + characteristic.getUuid());
        if (characteristic.uuid == reportCharacteristic) {
            // Only register controller with the native side once it has been fully configured
            if (!isRegistered) {
                Log.v(TAG, "Registering Steam Controller with ID: $id")
                mManager!!.HIDDeviceConnected(
                    id,
                    identifier,
                    vendorId,
                    productId,
                    serialNumber,
                    version,
                    manufacturerName,
                    productName,
                    0,
                    0,
                    0,
                    0
                )
                setRegistered()
            }
        }
        finishCurrentGattOperation()
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        // Enable this for verbose logging of controller input reports
        //Log.v(TAG, "onCharacteristicChanged uuid=" + characteristic.getUuid() + " data=" + HexDump.dumpHexString(characteristic.getValue()));
        if (characteristic.uuid == inputCharacteristic && !mFrozen) {
            mManager!!.HIDDeviceInputReport(id, characteristic.value)
        }
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        //Log.v(TAG, "onDescriptorRead status=" + status);
    }

    @SuppressLint("MissingPermission")
    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        val chr = descriptor.characteristic
        //Log.v(TAG, "onDescriptorWrite status=" + status + " uuid=" + chr.getUuid() + " descriptor=" + descriptor.getUuid());
        if (chr.uuid == inputCharacteristic) {
            val hasWrittenInputDescriptor = true
            val reportChr = chr.service.getCharacteristic(reportCharacteristic)
            if (reportChr != null) {
                Log.v(TAG, "Writing report characteristic to enter valve mode")
                reportChr.setValue(enterValveMode)
                gatt.writeCharacteristic(reportChr)
            }
        }
        finishCurrentGattOperation()
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
        //Log.v(TAG, "onReliableWriteCompleted status=" + status);
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        //Log.v(TAG, "onReadRemoteRssi status=" + status);
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        //Log.v(TAG, "onMtuChanged status=" + status);
    }

    override val vendorId: Int
        get() =// Valve Corporation
            0x28DE
    override val productId: Int
        get() =// We don't have an easy way to query from the Bluetooth device, but we know what it is
            0x1106
    override val serialNumber: String
        get() =// This will be read later via feature report by Steam
            "12345"
    override val version: Int
        get() = 0
    override val manufacturerName: String
        get() = "Valve Corporation"
    override val productName: String
        get() = "Steam Controller"
    override val device: UsbDevice?
        get() = null

    override fun open(): Boolean {
        return true
    }

    override fun sendFeatureReport(report: ByteArray?): Int {
        if (!isRegistered) {
            Log.e(TAG, "Attempted sendFeatureReport before Steam Controller is registered!")
            if (mIsConnected) {
                probeService(this)
            }
            return -1
        }

        // We need to skip the first byte, as that doesn't go over the air
        val actual_report = Arrays.copyOfRange(report, 1, report!!.size - 1)
        //Log.v(TAG, "sendFeatureReport " + HexDump.dumpHexString(actual_report));
        writeCharacteristic(reportCharacteristic, actual_report)
        return report.size
    }

    override fun sendOutputReport(report: ByteArray?): Int {
        if (!isRegistered) {
            Log.e(TAG, "Attempted sendOutputReport before Steam Controller is registered!")
            if (mIsConnected) {
                probeService(this)
            }
            return -1
        }

        //Log.v(TAG, "sendFeatureReport " + HexDump.dumpHexString(report));
        writeCharacteristic(reportCharacteristic, report)
        return report!!.size
    }

    override fun getFeatureReport(report: ByteArray?): Boolean {
        if (!isRegistered) {
            Log.e(TAG, "Attempted getFeatureReport before Steam Controller is registered!")
            if (mIsConnected) {
                probeService(this)
            }
            return false
        }

        //Log.v(TAG, "getFeatureReport");
        readCharacteristic(reportCharacteristic)
        return true
    }

    override fun close() {}
    override fun setFrozen(frozen: Boolean) {
        mFrozen = frozen
    }

    @SuppressLint("MissingPermission")
    override fun shutdown() {
        close()
        val g = gatt
        if (g != null) {
            g.disconnect()
            g.close()
            gatt = null
        }
        mManager = null
        isRegistered = false
        mIsConnected = false
        mOperations.clear()
    }

    companion object {
        private const val TAG = "hidapi"
        private const val TRANSPORT_AUTO = 0
        private const val TRANSPORT_BREDR = 1
        private const val TRANSPORT_LE = 2
        private const val CHROMEBOOK_CONNECTION_CHECK_INTERVAL = 10000
        val steamControllerService = UUID.fromString("100F6C32-1735-4313-B402-38567131E5F3")
        val inputCharacteristic = UUID.fromString("100F6C33-1735-4313-B402-38567131E5F3")
        val reportCharacteristic = UUID.fromString("100F6C34-1735-4313-B402-38567131E5F3")
        private val enterValveMode =
            byteArrayOf(0xC0.toByte(), 0x87.toByte(), 0x03, 0x08, 0x07, 0x00)
    }
}
