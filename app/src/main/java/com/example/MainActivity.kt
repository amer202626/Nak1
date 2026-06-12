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
        
        setContent {
            // Live observe master setting primary and secondary hex colors
            val settings by viewModel.appSettings.collectAsState()
            
            val primaryHex = settings?.primaryColor ?: "#ECEFF1"
            val secondaryHex = settings?.secondaryColor ?: "#37474F"

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
