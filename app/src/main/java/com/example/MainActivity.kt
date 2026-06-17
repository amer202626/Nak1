package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screen.MainDashboard
import com.example.ui.theme.WamServicesTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {

    // Initialize unified logical controller
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Fast Synchronous Preference read to avoid slow layout render flash
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val cachedPrimary = sharedPrefs.getString("primary_color", "#ECEFF1") ?: "#ECEFF1"
        val cachedSecondary = sharedPrefs.getString("secondary_color", "#37474F") ?: "#37474F"
        
        setContent {
            // Live observe master setting primary and secondary hex colors
            val settings by viewModel.appSettings.collectAsState()
            
            val primaryHex = settings?.primaryColor ?: cachedPrimary
            val secondaryHex = settings?.secondaryColor ?: cachedSecondary

            WamServicesTheme(
                primaryHex = primaryHex,
                secondaryHex = secondaryHex
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainDashboard(viewModel = viewModel)
                }
            }
        }
    }
}
