package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMeasurements(limit: Int = 100): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE source = :source ORDER BY timestamp DESC LIMIT :limit")
    fun getMeasurementsBySource(source: String, limit: Int = 100): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    suspend fun getAllMeasurementsList(): List<MeasurementEntity>

    @Query("SELECT COUNT(*) FROM measurements")
    fun getMeasurementCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<MeasurementEntity>)

    @Query("DELETE FROM measurements")
    suspend fun clearAll()

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
