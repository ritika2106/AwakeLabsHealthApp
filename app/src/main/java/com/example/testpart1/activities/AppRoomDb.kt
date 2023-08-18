package com.example.testpart1.activities

import androidx.room.Database
import androidx.room.RoomDatabase //base class for defining db provided by Room


//main access point for db interaction
@Database(entities = [CachedHeartRate::class], version = 1)
abstract class AppRoomDb : RoomDatabase() {

    //returns an instance of a data access object
    abstract fun cachedHeartRateDao(): CachedHeartRateDao

}
