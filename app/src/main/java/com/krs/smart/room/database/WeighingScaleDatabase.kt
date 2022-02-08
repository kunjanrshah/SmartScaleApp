package com.krs.smart.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.krs.smart.room.dao.WeighingScaleDao
import com.krs.smart.room.model.WeighingScale

@Database(entities = [WeighingScale::class], version = 1, exportSchema= false)
abstract class WeighingScaleDatabase : RoomDatabase() {
    abstract fun scaleDao(): WeighingScaleDao
    companion object {
        @Volatile
        private var INSTANCE: WeighingScaleDatabase? = null
        fun getInstance(context: Context): WeighingScaleDatabase {
            return INSTANCE ?: synchronized(WeighingScaleDatabase::class) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeighingScaleDatabase::class.java, "scales_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}