package com.example.ui.main

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MeasurementEntity
import com.example.data.network.BluetoothRfcommService
import com.example.data.network.ConnectionState
import com.example.data.network.DeviceRole
import com.example.data.network.LocalTcpService
import com.example.data.network.SensorMessage
import com.example.data.network.TransportType
import com.example.data.repository.SensorRepository
import com.example.domain.ActiveAlert
import com.example.domain.AlertEngine
import com.example.domain.SensorSimulator
import com.example.domain.SimulationConfig
import com.example.domain.SystemHealthStatus
import com.example.domain.ThresholdConfig
import com.example.ui.theme.AccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.LinkedList

data class LiveReadingState(
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Double? = 24.2,
    val humidityPercent: Double? = 51.5,
    val mq135Raw: Int? = 385,
    val airQualityStatus: String = "NORMAL",
    val source: String = "SIMULATED", // "REAL" or "SIMULATED"
    val deviceId: String = "LOCAL-SIM"
)

data class ChartPoint(
    val timestamp: Long,
    val value: Float
)

data class IoTUiState(
    val currentTab: Int = 0,
    val deviceRole: DeviceRole = DeviceRole.MONITORING_STATION,
    val transportType: TransportType = TransportType.WIFI_TCP,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isSimulationActive: Boolean = false,
    val liveReading: LiveReadingState = LiveReadingState(),
    val systemHealth: SystemHealthStatus = SystemHealthStatus.NORMAL,
    val activeAlerts: List<ActiveAlert> = emptyList(),
    val localIpAddress: String = "127.0.0.1",
    val tcpPort: Int = 8088,
    val targetIp: String = "192.168.1.",
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val selectedBluetoothDevice: BluetoothDevice? = null,
    val logs: List<String> = emptyList(),
    val packetsSentCount: Int = 0,
    val packetsReceivedCount: Int = 0,
    val lastPacketTimestamp: Long = 0L,
    val historyFilterSource: String = "TODOS",
    val isDarkMode: Boolean = true,
    val accentColor: AccentColor = AccentColor.CYAN
)

class IoTViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("iot_sentinel_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getInstance(application)
    val sensorRepository = SensorRepository(database.measurementDao())

    val bluetoothService = BluetoothRfcommService(application, viewModelScope)
    val tcpService = LocalTcpService(viewModelScope)
    val sensorSimulator = SensorSimulator(viewModelScope)
    val alertEngine = AlertEngine()

    private val _uiState = MutableStateFlow(
        IoTUiState(
            isDarkMode = prefs.getBoolean("key_is_dark_mode", true),
            accentColor = AccentColor.fromId(prefs.getString("key_accent_color", AccentColor.CYAN.id))
        )
    )
    val uiState: StateFlow<IoTUiState> = _uiState.asStateFlow()

    // Chart histories (last 30 samples)
    private val _temperatureHistory = MutableStateFlow<List<ChartPoint>>(emptyList())
    val temperatureHistory: StateFlow<List<ChartPoint>> = _temperatureHistory.asStateFlow()

    private val _humidityHistory = MutableStateFlow<List<ChartPoint>>(emptyList())
    val humidityHistory: StateFlow<List<ChartPoint>> = _humidityHistory.asStateFlow()

    private val _mq135History = MutableStateFlow<List<ChartPoint>>(emptyList())
    val mq135History: StateFlow<List<ChartPoint>> = _mq135History.asStateFlow()

    private val _historyFilter = MutableStateFlow("TODOS")
    val measurementHistory: StateFlow<List<MeasurementEntity>> = _historyFilter.flatMapLatest { filter ->
        sensorRepository.getMeasurementsBySource(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var nodeTransmitterJob: Job? = null
    private var stationWatchdogJob: Job? = null

    init {
        refreshNetworkInfo()
        observeNetworkServices()
        observeSimulator()
        startStationWatchdog()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(currentTab = tabIndex)
    }

    fun setDarkMode(isDark: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = isDark)
        prefs.edit().putBoolean("key_is_dark_mode", isDark).apply()
    }

    fun setAccentColor(accent: AccentColor) {
        _uiState.value = _uiState.value.copy(accentColor = accent)
        prefs.edit().putString("key_accent_color", accent.id).apply()
    }

    fun setDeviceRole(role: DeviceRole) {
        if (_uiState.value.deviceRole != role) {
            disconnect()
            _uiState.value = _uiState.value.copy(deviceRole = role)
        }
    }

    fun setTransportType(transport: TransportType) {
        if (_uiState.value.transportType != transport) {
            disconnect()
            _uiState.value = _uiState.value.copy(transportType = transport)
        }
    }

    fun setTargetIp(ip: String) {
        _uiState.value = _uiState.value.copy(targetIp = ip)
    }

    fun setTcpPort(port: Int) {
        _uiState.value = _uiState.value.copy(tcpPort = port)
    }

    fun setSelectedBluetoothDevice(device: BluetoothDevice?) {
        _uiState.value = _uiState.value.copy(selectedBluetoothDevice = device)
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
        _uiState.value = _uiState.value.copy(historyFilterSource = filter)
    }

    fun refreshNetworkInfo() {
        val ip = tcpService.getLocalIpAddress()
        val paired = bluetoothService.getPairedDevices()
        _uiState.value = _uiState.value.copy(
            localIpAddress = ip,
            pairedDevices = paired
        )
    }

    private fun observeNetworkServices() {
        // Bluetooth State
        viewModelScope.launch {
            bluetoothService.connectionState.collect { state ->
                if (_uiState.value.transportType == TransportType.BLUETOOTH_RFCOMM) {
                    _uiState.value = _uiState.value.copy(connectionState = state)
                    handleConnectionStateChanged(state)
                }
            }
        }
        // TCP State
        viewModelScope.launch {
            tcpService.connectionState.collect { state ->
                if (_uiState.value.transportType == TransportType.WIFI_TCP) {
                    _uiState.value = _uiState.value.copy(connectionState = state)
                    handleConnectionStateChanged(state)
                }
            }
        }
        // Incoming messages from Bluetooth
        viewModelScope.launch {
            bluetoothService.incomingMessages.collect { rawJson ->
                if (_uiState.value.transportType == TransportType.BLUETOOTH_RFCOMM) {
                    processIncomingMessage(rawJson)
                }
            }
        }
        // Incoming messages from TCP
        viewModelScope.launch {
            tcpService.incomingMessages.collect { rawJson ->
                if (_uiState.value.transportType == TransportType.WIFI_TCP) {
                    processIncomingMessage(rawJson)
                }
            }
        }
        // Bluetooth Logs
        viewModelScope.launch {
            bluetoothService.logFlow.collect { log ->
                addLog("[BT] $log")
            }
        }
        // TCP Logs
        viewModelScope.launch {
            tcpService.logFlow.collect { log ->
                addLog("[TCP] $log")
            }
        }
    }

    private fun observeSimulator() {
        viewModelScope.launch {
            sensorSimulator.currentReading.collect { simReading ->
                _uiState.value = _uiState.value.copy(isSimulationActive = simReading.isRunning)
                // If not connected as station receiving external data, update dashboard with simulator
                if (_uiState.value.connectionState != ConnectionState.CONNECTED || _uiState.value.deviceRole == DeviceRole.SENSOR_NODE) {
                    if (simReading.isRunning) {
                        applyNewReading(
                            temp = simReading.temperatureC,
                            hum = simReading.humidityPercent,
                            mqRaw = simReading.mq135Raw,
                            airStatus = simReading.airQualityStatus,
                            source = "SIMULATED",
                            deviceId = "LOCAL-SIM",
                            timestamp = simReading.timestamp
                        )
                    }
                }
            }
        }
    }

    private fun handleConnectionStateChanged(state: ConnectionState) {
        if (state == ConnectionState.CONNECTED) {
            if (_uiState.value.deviceRole == DeviceRole.SENSOR_NODE) {
                startSensorNodeTransmitter()
            }
        } else {
            nodeTransmitterJob?.cancel()
            nodeTransmitterJob = null
        }
    }

    private fun startSensorNodeTransmitter() {
        nodeTransmitterJob?.cancel()
        nodeTransmitterJob = viewModelScope.launch(Dispatchers.IO) {
            addLog("Transmisor Nodo Sensor iniciado: enviando cada 1.5s")
            while (isActive && _uiState.value.connectionState == ConnectionState.CONNECTED) {
                val current = _uiState.value.liveReading
                val msg = SensorMessage(
                    protocolVersion = 1,
                    deviceId = "NODE-ANDROID",
                    role = "sensor",
                    timestamp = System.currentTimeMillis(),
                    source = if (_uiState.value.isSimulationActive) "SIMULATED" else current.source,
                    temperatureC = current.temperatureC,
                    humidityPercent = current.humidityPercent,
                    mq135Raw = current.mq135Raw,
                    airQualityStatus = current.airQualityStatus
                )
                val json = msg.toJsonString()
                val sent = if (_uiState.value.transportType == TransportType.BLUETOOTH_RFCOMM) {
                    bluetoothService.sendMessage(json)
                } else {
                    tcpService.sendMessage(json)
                }
                if (sent) {
                    _uiState.value = _uiState.value.copy(
                        packetsSentCount = _uiState.value.packetsSentCount + 1,
                        lastPacketTimestamp = System.currentTimeMillis()
                    )
                }
                delay(1500L)
            }
        }
    }

    private fun processIncomingMessage(rawJson: String) {
        val result = SensorMessage.parse(rawJson)
        result.onSuccess { msg ->
            _uiState.value = _uiState.value.copy(
                packetsReceivedCount = _uiState.value.packetsReceivedCount + 1,
                lastPacketTimestamp = System.currentTimeMillis()
            )
            applyNewReading(
                temp = msg.temperatureC,
                hum = msg.humidityPercent,
                mqRaw = msg.mq135Raw,
                airStatus = msg.airQualityStatus,
                source = msg.source,
                deviceId = msg.deviceId,
                timestamp = msg.timestamp
            )
        }.onFailure { err ->
            addLog("Error analizando JSON entrante: ${err.message}")
        }
    }

    private fun applyNewReading(
        temp: Double?,
        hum: Double?,
        mqRaw: Int?,
        airStatus: String,
        source: String,
        deviceId: String,
        timestamp: Long
    ) {
        val newReading = LiveReadingState(
            timestamp = timestamp,
            temperatureC = temp,
            humidityPercent = hum,
            mq135Raw = mqRaw,
            airQualityStatus = airStatus,
            source = source,
            deviceId = deviceId
        )

        // Threshold evaluation
        val (health, alerts) = alertEngine.evaluate(temp, hum, mqRaw)
        _uiState.value = _uiState.value.copy(
            liveReading = newReading,
            systemHealth = health,
            activeAlerts = alerts
        )

        if (health == SystemHealthStatus.ALERTA && alertEngine.config.vibrationEnabled) {
            triggerVibrationHaptic()
        }

        // Update chart history
        updateChartHistories(timestamp, temp, hum, mqRaw)

        // Persist to Room
        viewModelScope.launch(Dispatchers.IO) {
            sensorRepository.saveMeasurement(
                temperatureC = temp,
                humidityPercent = hum,
                mq135Raw = mqRaw,
                airQualityStatus = airStatus,
                source = source,
                deviceId = deviceId,
                timestamp = timestamp
            )
        }
    }

    private fun updateChartHistories(timestamp: Long, temp: Double?, hum: Double?, mqRaw: Int?) {
        val maxPoints = 25
        if (temp != null) {
            val list = _temperatureHistory.value.toMutableList()
            list.add(ChartPoint(timestamp, temp.toFloat()))
            if (list.size > maxPoints) list.removeAt(0)
            _temperatureHistory.value = list
        }
        if (hum != null) {
            val list = _humidityHistory.value.toMutableList()
            list.add(ChartPoint(timestamp, hum.toFloat()))
            if (list.size > maxPoints) list.removeAt(0)
            _humidityHistory.value = list
        }
        if (mqRaw != null) {
            val list = _mq135History.value.toMutableList()
            list.add(ChartPoint(timestamp, mqRaw.toFloat()))
            if (list.size > maxPoints) list.removeAt(0)
            _mq135History.value = list
        }
    }

    private fun startStationWatchdog() {
        stationWatchdogJob?.cancel()
        stationWatchdogJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(3000L)
                val state = _uiState.value.connectionState
                val role = _uiState.value.deviceRole
                val lastTime = _uiState.value.lastPacketTimestamp

                if (role == DeviceRole.MONITORING_STATION && state == ConnectionState.CONNECTED) {
                    if (lastTime > 0 && (System.currentTimeMillis() - lastTime) > 6000L) {
                        addLog("Alerta: Sin mensajes del nodo por más de 6s. Detección de pérdida de señal.")
                        _uiState.value = _uiState.value.copy(
                            connectionState = ConnectionState.CONNECTION_LOST,
                            systemHealth = SystemHealthStatus.SIN_DATOS
                        )
                    }
                }
            }
        }
    }

    // Connection Control
    fun startListeningServer() {
        refreshNetworkInfo()
        if (_uiState.value.transportType == TransportType.BLUETOOTH_RFCOMM) {
            bluetoothService.startServer()
        } else {
            tcpService.startServer(_uiState.value.tcpPort)
        }
    }

    fun connectToRemote() {
        refreshNetworkInfo()
        if (_uiState.value.transportType == TransportType.BLUETOOTH_RFCOMM) {
            val dev = _uiState.value.selectedBluetoothDevice
            if (dev != null) {
                bluetoothService.connectToDevice(dev)
            } else {
                addLog("Seleccione un dispositivo Bluetooth emparejado")
            }
        } else {
            val target = _uiState.value.targetIp.trim()
            if (target.isNotBlank()) {
                tcpService.connectToHost(target, _uiState.value.tcpPort)
            } else {
                addLog("Ingrese una dirección IP de destino válida")
            }
        }
    }

    fun disconnect() {
        bluetoothService.disconnect()
        tcpService.disconnect()
        nodeTransmitterJob?.cancel()
        nodeTransmitterJob = null
        _uiState.value = _uiState.value.copy(connectionState = ConnectionState.DISCONNECTED)
    }

    // Simulator Control
    fun startSimulation() {
        sensorSimulator.start()
    }

    fun pauseSimulation() {
        sensorSimulator.pause()
    }

    fun resetSimulation() {
        sensorSimulator.reset()
    }

    fun updateSimulationConfig(modifier: (SimulationConfig) -> SimulationConfig) {
        sensorSimulator.updateConfig(modifier)
    }

    fun updateThresholds(modifier: (ThresholdConfig) -> ThresholdConfig) {
        alertEngine.config = modifier(alertEngine.config)
    }

    fun addLog(message: String) {
        val list = _uiState.value.logs.toMutableList()
        if (list.size > 80) list.removeAt(0)
        list.add(message)
        _uiState.value = _uiState.value.copy(logs = list)
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
    }

    fun deleteMeasurement(id: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = sensorRepository.deleteMeasurementById(id)
            result.onSuccess {
                addLog("Registro #$id eliminado del historial local")
                onSuccess()
            }.onFailure { err ->
                val errorMsg = err.message ?: "No se pudo eliminar el registro de la base de datos"
                addLog("Error al eliminar registro #$id: $errorMsg")
                onError(errorMsg)
            }
        }
    }

    fun clearDatabaseHistory() {
        viewModelScope.launch {
            sensorRepository.clearHistory()
        }
    }

    private fun triggerVibrationHaptic() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(200)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
        sensorSimulator.stop()
        stationWatchdogJob?.cancel()
        stationWatchdogJob = null
    }
}
