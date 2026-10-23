package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.main.IoTViewModel
import com.example.ui.main.MainScreen
import com.example.ui.theme.IoTSentinelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val iotViewModel: IoTViewModel = viewModel()
            val iotState by iotViewModel.uiState.collectAsState()
            val authState by authViewModel.uiState.collectAsState()

            // Handle back navigation between tabs only when authenticated
            BackHandler(enabled = authState.isAuthenticated && iotState.currentTab != 0) {
                iotViewModel.selectTab(0)
            }

            IoTSentinelTheme(
                darkTheme = iotState.isDarkMode,
                accentColor = iotState.accentColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MainScreen(
                        iotViewModel = iotViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
