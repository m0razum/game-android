package com.carscanner.app.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carscanner.app.obd.ObdCommand
import com.carscanner.app.obd.ObdPids
import com.carscanner.app.obd.ObdService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PidValue(
    val command: ObdCommand,
    val value: Float? = null,
    val raw: String? = null
)

data class CarState(
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val statusMessage: String = "Disconnected",
    val availableDevices: List<BluetoothDevice> = emptyList(),
    val selectedDevice: BluetoothDevice? = null,
    val pidValues: List<PidValue> = emptyList(),
    val dtcCodes: List<String> = emptyList(),
    val connectionError: String? = null
)

class CarScannerViewModel : ViewModel() {

    private val obdService = ObdService()
    private var pollingJob: Job? = null

    private val _state = MutableStateFlow(CarState())
    val state: StateFlow<CarState> = _state.asStateFlow()

    private val pids = ObdPids.supportedPids

    fun scanDevices() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true, statusMessage = "Scanning...")
            val obdDevices = obdService.getPairedDevices()
            val allDevices = obdService.getAllPairedDevices()
            _state.value = _state.value.copy(
                isScanning = false,
                availableDevices = if (obdDevices.isNotEmpty()) obdDevices else allDevices,
                statusMessage = if (obdDevices.isEmpty() && allDevices.isEmpty()) "No paired devices found"
                              else "${_state.value.availableDevices.size} devices found"
            )
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                statusMessage = "Connecting to ${device.name}...",
                selectedDevice = device,
                connectionError = null
            )
            val result = obdService.connect(device)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isConnected = true,
                        statusMessage = "Connected to ${device.name}",
                        pidValues = pids.map { PidValue(it) }
                    )
                    startPolling()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isConnected = false,
                        statusMessage = "Connection failed",
                        connectionError = error.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                pollPids()
                delay(500)
            }
        }
    }

    private suspend fun pollPids() {
        val currentValues = _state.value.pidValues.toMutableList()
        for (i in currentValues.indices) {
            val pid = currentValues[i]
            val raw = obdService.readPid(pid.command.command)
            if (raw != null) {
                val value = pid.command.formula(raw)
                currentValues[i] = pid.copy(value = value, raw = raw)
            }
        }
        _state.value = _state.value.copy(pidValues = currentValues)
    }

    fun readDtc() {
        viewModelScope.launch {
            _state.value = _state.value.copy(statusMessage = "Reading DTC...")
            val codes = obdService.readDtc()
            _state.value = _state.value.copy(
                dtcCodes = codes,
                statusMessage = if (codes.isEmpty()) "No trouble codes"
                              else "Found ${codes.size} code(s)"
            )
        }
    }

    fun clearDtc() {
        viewModelScope.launch {
            _state.value = _state.value.copy(statusMessage = "Clearing DTC...")
            val success = obdService.clearDtc()
            if (success) {
                _state.value = _state.value.copy(
                    dtcCodes = emptyList(),
                    statusMessage = "DTC cleared successfully"
                )
            } else {
                _state.value = _state.value.copy(
                    statusMessage = "Failed to clear DTC"
                )
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        obdService.disconnect()
        _state.value = _state.value.copy(
            isConnected = false,
            statusMessage = "Disconnected",
            pidValues = emptyList(),
            dtcCodes = emptyList()
        )
    }

    override fun onCleared() {
        pollingJob?.cancel()
        obdService.disconnect()
        super.onCleared()
    }
}
