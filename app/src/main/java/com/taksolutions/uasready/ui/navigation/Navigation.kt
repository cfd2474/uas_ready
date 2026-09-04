package com.taksolutions.uasready.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Flight Readiness")
    object Assessment : Screen("assessment", "Assessment Audit")
    object Timeline : Screen("timeline", "Timeline")
    object Aircraft : Screen("aircraft", "Fleet")
    object Pilot : Screen("pilot", "Pilot")
    object Map : Screen("map", "Aviation Map")
    object Reference : Screen("reference", "Checklists")
    object Settings : Screen("settings", "Settings")
}
