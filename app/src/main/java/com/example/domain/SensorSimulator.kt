package com.example.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.roundToInt

data class SimulationConfig(
    val simulateDht11: Boolean = true,
    val simulateMq135: Boolean = true,
    val baseTemperature: Double = 24.0,
    val tempFluctuation: Double = 1.5,
    val baseHumidity: Double = 52.0,
    val humFluctuation: Double = 3.0,
    val baseMq135Raw: Int = 380,
    val mqFluctuation: Int = 40,
    val simulateSpikes: Boolean = false,
    val updateIntervalMs: Long = 1500L
)

data class SimulatedSensorReading(
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Double?,
    val humidityPercent: Double?,
    val mq135Raw: Int?,
    val airQualityStatus: String,
    val isRunning: Boolean
)

/**
 * High-fidelity mathematical simulator for DHT11 and MQ-135 sensors.
 * Runs completely independent of network, Bluetooth, or physical hardware.
 */
class SensorSimulator(private val scope: CoroutineScope) {
    private val random = Random()

    private val _config = MutableStateFlow(SimulationConfig())
    val config: StateFlow<SimulationConfig> = _config.asStateFlow()

    private val _currentReading = MutableStateFlow(
        SimulatedSensorReading(
            temperatureC = 24.2,
            humidityPercent = 51.5,
            mq135Raw = 385,
            airQualityStatus = "NORMAL",
            isRunning = false
        )
    )
    val currentReading: StateFlow<SimulatedSensorReading> = _currentReading.asStateFlow()

    private var simulationJob: Job? = null

    val isRunning: Boolean
        get() = simulationJob?.isActive == true

    fun start() {
        if (isRunning) return
        simulationJob = scope.launch(Dispatchers.Default) {
            _currentReading.value = _currentReading.value.copy(isRunning = true)
            while (isActive) {
                generateNextTick()
                delay(_config.value.updateIntervalMs)
            }
        }
    }

    fun pause() {
        simulationJob?.cancel()
        simulationJob = null
        _currentReading.value = _currentReading.value.copy(isRunning = false)
    }

    fun stop() {
        pause()
    }

    fun reset() {
        pause()
        _config.value = SimulationConfig()
        _currentReading.value = SimulatedSensorReading(
            temperatureC = 24.0,
            humidityPercent = 52.0,
            mq135Raw = 380,
            airQualityStatus = "NORMAL",
            isRunning = false
        )
    }

    fun updateConfig(modifier: (SimulationConfig) -> SimulationConfig) {
        _config.value = modifier(_config.value)
    }

    private fun generateNextTick() {
        val conf = _config.value

        val temp = if (conf.simulateDht11) {
            val noise = (random.nextDouble() * 2 - 1) * conf.tempFluctuation
            val value = (conf.baseTemperature + noise).coerceIn(-10.0, 60.0)
            ((value * 10).roundToInt()) / 10.0
        } else null

        val hum = if (conf.simulateDht11) {
            val noise = (random.nextDouble() * 2 - 1) * conf.humFluctuation
            val value = (conf.baseHumidity + noise).coerceIn(10.0, 95.0)
            ((value * 10).roundToInt()) / 10.0
        } else null

        val mqRaw = if (conf.simulateMq135) {
            val noise = (random.nextInt(conf.mqFluctuation * 2 + 1) - conf.mqFluctuation)
            val spike = if (conf.simulateSpikes && random.nextInt(10) == 0) 350 else 0
            (conf.baseMq135Raw + noise + spike).coerceIn(50, 4095)
        } else null

        val airStatus = when {
            mqRaw == null -> "NORMAL"
            mqRaw > 750 -> "ALERTA"
            mqRaw > 450 -> "PRECAUCIÓN"
            else -> "NORMAL"
        }

        _currentReading.value = SimulatedSensorReading(
            timestamp = System.currentTimeMillis(),
            temperatureC = temp,
            humidityPercent = hum,
            mq135Raw = mqRaw,
            airQualityStatus = airStatus,
            isRunning = true
        )
    }
}
