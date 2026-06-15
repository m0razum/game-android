package com.carscanner.app.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdService {

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun getPairedDevices(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.bondedDevices?.filter {
            it.name.contains("OBD", ignoreCase = true) ||
            it.name.contains("ELM", ignoreCase = true) ||
            it.name.contains("OBD2", ignoreCase = true) ||
            it.name.contains("CAR", ignoreCase = true) ||
            it.name.contains("BT", ignoreCase = true) ||
            it.name.contains("HC-0", ignoreCase = true)
        } ?: emptyList()
    }

    suspend fun getAllPairedDevices(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.bondedDevices?.toList() ?: emptyList()
    }

    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val sock = device.createRfcommSocketToServiceRecord(sppUuid)
            sock.connect()
            socket = sock
            inputStream = sock.inputStream
            outputStream = sock.outputStream
            isConnected = true
            initialize()
            Result.success(Unit)
        } catch (e: IOException) {
            isConnected = false
            Result.failure(e)
        }
    }

    private suspend fun initialize() {
        sendCommand("ATZ")
        kotlinx.coroutines.delay(1500)
        sendCommand("ATE0")
        kotlinx.coroutines.delay(200)
        sendCommand("ATL0")
        kotlinx.coroutines.delay(200)
        sendCommand("ATS0")
        kotlinx.coroutines.delay(200)
        sendCommand("ATH0")
        kotlinx.coroutines.delay(200)
        sendCommand("ATSP0")
        kotlinx.coroutines.delay(200)
    }

    suspend fun sendCommand(command: String): String? = withContext(Dispatchers.IO) {
        try {
            val cmd = (command + "\r").toByteArray()
            outputStream?.write(cmd)
            outputStream?.flush()
            kotlinx.coroutines.delay(100)
            readResponse()
        } catch (e: IOException) {
            null
        }
    }

    private fun readResponse(): String? {
        val buffer = ByteArray(1024)
        val sb = StringBuilder()
        val start = System.currentTimeMillis()
        var bytesRead: Int

        while (System.currentTimeMillis() - start < 1000) {
            try {
                while (inputStream?.available()?.also { bytesRead = it } ?: 0 > 0) {
                    val n = inputStream?.read(buffer) ?: 0
                    if (n > 0) {
                        sb.append(String(buffer, 0, n).trim())
                    }
                }
                if (sb.isNotEmpty()) break
            } catch (_: IOException) {
                break
            }
        }

        return sb.toString().ifEmpty { null }
    }

    suspend fun readPid(command: String): String? {
        return sendCommand(command)
    }

    suspend fun readDtc(): List<String> = withContext(Dispatchers.IO) {
        val response = sendCommand("03") ?: return@withContext emptyList()
        parseDtc(response)
    }

    private fun parseDtc(response: String): List<String> {
        val codes = mutableListOf<String>()
        val parts = response.trim().split(" ")

        var i = 0
        while (i < parts.size - 1) {
            val first = parts[i].toIntOrNull(16)
            val second = parts[i + 1].toIntOrNull(16)

            if (first == null || second == null) { i++; continue }
            if (first == 0 && second == 0) { i += 2; continue }
            if (first == 0x7F || first == 0xFE) { i++; continue }
            if (first in 0x41..0x4A) { i += 2; continue }
            if (first == 0x55 && second == 0xAA) { break }

            val prefix = when ((first and 0xC0) shr 6) {
                0 -> "P0"
                1 -> "C0"
                2 -> "B0"
                3 -> "U0"
                else -> "?"
            }
            val code = prefix + (first and 0x3F).toString().padStart(2, '0') + second.toString().padStart(2, '0')
            codes.add(code)
            i += 2
        }
        return codes.distinct()
    }

    suspend fun clearDtc(): Boolean {
        val response = sendCommand("04")
        return response?.contains("44") == true
    }

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: IOException) {}
        inputStream = null
        outputStream = null
        socket = null
        isConnected = false
    }

    fun isConnected(): Boolean = isConnected
}
