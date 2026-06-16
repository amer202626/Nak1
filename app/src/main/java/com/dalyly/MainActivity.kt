package com.dalyly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dalyly.screens.BookingFormScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:89823302013:android:1910d098b23f547aa3fc14")
                    .setProjectId("dalyly2026")
                    .setApiKey("AIzaSyCgFnPJso1f2mwB1jvyRbGzZReAdf4eug0")
                    .setStorageBucket("dalyly2026.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme,
                typography = MaterialTheme.typography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AppViewModel = viewModel()
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "dashboard"
                    ) {
                        composable("dashboard") {
                            MainDashboardScreen(
                                viewModel = viewModel,
                                onNavigateToBooking = { providerId ->
                                    val route = if (providerId != null) {
                                        "booking_form?providerId=$providerId"
                                    } else {
                                        "booking_form"
                                    }
                                    navController.navigate(route)
                                }
                            )
                        }

                        composable(
                            route = "booking_form?providerId={providerId}",
                            arguments = listOf(
                                navArgument("providerId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val providerId = backStackEntry.arguments?.getString("providerId")
                            BookingFormScreen(
                                viewModel = viewModel,
                                initialProviderId = providerId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
