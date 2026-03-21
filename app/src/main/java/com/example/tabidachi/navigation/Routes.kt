package com.example.tabidachi.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SetupRoute

@Serializable
data object SetupPinRoute

@Serializable
data object LockRoute

@Serializable
data object DashboardRoute

@Serializable
data class TripDetailRoute(val tripId: String)

@Serializable
data object SettingsRoute
