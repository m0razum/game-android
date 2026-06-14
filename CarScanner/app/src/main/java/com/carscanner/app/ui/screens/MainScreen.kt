package com.carscanner.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carscanner.app.viewmodel.CarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: CarState,
    onScan: () -> Unit,
    onConnect: (String) -> Unit,
    onDashboard: () -> Unit,
    onDtc: () -> Unit,
    onDisconnect: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Car Scanner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusCard(state = state)

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isConnected) {
                ConnectedActions(onDashboard, onDtc, onDisconnect)
            } else {
                ConnectionControls(state, onScan, onConnect)
            }
        }
    }
}

@Composable
private fun StatusCard(state: CarState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (state.isConnected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = if (state.isConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            if (state.selectedDevice != null) {
                Text(
                    text = state.selectedDevice.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ConnectedActions(
    onDashboard: () -> Unit,
    onDtc: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onDashboard,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.CarRepair, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dashboard", style = MaterialTheme.typography.titleMedium)
        }

        Button(
            onClick = onDtc,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Default.MiscellaneousServices, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Diagnostic Codes", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Disconnect")
        }
    }
}

@Composable
private fun ConnectionControls(
    state: CarState,
    onScan: () -> Unit,
    onConnect: (String) -> Unit
) {
    Button(
        onClick = onScan,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = !state.isScanning,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (state.isScanning) MaterialTheme.colorScheme.surface
                           else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = if (state.isScanning) Icons.Default.BluetoothSearching
                         else Icons.Default.Bluetooth,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (state.isScanning) "Scanning..." else "Scan Devices",
            style = MaterialTheme.typography.titleMedium
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (state.availableDevices.isNotEmpty()) {
        Text(
            "Available Devices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.availableDevices) { device ->
                Card(
                    onClick = { onConnect(device.address) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isConnected) Icons.Default.BluetoothConnected
                                         else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = device.name ?: "Unknown Device",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
