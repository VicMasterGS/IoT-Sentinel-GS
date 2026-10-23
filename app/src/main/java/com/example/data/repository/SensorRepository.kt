package com.example.data.repository

import com.example.data.local.MeasurementDao
import com.example.data.local.MeasurementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorRepository(private val measurementDao: MeasurementDao) {

    val recentMeasurements: Flow<List<MeasurementEntity>> = measurementDao.getRecentMeasurements(100)

    fun getMeasurementsBySource(source: String): Flow<List<MeasurementEntity>> {
        return if (source == "TODOS") {
            measurementDao.getRecentMeasurements(100)
        } else {
            measurementDao.getMeasurementsBySource(source, 100)
        }
    }

    suspend fun saveMeasurement(
        temperatureC: Double?,
        humidityPercent: Double?,
        mq135Raw: Int?,
        airQualityStatus: String,
        source: String,
        deviceId: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val entity = MeasurementEntity(
            timestamp = timestamp,
            temperatureC = temperatureC,
            humidityPercent = humidityPercent,
            mq135Raw = mq135Raw,
            airQualityStatus = airQualityStatus,
            source = source,
            deviceId = deviceId
        )
        measurementDao.insert(entity)
    }

    suspend fun deleteMeasurementById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            measurementDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        measurementDao.clearAll()
    }

    suspend fun exportToCsv(): String = withContext(Dispatchers.IO) {
        val list = measurementDao.getAllMeasurementsList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,FechaHora,Timestamp,Dispositivo,Origen,Temperatura_C,Humedad_Pct,MQ135_Raw,Estado_Aire\n")
        for (item in list) {
            val dateStr = dateFormat.format(Date(item.timestamp))
            val temp = item.temperatureC?.toString() ?: "N/A"
            val hum = item.humidityPercent?.toString() ?: "N/A"
            val mq = item.mq135Raw?.toString() ?: "N/A"
            sb.append("${item.id},\"$dateStr\",${item.timestamp},\"${item.deviceId}\",\"${item.source}\",$temp,$hum,$mq,\"${item.airQualityStatus}\"\n")
        }
        sb.toString()
    }
}
