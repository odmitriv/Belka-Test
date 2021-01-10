package com.example.belkatest.data.impl

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.belkatest.data.CarDao
import com.example.belkatest.data.model.Car

@Database(entities = [Car::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun carDao(): CarDao
}