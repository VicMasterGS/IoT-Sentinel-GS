package com.example.data.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID

/**
 * Real Bluetooth Classic RFCOMM service.
 * Supports both:
 * 1. Client mode: Connects to a paired ESP32 or Android phone running RFCOMM server.
 * 2. Server mode: Listens with BluetoothServerSocket for incoming RFCOMM connection from another phone or ESP32.
 * Uses standard Serial Port Profile (SPP) UUID: 00001101-0000-1000-8000-00805F9B34FB.
 */
class BluetoothRfcommService(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        // Standard SPP UUID compatible with ESP32 BluetoothSerial and Android RFCOMM
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SERVICE_NAME = "IoTSentinelSPP"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()

    private var activeSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var workerJob: Job? = null
    private var printWriter: PrintWriter? = null

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Start RFCOMM server to listen for incoming connections from another phone.
     */
    @SuppressLint("MissingPermission")
    fun startServer() {
        disconnect()
        isManualDisconnect = false
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.ERROR
            scope.launch { _logFlow.emit("Bluetooth no está disponible o habilitado") }
            return
        }

        workerJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.LISTENING
                _logFlow.emit("Iniciando servidor RFCOMM SPP... UUID: $SPP_UUID")
                val server = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
                serverSocket = server

                val socket = server.accept() // Blocks until client connects or timeout/closed
                server.close()
                serverSocket = null

                activeSocket = socket
                _connectionState.value = ConnectionState.CONNECTED
                val remoteName = try { socket.remoteDevice.name ?: socket.remoteDevice.address } catch (e: Exception) { "Remoto" }
                _logFlow.emit("Conexión entrante aceptada de: $remoteName")

                handleSocketIo(socket)
            } catch (e: Exception) {
                if (isActive) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _logFlow.emit("Servidor RFCOMM detenido: ${e.message}")
                }
            }
        }
    }

    /**
     * Connect to a paired device as an RFCOMM client.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        disconnect()
        isManualDisconnect = false
        workerJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                val devName = try { device.name ?: device.address } catch (e: Exception) { device.address }
                _logFlow.emit("Conectando a dispositivo: $devName...")

                // Cancel discovery before connecting as it slows down connection
                try { bluetoothAdapter?.cancelDiscovery() } catch (_: Exception) {}

                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect() // Blocks on Dispatchers.IO

                activeSocket = socket
                _connectionState.value = ConnectionState.CONNECTED
                _logFlow.emit("Conectado exitosamente con $devName")

                handleSocketIo(socket)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                _logFlow.emit("Fallo de conexión RFCOMM: ${e.message}")
                disconnect()
            }
        }
    }

    @Volatile
    private var isManualDisconnect = false

    private suspend fun handleSocketIo(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        try {
            printWriter = PrintWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8), true)
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

            while (isActive && socket.isConnected) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    _incomingMessages.emit(line)
                }
            }
        } catch (e: Exception) {
            if (!isManualDisconnect) {
                _logFlow.emit("Lectura RFCOMM finalizada: ${e.message}")
            }
        } finally {
            if (!isManualDisconnect) {
                _connectionState.value = ConnectionState.CONNECTION_LOST
                _logFlow.emit("Conexión RFCOMM perdida")
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
            disconnectInternal()
            isManualDisconnect = false
        }
    }

    /**
     * Transmit a newline-delimited message over the active socket.
     */
    suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val writer = printWriter
            if (writer != null && activeSocket?.isConnected == true) {
                writer.println(message)
                writer.flush()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            _logFlow.emit("Error al enviar mensaje RFCOMM: ${e.message}")
            false
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        workerJob?.cancel()
        workerJob = null
        disconnectInternal()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun disconnectInternal() {
        try { printWriter?.close() } catch (_: Exception) {}
        printWriter = null
        try { activeSocket?.close() } catch (_: Exception) {}
        activeSocket = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }
}
