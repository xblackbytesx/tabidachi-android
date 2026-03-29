package com.example.tabidachi.navigation

import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.ui.dashboard.DashboardScreen
import com.example.tabidachi.ui.lock.LockScreen
import com.example.tabidachi.ui.settings.SettingsScreen
import com.example.tabidachi.ui.setup.SetupPinScreen
import com.example.tabidachi.ui.setup.SetupScreen
import com.example.tabidachi.ui.trip.SharedTripScreen
import com.example.tabidachi.ui.trip.TripDetailScreen

@Composable
fun TabidachiNavHost(app: TabidachiApp, activity: FragmentActivity) {
    val navController = rememberNavController()

    val startDestination: Any = when {
        !app.secureStorage.isConfigured() -> SetupRoute
        app.secureStorage.isPinConfigured() && !app.isAuthenticated -> LockRoute
        app.prefsManager.isDefaultTripEnabled && app.prefsManager.defaultTripId != null ->
            TripDetailRoute(app.prefsManager.defaultTripId!!)
        else -> DashboardRoute
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<SetupRoute> {
            SetupScreen(
                app = app,
                onSetupComplete = {
                    navController.navigate(SetupPinRoute) {
                        popUpTo<SetupRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<SetupPinRoute> {
            SetupPinScreen(
                app = app,
                activity = activity,
                onComplete = {
                    app.prefsManager.setupCompleted = true
                    navController.navigate(DashboardRoute) {
                        popUpTo<SetupPinRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<LockRoute> {
            LockScreen(
                app = app,
                activity = activity,
                onUnlocked = {
                    app.isAuthenticated = true
                    val destination: Any =
                        if (app.prefsManager.isDefaultTripEnabled && app.prefsManager.defaultTripId != null) {
                            TripDetailRoute(app.prefsManager.defaultTripId!!)
                        } else {
                            DashboardRoute
                        }
                    navController.navigate(destination) {
                        popUpTo<LockRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<DashboardRoute> {
            DashboardScreen(
                app = app,
                onTripClick = { tripId ->
                    navController.navigate(TripDetailRoute(tripId))
                },
                onSettingsClick = {
                    navController.navigate(SettingsRoute)
                },
                onOpenSharedTrip = { serverUrl, shareToken ->
                    navController.navigate(SharedTripRoute(serverUrl, shareToken))
                },
            )
        }

        composable<SharedTripRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SharedTripRoute>()
            SharedTripScreen(
                app = app,
                serverUrl = route.serverUrl,
                shareToken = route.shareToken,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<TripDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TripDetailRoute>()
            TripDetailScreen(
                app = app,
                tripId = route.tripId,
                onNavigateToDashboard = {
                    navController.navigate(DashboardRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                app = app,
                activity = activity,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(SetupRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
