package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screen.MainDashboard
import com.example.ui.theme.WAMServicesTheme
import com.example.ui.viewmodel.AppViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Manual self-contained Firebase initialization to bypass buggy Google Services Gradle Plugin mutation conflict
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

        // Initialize Core App ViewModel containing Firestore live syncer and database interactions
        val viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        setContent {
            WAMServicesTheme {
                MainDashboard(viewModel)
            }
        }
    }
}
