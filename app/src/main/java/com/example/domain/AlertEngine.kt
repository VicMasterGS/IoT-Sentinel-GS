package com.example.domain

data class ThresholdConfig(
    val tempMaxC: Double = 30.0,
    val tempMinC: Double = 16.0,
    val humidityMaxPercent: Double = 70.0,
    val humidityMinPercent: Double = 30.0,
    val mqCautionRaw: Int = 450,
    val mqAlertRaw: Int = 750,
    val alertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

enum class SystemHealthStatus {
    NORMAL,
    PRECAUCIÓN,
    ALERTA,
    SIN_DATOS
}

data class ActiveAlert(
    val title: String,
    val description: String,
    val level: SystemHealthStatus,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Threshold evaluation engine for IoT environmental monitoring.
 */
class AlertEngine(
    var config: ThresholdConfig = ThresholdConfig()
) {
    fun evaluate(
        temperatureC: Double?,
        humidityPercent: Double?,
        mq135Raw: Int?
    ): Pair<SystemHealthStatus, List<ActiveAlert>> {
        if (temperatureC == null && humidityPercent == null && mq135Raw == null) {
            return Pair(SystemHealthStatus.SIN_DATOS, emptyList())
        }

        if (!config.alertsEnabled) {
            return Pair(SystemHealthStatus.NORMAL, emptyList())
        }

        val alerts = mutableListOf<ActiveAlert>()
        var highestStatus = SystemHealthStatus.NORMAL

        // Temperature checks
        if (temperatureC != null) {
            if (temperatureC >= config.tempMaxC) {
                highestStatus = SystemHealthStatus.ALERTA
                alerts.add(
                    ActiveAlert(
                        title = "Temperatura Crítica Alta",
                        description = "Lectura de ${temperatureC}°C supera el límite superior (${config.tempMaxC}°C)",
                        level = SystemHealthStatus.ALERTA
                    )
                )
            } else if (temperatureC <= config.tempMinC) {
                highestStatus = SystemHealthStatus.PRECAUCIÓN
                alerts.add(
                    ActiveAlert(
                        title = "Temperatura Baja",
                        description = "Lectura de ${temperatureC}°C por debajo del umbral mínimo (${config.tempMinC}°C)",
                        level = SystemHealthStatus.PRECAUCIÓN
                    )
                )
            }
        }

        // Humidity checks
        if (humidityPercent != null) {
            if (humidityPercent >= config.humidityMaxPercent) {
                if (highestStatus != SystemHealthStatus.ALERTA) highestStatus = SystemHealthStatus.PRECAUCIÓN
                alerts.add(
                    ActiveAlert(
                        title = "Humedad Excesiva",
                        description = "Humedad relativa ${humidityPercent}% sobrepasa el límite (${config.humidityMaxPercent}%)",
                        level = SystemHealthStatus.PRECAUCIÓN
                    )
                )
            } else if (humidityPercent <= config.humidityMinPercent) {
                if (highestStatus != SystemHealthStatus.ALERTA) highestStatus = SystemHealthStatus.PRECAUCIÓN
                alerts.add(
                    ActiveAlert(
                        title = "Ambiente Seco",
                        description = "Humedad relativa ${humidityPercent}% por debajo del mínimo recomendado (${config.humidityMinPercent}%)",
                        level = SystemHealthStatus.PRECAUCIÓN
                    )
                )
            }
        }

        // MQ-135 checks
        if (mq135Raw != null) {
            if (mq135Raw >= config.mqAlertRaw) {
                highestStatus = SystemHealthStatus.ALERTA
                alerts.add(
                    ActiveAlert(
                        title = "Alerta Calidad de Aire (MQ-135)",
                        description = "Valor analógico $mq135Raw supera el umbral crítico ($config.mqAlertRaw)",
                        level = SystemHealthStatus.ALERTA
                    )
                )
            } else if (mq135Raw >= config.mqCautionRaw) {
                if (highestStatus != SystemHealthStatus.ALERTA) highestStatus = SystemHealthStatus.PRECAUCIÓN
                alerts.add(
                    ActiveAlert(
                        title = "Precaución Calidad de Aire",
                        description = "Valor analógico $mq135Raw excede nivel preventivo ($config.mqCautionRaw)",
                        level = SystemHealthStatus.PRECAUCIÓN
                    )
                )
            }
        }

        return Pair(highestStatus, alerts)
    }
}
