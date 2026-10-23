package com.example.data.network

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
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Real local TCP socket service over Wi-Fi.
 * Essential for robust testing between two Android devices or ESP32 over Wi-Fi.
 * Supports:
 * - Server mode: ServerSocket listening on configurable port (default 8088).
 * - Client mode: Socket connecting to IP and port of the Monitoring Station.
 * - Local IP auto-detection.
 */
class LocalTcpService(
    private val scope: CoroutineScope
) {
    companion object {
        const val DEFAULT_PORT = 8088
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var printWriter: PrintWriter? = null
    private var workerJob: Job? = null

    /**
     * Resolves the primary local IPv4 address (e.g. 192.168.1.X) to display on UI.
     */
    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    /**
     * Start TCP Server on local port to receive sensor data.
     */
    fun startServer(port: Int = DEFAULT_PORT) {
        disconnect()
        workerJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.LISTENING
                val localIp = getLocalIpAddress()
                _logFlow.emit("Iniciando Servidor TCP en $localIp:$port...")

                val server = ServerSocket(port)
                serverSocket = server

                _logFlow.emit("Esperando conexión de nodo sensor en puerto $port...")
                val socket = server.accept()
                activeSocket = socket
                _connectionState.value = ConnectionState.CONNECTED
                val remoteIp = socket.inetAddress.hostAddress
                _logFlow.emit("Cliente conectado desde: $remoteIp")

                handleSocketIo(socket)
            } catch (e: Exception) {
                if (isActive) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _logFlow.emit("Servidor TCP finalizado: ${e.message}")
                }
            }
        }
    }

    /**
     * Connect to remote TCP Server (e.g. Monitoring Station).
     */
    fun connectToHost(host: String, port: Int = DEFAULT_PORT) {
        disconnect()
        workerJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                _logFlow.emit("Conectando a TCP $host:$port...")

                val socket = Socket(host, port)
                socket.soTimeout = 10000 // 10s socket timeout
                activeSocket = socket

                _connectionState.value = ConnectionState.CONNECTED
                _logFlow.emit("Conectado exitosamente con $host:$port")

                handleSocketIo(socket)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                _logFlow.emit("Error de conexión TCP: ${e.message}")
                disconnect()
            }
        }
    }

    private suspend fun handleSocketIo(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            printWriter = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

            while (isActive && !socket.isClosed && socket.isConnected) {
                val line = try {
                    reader.readLine()
                } catch (timeout: SocketTimeoutException) {
                    // Periodic timeout allows checking isActive
                    continue
                } ?: break

                if (line.isNotBlank()) {
                    _incomingMessages.emit(line)
                }
            }
        } catch (e: Exception) {
            _logFlow.emit("Excepción I/O TCP: ${e.message}")
        } finally {
            _connectionState.value = ConnectionState.CONNECTION_LOST
            _logFlow.emit("Conexión TCP finalizada / perdida")
            disconnectInternal()
        }
    }

    suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val writer = printWriter
            if (writer != null && activeSocket?.isConnected == true && activeSocket?.isClosed == false) {
                writer.println(message)
                writer.flush()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            _logFlow.emit("Error enviando TCP: ${e.message}")
            false
        }
    }

    fun disconnect() {
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
