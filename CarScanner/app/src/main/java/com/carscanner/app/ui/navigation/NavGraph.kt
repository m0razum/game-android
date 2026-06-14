package com.carscanner.app.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Dashboard : Screen("dashboard")
    data object Dtc : Screen("dtc")
}
