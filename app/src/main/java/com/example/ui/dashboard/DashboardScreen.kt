package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.data.network.DeviceRole
import com.example.domain.SystemHealthStatus
import com.example.ui.main.IoTViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedAlert

@Composable
fun DashboardScreen(
    viewModel: IoTViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tempHistory by viewModel.temperatureHistory.collectAsState()
    val humHistory by viewModel.humidityHistory.collectAsState()
    val mqHistory by viewModel.mq135History.collectAsState()

    val scrollState = rememberScrollState()

    val healthColor = when (uiState.systemHealth) {
        SystemHealthStatus.NORMAL -> EmeraldAccent
        SystemHealthStatus.PRECAUCIÓN -> AmberWarning
        SystemHealthStatus.ALERTA -> RedAlert
        SystemHealthStatus.SIN_DATOS -> Color.Gray
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System Health & Connection Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("system_health_banner"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(healthColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ESTADO GENERAL: ${uiState.systemHealth.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = healthColor
                        )
                    }

                    // Role pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (uiState.deviceRole == DeviceRole.MONITORING_STATION) "ESTACIÓN" else "NODO SENSOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection details line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val connText = when (uiState.connectionState) {
                        ConnectionState.CONNECTED -> "Conectado (${uiState.transportType.name.replace("_", " ")})"
                        ConnectionState.LISTENING -> "Escuchando conexiones..."
                        ConnectionState.CONNECTING -> "Estableciendo conexión..."
                        ConnectionState.CONNECTION_LOST -> "¡CONEXIÓN PERDIDA!"
                        ConnectionState.ERROR -> "Error en conexión"
                        ConnectionState.DISCONNECTED -> "Desconectado (Modo Autónomo)"
                    }
                    val connColor = when (uiState.connectionState) {
                        ConnectionState.CONNECTED -> EmeraldAccent
                        ConnectionState.CONNECTION_LOST, ConnectionState.ERROR -> RedAlert
                        ConnectionState.LISTENING, ConnectionState.CONNECTING -> AmberWarning
                        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Text(
                        text = connText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = connColor
                    )

                    Text(
                        text = "Recibidos: ${uiState.packetsReceivedCount} | Enviados: ${uiState.packetsSentCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.isSimulationActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ SIMULACIÓN ACTIVA: Generando lecturas locales cada 1.5s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberWarning
                    )
                }
            }
        }

        // Active Alerts Card (if any)
        if (uiState.activeAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_alerts_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alertas",
                            tint = RedAlert,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Alertas Activas (${uiState.activeAlerts.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAlert
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.activeAlerts.forEach { alert ->
                        Text(
                            text = "• ${alert.title}: ${alert.description}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // DHT11 Sensor Card
        Dht11Card(
            reading = uiState.liveReading,
            tempHistory = tempHistory,
            humHistory = humHistory
        )

        // MQ-135 Sensor Card
        Mq135Card(
            reading = uiState.liveReading,
            mqHistory = mqHistory
        )

        // Quick Navigation Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.selectTab(1) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("shortcut_simulator_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Simulador",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = { viewModel.selectTab(2) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("shortcut_connection_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Conectar",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
