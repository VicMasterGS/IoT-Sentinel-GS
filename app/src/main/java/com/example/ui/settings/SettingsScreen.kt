package com.example.ui.settings

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.auth.AuthViewModel
import com.example.ui.main.IoTViewModel
import com.example.ui.theme.AccentColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class SystemPermissionState(
    val label: String,
    val badgeColor: Color,
    val shortBadge: String = when (label) {
        "Concedido" -> "Concedido"
        "Pendiente de solicitar" -> "Pendiente"
        "Denegado" -> "Denegado"
        "Requiere habilitarse desde los ajustes del sistema" -> "En Ajustes"
        else -> "No requerido"
    }
) {
    CONCEDIDO("Concedido", EmeraldAccent, "Concedido"),
    PENDIENTE_SOLICITAR("Pendiente de solicitar", AmberWarning, "Pendiente"),
    DENEGADO("Denegado", AmberWarning, "Denegado"),
    REQUIERE_AJUSTES("Requiere habilitarse desde los ajustes del sistema", RedAlert, "En Ajustes"),
    NO_NECESARIO("No necesario para la versión o función actual", Color(0xFF94A3B8), "No requerido")
}

data class PermissionItemInfo(
    val title: String,
    val permissionCode: String,
    val purpose: String,
    val state: SystemPermissionState,
    val stateDetail: String
)

