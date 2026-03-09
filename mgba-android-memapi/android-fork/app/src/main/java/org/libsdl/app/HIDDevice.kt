package org.libsdl.app

import android.hardware.usb.UsbDevice

interface HIDDevice {
    val id: Int
    val vendorId: Int
    val productId: Int
    val serialNumber: String?
    val version: Int
    val manufacturerName: String?
    val productName: String?
    val device: UsbDevice?
    fun open(): Boolean
    fun sendFeatureReport(report: ByteArray?): Int
    fun sendOutputReport(report: ByteArray?): Int
    fun getFeatureReport(report: ByteArray?): Boolean
    fun setFrozen(frozen: Boolean)
    fun close()
    fun shutdown()
}
