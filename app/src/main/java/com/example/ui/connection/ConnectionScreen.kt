package com.example.ui.connection

import android.app.Activity
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.network.BluetoothRfcommService
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.ConnectionState
import com.example.data.network.DeviceRole
import com.example.data.network.TransportType
import com.example.ui.main.IoTViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: IoTViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showGuideDialog by remember { mutableStateOf(false) }
    var targetIpInput by remember { mutableStateOf(uiState.targetIp) }
    var expandedBluetoothDropdown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Automatically refresh network & bonded devices when returning to the screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNetworkInfo()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "Permiso Bluetooth concedido", Toast.LENGTH_SHORT).show()
            viewModel.refreshNetworkInfo()
            if (uiState.deviceRole == DeviceRole.MONITORING_STATION) {
                viewModel.startListeningServer()
            } else {
                viewModel.connectToRemote()
            }
        } else {
            Toast.makeText(
                context,
                "Permiso Bluetooth denegado. El Modo Simulación y Wi-Fi TCP continúan disponibles.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun initiateBluetoothOrAction(action: () -> Unit) {
        if (uiState.transportType == TransportType.BLUETOOTH_RFCOMM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                btPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                return
            }
        }
        action()
    }

    if (showGuideDialog) {
        InterconnectionGuideDialog(onDismiss = { showGuideDialog = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Guide Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Interconexión Física",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bluetooth RFCOMM & TCP Wi-Fi (CE 2.1.2 / 2.1.4)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { showGuideDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("open_guide_button")
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guía 2 Teléfonos", fontSize = 12.sp)
            }
        }

        // 1. Role Selection Tabs (Station vs Sensor Node)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("role_selection_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Selecciona el Rol de este Dispositivo:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                TabRow(
                    selectedTabIndex = if (uiState.deviceRole == DeviceRole.MONITORING_STATION) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = CyanAccent,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = uiState.deviceRole == DeviceRole.MONITORING_STATION,
                        onClick = { viewModel.setDeviceRole(DeviceRole.MONITORING_STATION) },
                        text = { Text("Estación de Monitoreo", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_role_station")
                    )
                    Tab(
                        selected = uiState.deviceRole == DeviceRole.SENSOR_NODE,
                        onClick = { viewModel.setDeviceRole(DeviceRole.SENSOR_NODE) },
                        text = { Text("Nodo Sensor", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_role_node")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.deviceRole == DeviceRole.MONITORING_STATION)
                        "La Estación recibe datos por sockets inalámbricos y actualiza el dashboard en vivo."
                    else
                        "El Nodo Sensor transmite lecturas en formato JSON estructurado cada 1.5s.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Transport Protocol Selection (Wi-Fi TCP vs Bluetooth Classic RFCOMM)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transport_selection_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Medio de Comunicación Inalámbrico:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                TabRow(
                    selectedTabIndex = if (uiState.transportType == TransportType.WIFI_TCP) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = EmeraldAccent,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = uiState.transportType == TransportType.WIFI_TCP,
                        onClick = { viewModel.setTransportType(TransportType.WIFI_TCP) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wi-Fi TCP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("tab_transport_tcp")
                    )
                    Tab(
                        selected = uiState.transportType == TransportType.BLUETOOTH_RFCOMM,
                        onClick = { viewModel.setTransportType(TransportType.BLUETOOTH_RFCOMM) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bluetooth RFCOMM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("tab_transport_bluetooth")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transport Specific Parameters
                if (uiState.transportType == TransportType.WIFI_TCP) {
                    // TCP Configuration Info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "IP Local de este Teléfono: ${uiState.localIpAddress}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Text(
                                text = "Puerto TCP: ${uiState.tcpPort}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.deviceRole == DeviceRole.SENSOR_NODE) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = targetIpInput,
                            onValueChange = {
                                targetIpInput = it
                                viewModel.setTargetIp(it)
                            },
                            label = { Text("IP de la Estación de Monitoreo") },
                            placeholder = { Text("Ej: 192.168.1.45") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_ip_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                focusedLabelColor = EmeraldAccent
                            )
                        )
                    }
                } else {
                    // Bluetooth RFCOMM Configuration Info
                    Text(
                        text = "UUID SPP Estándar: ${BluetoothRfcommService.SPP_UUID}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.deviceRole == DeviceRole.SENSOR_NODE) {
                        Text(
                            text = "Dispositivos Bluetooth Emparejados:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = expandedBluetoothDropdown,
                            onExpandedChange = { expandedBluetoothDropdown = !expandedBluetoothDropdown }
                        ) {
                            @SuppressLint("MissingPermission")
                            val devName = uiState.selectedBluetoothDevice?.let {
                                try { it.name ?: it.address } catch (e: Exception) { it.address }
                            } ?: "Seleccionar dispositivo emparejado"

                            OutlinedTextField(
                                value = devName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBluetoothDropdown) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("bluetooth_device_dropdown"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = expandedBluetoothDropdown,
                                onDismissRequest = { expandedBluetoothDropdown = false }
                            ) {
                                if (uiState.pairedDevices.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hay dispositivos emparejados") },
                                        onClick = { expandedBluetoothDropdown = false }
                                    )
                                } else {
                                    uiState.pairedDevices.forEach { device ->
                                        @SuppressLint("MissingPermission")
                                        val name = try { device.name ?: device.address } catch (e: Exception) { device.address }
                                        DropdownMenuItem(
                                            text = { Text("$name (${device.address})") },
                                            onClick = {
                                                viewModel.setSelectedBluetoothDevice(device)
                                                expandedBluetoothDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Connect / Listen / Disconnect Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.connectionState == ConnectionState.DISCONNECTED ||
                        uiState.connectionState == ConnectionState.CONNECTION_LOST ||
                        uiState.connectionState == ConnectionState.ERROR
                    ) {
                        // Action based on role
                        if (uiState.deviceRole == DeviceRole.MONITORING_STATION) {
                            Button(
                                onClick = { initiateBluetoothOrAction { viewModel.startListeningServer() } },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("start_server_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Escuchar Conexiones", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { initiateBluetoothOrAction { viewModel.connectToRemote() } },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("connect_to_station_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Conectar a Estación", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.disconnect() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("disconnect_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Desconectar", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.refreshNetworkInfo() },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar red")
                    }
                }
            }
        }

        // Live Communication Terminal & Logs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_logs_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terminal de Tramas JSON en Vivo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B1120))
                        .padding(8.dp)
                ) {
                    if (uiState.logs.isEmpty()) {
                        Text(
                            text = "Esperando transmisión o recepción de paquetes...\nProtocolo: JSON delimitado por '\\n'",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        val logScroll = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(logScroll)) {
                            uiState.logs.takeLast(30).forEach { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (log.contains("Error") || log.contains("perdida")) RedAlert
                                    else if (log.contains("Conectado") || log.contains("aceptada")) EmeraldAccent
                                    else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