@Composable
fun SettingsScreen(
    viewModel: IoTViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showFirmwareCode by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    // Lifecycle observer to re-check system permissions on return from Android Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleResumeTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleResumeTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Persist whether the user has triggered a request before
    val prefs = remember { context.getSharedPreferences("app_permissions_prefs", Context.MODE_PRIVATE) }
    var permissionRequestCount by remember { mutableIntStateOf(0) }

    // Launcher requesting strictly required runtime permission (BLUETOOTH_CONNECT on Android 12+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionRequestCount++
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "Permiso Bluetooth autorizado correctamente", Toast.LENGTH_SHORT).show()
            viewModel.refreshNetworkInfo()
        } else {
            Toast.makeText(
                context,
                "Permiso denegado. El Modo Simulación y Wi-Fi TCP continúan funcionando sin restricciones.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val bluetoothAdapter = remember {
        try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
    }
    val hasBluetoothHardware = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }
    val isEmulator = remember {
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        "google_sdk" == Build.PRODUCT
    }
    val isPhysicalRadioReady = bluetoothAdapter != null && hasBluetoothHardware && !isEmulator
    val isBluetoothRadioEnabled = try { bluetoothAdapter?.isEnabled == true } catch (_: Exception) { false }

    // Evaluates real permission state strictly from Android system APIs
    fun evaluatePermissionStatus(
        permission: String,
        isApplicableToApi: Boolean,
        isFeatureImplemented: Boolean
    ): Pair<SystemPermissionState, String> {
        if (!isApplicableToApi) {
            return SystemPermissionState.NO_NECESARIO to "No requerido en Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        }
        if (!isFeatureImplemented) {
            val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            return if (isGranted) {
                SystemPermissionState.CONCEDIDO to "Concedido (Opcional, no requerido para la función actual)"
            } else {
                SystemPermissionState.NO_NECESARIO to "No requerido ni declarado: la app enlaza con dispositivos ya emparejados (ISO 27400)"
            }
        }

        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            val note = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                "Concedido automáticamente por el sistema al instalar el APK (Permiso normal de instalación)"
            } else {
                "Concedido por el usuario en tiempo de ejecución para RFCOMM SPP"
            }
            return SystemPermissionState.CONCEDIDO to note
        }

        val hasRequestedBefore = prefs.getBoolean("requested_$permission", false) || permissionRequestCount > 0
        if (!hasRequestedBefore) {
            return SystemPermissionState.PENDIENTE_SOLICITAR to "Pendiente de solicitar autorización al usuario"
        }

        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } ?: false

        return if (shouldShowRationale) {
            SystemPermissionState.DENEGADO to "Denegado por el usuario (se puede solicitar nuevamente)"
        } else {
            SystemPermissionState.REQUIERE_AJUSTES to "Requiere habilitarse manualmente desde los ajustes de la aplicación en el sistema"
        }
    }

    // Evaluated list of permissions relevant to implemented features
    val permissionItems = remember(lifecycleResumeTrigger, permissionRequestCount) {
        val list = mutableListOf<PermissionItemInfo>()

        // 1. Bluetooth: Conectar con dispositivos cercanos (Único permiso necesario para RFCOMM entre celulares)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val (state, detail) = evaluatePermissionStatus(
                permission = Manifest.permission.BLUETOOTH_CONNECT,
                isApplicableToApi = true,
                isFeatureImplemented = true
            )
            list.add(
                PermissionItemInfo(
                    title = "Bluetooth: Conectar dispositivos",
                    permissionCode = "BLUETOOTH_CONNECT (API 31+)",
                    purpose = "Requerido para enlazar y transferir telemetría con la estación o teléfono secundario mediante socket RFCOMM SPP.",
                    state = state,
                    stateDetail = detail
                )
            )
        } else {
            val (state, detail) = evaluatePermissionStatus(
                permission = Manifest.permission.BLUETOOTH,
                isApplicableToApi = true,
                isFeatureImplemented = true
            )
            list.add(
                PermissionItemInfo(
                    title = "Bluetooth: Conectar dispositivos",
                    permissionCode = "BLUETOOTH (Normal / API ≤ 30)",
                    purpose = "Permiso normal de sistema en Android 11 o inferior. Concedido automáticamente al instalar el APK.",
                    state = state,
                    stateDetail = detail
                )
            )
        }

        // 2. Bluetooth: Búsqueda y Escaneo de dispositivos no emparejados (BLUETOOTH_SCAN)
        list.add(
            PermissionItemInfo(
                title = "Bluetooth: Búsqueda y escaneo",
                permissionCode = "BLUETOOTH_SCAN",
                purpose = "Búsqueda activa de dispositivos desconocidos. No requerida ni declarada: la aplicación enlaza exclusivamente con dispositivos emparejados en el sistema Android (Mínimo Privilegio ISO 27400).",
                state = SystemPermissionState.NO_NECESARIO,
                stateDetail = "No necesario para la función actual (evita accesos no justificados)"
            )
        )

        // 3. Red Local Wi-Fi (Sockets TCP) e Internet
        list.add(
            PermissionItemInfo(
                title = "Red local Wi-Fi & Sockets TCP",
                permissionCode = "INTERNET & ACCESS_NETWORK_STATE",
                purpose = "Comunicación TCP entre dos celulares o ESP32 mediante dirección IP local. Permisos estándar de instalación que no requieren diálogo emergente.",
                state = SystemPermissionState.CONCEDIDO,
                stateDetail = "Concedido en la instalación del APK (Permiso de red estándar)"
            )
        )

        list
    }

    // Pre-request rationale dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Permiso de Dispositivos Cercanos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "IoT Sentinel requiere el permiso de Bluetooth 'Dispositivos cercanos' para establecer la comunicación RFCOMM con tu estación de monitoreo o teléfono secundario.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Uso exclusivo: Transmisión de telemetría de sensores DHT11 y MQ-135.\n• Privacidad: No se accede a la ubicación geográfica ni a archivos del dispositivo.\n• Si prefieres no concederlo, el Modo Simulación y Wi-Fi TCP siguen disponibles.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            prefs.edit().putBoolean("requested_${Manifest.permission.BLUETOOTH_CONNECT}", true).apply()
                            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                        }
                    },
                    modifier = Modifier.testTag("confirm_request_permission_button")
                ) {
                    Text("Autorizar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPermissionRationaleDialog = false },
                    modifier = Modifier.testTag("dismiss_request_permission_button")
                ) {
                    Text("Ahora no")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "¿Cerrar Sesión?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Se cerrará la sesión activa para el usuario '${authState.currentUser?.username ?: ""}'.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tus credenciales cifradas y los datos del historial de sensores se conservarán de forma segura en este dispositivo. Podrás volver a iniciar sesión cuando lo desees.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.selectTab(0)
                        authViewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_logout_button")
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Text(
            text = "Ajustes y Seguridad",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent
        )

        // ==========================================
        // 1. SECCIÓN CUENTA (User Account & Session)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_section_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Cuenta",
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cuenta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val sessionTimeStr = authState.currentUser?.loginTimestamp?.let {
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(it))
                } ?: "N/D"

                // Session Information Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Estado de sesión:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (authState.isAuthenticated) "● Activa y autenticada" else "Inactiva",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (authState.isAuthenticated) EmeraldAccent else RedAlert
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Usuario:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = authState.currentUser?.username ?: "Invitado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Inicio de sesión:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = sessionTimeStr,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Almacenamiento seguro:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "KeyStore AES-GCM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Logout Button
                Button(
                    onClick = { showLogoutConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cerrar sesión",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 2. PERMISOS DE LA APLICACIÓN
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_permissions_section_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permisos de la aplicación",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Android OS version badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Entorno de Ejecución & Diagnóstico de Hardware Real vs Emulado
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPhysicalRadioReady) {
                            EmeraldAccent.copy(alpha = 0.12f)
                        } else {
                            AmberWarning.copy(alpha = 0.12f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isPhysicalRadioReady) EmeraldAccent.copy(alpha = 0.4f) else AmberWarning.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPhysicalRadioReady) Icons.Default.Bluetooth else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isPhysicalRadioReady) EmeraldAccent else AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPhysicalRadioReady) "Dispositivo Físico Android Detectado" else "Entorno Emulado / Cloud Studio",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPhysicalRadioReady) EmeraldAccent else AmberWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val environmentExplanation = if (isPhysicalRadioReady) {
                            "Hardware Bluetooth físico detectado y disponible. Estado del radio: ${if (isBluetoothRadioEnabled) "Encendido" else "Apagado en ajustes"}. Listo para enlace RFCOMM entre celulares."
                        } else {
                            "El hardware de radiofrecuencia Bluetooth no está virtualizado en este entorno emulado. El estado de permisos refleja fielmente las API reales de Android sin datos inventados. Las pruebas de transmisión RFCOMM entre teléfonos físicos deben validarse instalando el APK en dispositivos reales. Para pruebas aquí, use Wi-Fi TCP o Simulación."
                        }
                        Text(
                            text = environmentExplanation,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Separación de Capas (Seguridad y Conectividad - ISO 27400)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Arquitectura de Conectividad (Capas Independientes)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("1. Permiso OS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val primaryState = permissionItems.firstOrNull()?.state ?: SystemPermissionState.NO_NECESARIO
                                Text(
                                    text = primaryState.label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryState.badgeColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("2. Radio Bluetooth", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val radioText = when {
                                    bluetoothAdapter == null -> "No disponible"
                                    isBluetoothRadioEnabled -> "Encendido"
                                    else -> "Apagado"
                                }
                                val radioColor = if (isBluetoothRadioEnabled) EmeraldAccent else AmberWarning
                                Text(
                                    text = radioText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = radioColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("3. Enlace RFCOMM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val (connLabel, connColor) = when (uiState.connectionState) {
                                    com.example.data.network.ConnectionState.CONNECTED -> "Conectado" to EmeraldAccent
                                    com.example.data.network.ConnectionState.CONNECTING -> "Conectando" to CyanAccent
                                    com.example.data.network.ConnectionState.LISTENING -> "Escuchando" to CyanAccent
                                    com.example.data.network.ConnectionState.CONNECTION_LOST -> "Perdida" to RedAlert
                                    com.example.data.network.ConnectionState.ERROR -> "Error" to RedAlert
                                    com.example.data.network.ConnectionState.DISCONNECTED -> "Desconectado" to MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = connLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = connColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nota ISO 27400: Conceder el permiso no activa automáticamente el radio ni establece una conexión. Cada nivel se gestiona de forma autónoma para garantizar mínimo privilegio.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real OS permissions status
                permissionItems.forEachIndexed { index, item ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    PermissionItemCard(item = item)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                val primaryPermState = permissionItems.firstOrNull()?.state ?: SystemPermissionState.NO_NECESARIO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (primaryPermState) {
                        SystemPermissionState.CONCEDIDO -> {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Los permisos necesarios ya están autorizados", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_permissions_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "✓ Autorizado",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        SystemPermissionState.REQUIERE_AJUSTES -> {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_permissions_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Habilitar en Ajustes",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        SystemPermissionState.PENDIENTE_SOLICITAR, SystemPermissionState.DENEGADO -> {
                            Button(
                                onClick = {
                                    showPermissionRationaleDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_permissions_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Solicitar Permiso",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        SystemPermissionState.NO_NECESARIO -> {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "No se requieren permisos de ejecución en esta versión", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_permissions_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "No requerido",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_app_settings_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajustes del Sistema", fontSize = 11.5.sp)
                    }
                }
            }
        }

        // ==========================================
        // 3. UMBRALES Y ALERTAS AMBIENTALES
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("thresholds_config_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Umbrales y Alertas Ambientales", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = viewModel.alertEngine.config.alertsEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateThresholds { it.copy(alertsEnabled = checked) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AmberWarning),
                        modifier = Modifier.testTag("switch_alerts_enabled")
                    )
                }

                if (viewModel.alertEngine.config.alertsEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Temperatura Máxima Crítica: ${viewModel.alertEngine.config.tempMaxC.roundToInt()} °C",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = viewModel.alertEngine.config.tempMaxC.toFloat(),
                        onValueChange = { viewModel.updateThresholds { conf -> conf.copy(tempMaxC = it.toDouble()) } },
                        valueRange = 25f..50f,
                        colors = SliderDefaults.colors(thumbColor = RedAlert, activeTrackColor = RedAlert),
                        modifier = Modifier.testTag("slider_temp_max")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Humedad Máxima Relativa: ${viewModel.alertEngine.config.humidityMaxPercent.roundToInt()} %",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = viewModel.alertEngine.config.humidityMaxPercent.toFloat(),
                        onValueChange = { viewModel.updateThresholds { conf -> conf.copy(humidityMaxPercent = it.toDouble()) } },
                        valueRange = 50f..95f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                        modifier = Modifier.testTag("slider_hum_max")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Umbral Alerta Crítica MQ-135: ${viewModel.alertEngine.config.mqAlertRaw} ADC",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = viewModel.alertEngine.config.mqAlertRaw.toFloat(),
                        onValueChange = { viewModel.updateThresholds { conf -> conf.copy(mqAlertRaw = it.toInt()) } },
                        valueRange = 500f..1200f,
                        colors = SliderDefaults.colors(thumbColor = RedAlert, activeTrackColor = RedAlert),
                        modifier = Modifier.testTag("slider_mq_alert")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibración Háptica al Disparar Alerta", fontSize = 12.sp)
                        Switch(
                            checked = viewModel.alertEngine.config.vibrationEnabled,
                            onCheckedChange = { checked ->
                                viewModel.updateThresholds { it.copy(vibrationEnabled = checked) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent),
                            modifier = Modifier.testTag("switch_vibration")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. ISO 27400 INFORMACIÓN & SEGURIDAD
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Principios de Seguridad y Privacidad — ISO 27400", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Minimización de Datos: Solo se transmiten métricas ambientales (temperatura, humedad, gas ADC) y timestamps necesarios. No se recaban identificadores de hardware permanentes ni datos de ubicación geográfica (usesPermissionFlags=\"neverForLocation\").\n" +
                            "• Criptografía por Diseño: Almacenamiento seguro de contraseñas mediante derivación PBKDF2-HMAC-SHA256 con 65.536 iteraciones y salts aleatorios de 32 bytes. Clave de sesión cifrada en reposo mediante Android KeyStore (AES-GCM-256).\n" +
                            "• Transparencia y Consentimiento: Se explican previamente los permisos solicitados y se ofrece una alternativa de simulación completamente funcional si se deniegan.\n" +
                            "• Aviso Aclaratorio: La aplicación adopta estos lineamientos de seguridad conforme a las directrices de diseño IoT de ISO 27400, sin constituir una certificación formal expedida por entidad auditora.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        // ==========================================
        // 5. HARDWARE ESP32 Y FIRMWARE
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ElectricalServices, contentDescription = null, tint = AmberWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Integración Física con ESP32", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Precauciones Eléctricas Importantes:\n" +
                            "• El ESP32 opera a 3.3V en sus entradas GPIO analógicas. Los pines ADC NO toleran 5V directos.\n" +
                            "• El sensor MQ-135 requiere alimentación de 5V para el calefactor interno. Su salida analógica A0 puede entregar hasta 5V. Se DEBE utilizar un divisor de tensión resistivo (ej. R1=1kΩ hacia A0 y R2=2kΩ hacia GND) para que la señal en el pin GPIO34 del ESP32 no supere los 3.3V.\n" +
                            "• El sensor DHT11 se alimenta a 3.3V o 5V y su pin de señal de datos se conecta a GPIO4 con una resistencia pull-up de 4.7kΩ/10kΩ a VCC.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showFirmwareCode = !showFirmwareCode },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_firmware_code_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showFirmwareCode) "Ocultar Firmware Arduino ESP32" else "Ver Código Firmware Arduino para ESP32")
                }

                AnimatedVisibility(visible = showFirmwareCode) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        val firmwareCode = esp32FirmwareCode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Código Firmware C++:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ESP32 Firmware", firmwareCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Firmware copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier.testTag("copy_firmware_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar Código", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF030712))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = firmwareCode,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF67E8F9)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. APARIENCIA / PERSONALIZACIÓN ESTÉTICA
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appearance_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apariencia",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Light / Dark Theme Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (uiState.isDarkMode) "Tema Oscuro (Grafito / Deep Navy)" else "Tema Claro",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Predeterminado: Oscuro para entornos industriales",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("switch_dark_mode")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accent Color Selector Header
                Text(
                    text = "Color de Acento:",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Afecta a botones principales, pestañas seleccionadas e indicadores destacados.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4 Accent Colors Options
                AccentColor.entries.forEach { option ->
                    val isSelected = uiState.accentColor == option
                    val testTagKey = when (option) {
                        AccentColor.CYAN -> "accent_color_cyan"
                        AccentColor.GREEN -> "accent_color_green"
                        AccentColor.VIOLET -> "accent_color_violet"
                        AccentColor.AMBER -> "accent_color_amber"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setAccentColor(option) }
                            .testTag(testTagKey),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                option.previewColor.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, option.previewColor)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Color preview circle swatch
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(option.previewColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = option.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (option == AccentColor.CYAN) "Predeterminado" else "Acento industrial",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(option.previewColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = option.onPrimaryDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Clarification note regarding sensor alarm colors
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aviso de Seguridad: Los colores e indicadores de alerta de los sensores (Verde: Normal, Ámbar: Precaución, Rojo: Alerta Crítica) se mantienen siempre fijos con sus etiquetas e iconos para garantizar la identificación inequívoca del estado ambiental.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun PermissionItemCard(item: PermissionItemInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title on the left, Compact Badge on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(item.state.badgeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.state.shortBadge,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.state.badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated technical tag chip with no awkward horizontal wrapping
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.permissionCode,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Purpose / Explanation
            Text(
                text = item.purpose,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // System State Detail Row with indicator dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(item.state.badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.stateDetail,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = item.state.badgeColor
                )
            }
        }
    }
}

private const val esp32FirmwareCode = """
/*
 * IoT Sentinel - ESP32 Firmware Reference
 * DHT11 + MQ-135 con Bluetooth Serial y TCP Socket
 * Protocolo JSON delimitado por salto de línea (\n)
 */

#include "BluetoothSerial.h"
#include <WiFi.h>
#include "DHT.h"

#define DHTPIN 4
#define DHTTYPE DHT11
#define MQ135_PIN 34 // ADC1 GPIO34 con divisor de tensión a 3.3V

BluetoothSerial SerialBT;
DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(115200);
  dht.begin();
  
  // Iniciar Bluetooth Classic SPP con nombre visible
  SerialBT.begin("ESP32-IoTSentinel");
  Serial.println("Bluetooth SPP iniciado: ESP32-IoTSentinel");
}

void loop() {
  float temp = dht.readTemperature();
  float hum = dht.readHumidity();
  int rawMq = analogRead(MQ135_PIN); // 0 - 4095

  // Verificación de lectura válida
  bool validDht = !isnan(temp) && !isnan(hum);

  String airStatus = "NORMAL";
  if (rawMq > 750) airStatus = "ALERTA";
  else if (rawMq > 450) airStatus = "PRECAUCION";

  // Construir trama JSON del protocolo IoT Sentinel
  String json = "{\"protocolVersion\":1,\"deviceId\":\"ESP32-01\",\"role\":\"sensor\",";
  json += "\"timestamp\":" + String(millis()) + ",";
  json += "\"source\":\"REAL\",";
  if (validDht) {
    json += "\"temperatureC\":" + String(temp, 1) + ",";
    json += "\"humidityPercent\":" + String(hum, 1) + ",";
  }
  json += "\"mq135Raw\":" + String(rawMq) + ",";
  json += "\"airQualityStatus\":\"" + airStatus + "\"}";

  // Transmitir por Bluetooth y Serial
  SerialBT.println(json);
  Serial.println(json);

  delay(1500); // Tasa de refresco 1.5s
}
"""
