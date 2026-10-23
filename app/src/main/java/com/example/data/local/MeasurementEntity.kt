package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Double?,
    val humidityPercent: Double?,
    val mq135Raw: Int?,
    val airQualityStatus: String,
    val source: String, // "REAL" or "SIMULATED"
    val deviceId: String
)
