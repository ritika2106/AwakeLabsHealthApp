package com.example.testpart1.activities

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedHeartRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(heartRate: CachedHeartRate)

    @Insert
    suspend fun insertHeartRateList(heartRateList: List<CachedHeartRate>)

}
