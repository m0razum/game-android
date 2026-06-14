package com.carscanner.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carscanner.app.ui.navigation.Screen
import com.carscanner.app.ui.screens.DashboardScreen
import com.carscanner.app.ui.screens.DtcScreen
import com.carscanner.app.ui.screens.MainScreen
import com.carscanner.app.ui.theme.CarScannerTheme
import com.carscanner.app.viewmodel.CarScannerViewModel

class MainActivity : ComponentActivity() {

    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Bluetooth enabled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableBluetooth()

        setContent {
            CarScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: CarScannerViewModel = viewModel()
                    val state by viewModel.state.collectAsState()
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = Screen.Main.route) {
                        composable(Screen.Main.route) {
                            MainScreen(
                                state = state,
                                onScan = { viewModel.scanDevices() },
                                onConnect = { address ->
                                    val device = getDeviceByAddress(address)
                                    if (device != null) viewModel.connectToDevice(device)
                                },
                                onDashboard = { navController.navigate(Screen.Dashboard.route) },
                                onDtc = { navController.navigate(Screen.Dtc.route) },
                                onDisconnect = { viewModel.disconnect() }
                            )
                        }

                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                pidValues = state.pidValues,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Dtc.route) {
                            DtcScreen(
                                dtcCodes = state.dtcCodes,
                                onBack = { navController.popBackStack() },
                                onRead = { viewModel.readDtc() },
                                onClear = { viewModel.clearDtc() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun enableBluetooth() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && !adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }

    private fun getDeviceByAddress(address: String): BluetoothDevice? {
        return try {
            BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)
        } catch (e: Exception) {
            null
        }
    }
}
