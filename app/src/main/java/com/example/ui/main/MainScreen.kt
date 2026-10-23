package com.example.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.ConnectionState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.connection.ConnectionScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.simulation.SimulationScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    iotViewModel: IoTViewModel,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.uiState.collectAsState()
    val iotState by iotViewModel.uiState.collectAsState()

    // Protected Route Gate: Check session
    if (authState.isCheckingSession) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (!authState.isAuthenticated) {
        AuthScreen(authViewModel = authViewModel)
        return
    }

    // Authenticated Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IoT Sentinel",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Connection Status indicator
                                val statusColor = when (iotState.connectionState) {
                                    ConnectionState.CONNECTED -> EmeraldAccent
                                    ConnectionState.LISTENING, ConnectionState.CONNECTING -> AmberWarning
                                    ConnectionState.CONNECTION_LOST, ConnectionState.ERROR -> RedAlert
                                    ConnectionState.DISCONNECTED -> Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                            }
                            Text(
                                text = "Usuario: ${authState.currentUser?.username ?: "Invitado"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("main_top_app_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                val navItems = listOf(
                    Triple(0, "Inicio", Icons.Default.Dashboard),
                    Triple(1, "Sensores", Icons.Default.Sensors),
                    Triple(2, "Conexión", Icons.Default.Sync),
                    Triple(3, "Historial", Icons.Default.History),
                    Triple(4, "Ajustes", Icons.Default.Settings)
                )

                navItems.forEach { (index, label, icon) ->
                    val selected = iotState.currentTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { iotViewModel.selectTab(index) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text(text = label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = iotState.currentTab,
            label = "tab_transition",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                0 -> DashboardScreen(viewModel = iotViewModel)
                1 -> SimulationScreen(viewModel = iotViewModel)
                2 -> ConnectionScreen(viewModel = iotViewModel)
                3 -> HistoryScreen(viewModel = iotViewModel)
                4 -> SettingsScreen(viewModel = iotViewModel, authViewModel = authViewModel)
                else -> DashboardScreen(viewModel = iotViewModel)
            }
        }
    }
}
