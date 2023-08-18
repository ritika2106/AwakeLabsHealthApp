package com.example.testpart1.activities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//creates a template for heart rate information of a single heartrate object
@Entity(tableName = "heart_rate_db")
data class CachedHeartRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "heart_rate") val heartRate: Double,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
