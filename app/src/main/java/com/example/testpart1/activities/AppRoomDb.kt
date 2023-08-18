package com.example.testpart1.activities

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CachedHeartRate::class], version = 1)
abstract class AppRoomDb : RoomDatabase() {

    abstract fun cachedHeartRateDao(): CachedHeartRateDao

//    companion object {
//        @Volatile
//        private var instance: AppRoomDb? = null
//
//        fun getDatabase(context: Context): AppRoomDb {
//            return instance ?: synchronized(this) {
//                val newInstance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppRoomDb::class.java, "exercise-db"
//                ).build()
//                instance = newInstance
//                newInstance
//            }
//        }
//    }
}
