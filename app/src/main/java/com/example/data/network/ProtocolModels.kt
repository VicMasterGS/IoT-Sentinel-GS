package com.example.data.network

import org.json.JSONObject

/**
 * Protocol definition for IoT Sentinel communication between ESP32 and Android devices.
 * Compliant with CE 2.1.2 & CE 2.1.4.
 *
 * Example message:
 * {"protocolVersion":1,"deviceId":"NODE-01","role":"sensor","timestamp":1792600000000,
 *  "source":"SIMULATED","temperatureC":24.6,"humidityPercent":51.2,"mq135Raw":420,
 *  "airQualityStatus":"NORMAL"}
 */
data class SensorMessage(
    val protocolVersion: Int = 1,
    val deviceId: String,
    val role: String, // "sensor" or "station"
    val timestamp: Long = System.currentTimeMillis(),
    val source: String, // "REAL" or "SIMULATED"
    val temperatureC: Double?,
    val humidityPercent: Double?,
    val mq135Raw: Int?,
    val airQualityStatus: String
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("protocolVersion", protocolVersion)
        json.put("deviceId", deviceId)
        json.put("role", role)
        json.put("timestamp", timestamp)
        json.put("source", source)
        if (temperatureC != null) json.put("temperatureC", temperatureC)
        if (humidityPercent != null) json.put("humidityPercent", humidityPercent)
        if (mq135Raw != null) json.put("mq135Raw", mq135Raw)
        json.put("airQualityStatus", airQualityStatus)
        return json.toString()
    }

    companion object {
        fun parse(rawJson: String): Result<SensorMessage> {
            return try {
                val json = JSONObject(rawJson)
                val version = json.optInt("protocolVersion", 1)
                if (version != 1) {
                    return Result.failure(IllegalArgumentException("Versión de protocolo no soportada: $version"))
                }

                val deviceId = json.optString("deviceId", "UNKNOWN-NODE")
                val role = json.optString("role", "sensor")
                val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                val source = json.optString("source", "SIMULATED")

                val temp = if (json.has("temperatureC") && !json.isNull("temperatureC")) {
                    val t = json.getDouble("temperatureC")
                    if (t in -40.0..85.0) t else null
                } else null

                val hum = if (json.has("humidityPercent") && !json.isNull("humidityPercent")) {
                    val h = json.getDouble("humidityPercent")
                    if (h in 0.0..100.0) h else null
                } else null

                val mqRaw = if (json.has("mq135Raw") && !json.isNull("mq135Raw")) {
                    val raw = json.getInt("mq135Raw")
                    if (raw in 0..4095) raw else null
                } else null

                val airStatus = json.optString("airQualityStatus", "NORMAL")

                Result.success(
                    SensorMessage(
                        protocolVersion = version,
                        deviceId = deviceId,
                        role = role,
                        timestamp = timestamp,
                        source = source,
                        temperatureC = temp,
                        humidityPercent = hum,
                        mq135Raw = mqRaw,
                        airQualityStatus = airStatus
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

enum class ConnectionState {
    DISCONNECTED,
    LISTENING,    // Server mode waiting for incoming connection
    CONNECTING,   // Client mode establishing socket
    CONNECTED,    // Actively transmitting/receiving
    CONNECTION_LOST,
    ERROR
}

enum class TransportType {
    BLUETOOTH_RFCOMM,
    WIFI_TCP
}

enum class DeviceRole {
    MONITORING_STATION, // Receives live data
    SENSOR_NODE         // Transmits real/simulated data
}
