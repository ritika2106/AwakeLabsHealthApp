package com.example.testpart1.activities

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy


@Dao
interface CachedHeartRateDao {

    //add a single heart rate to the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(heartRate: CachedHeartRate)

    //add a list of heart rate objects to the db
    @Insert
    suspend fun insertHeartRateList(heartRateList: List<CachedHeartRate>)

}
