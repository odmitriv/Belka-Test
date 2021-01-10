package com.example.belkatest.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.belkatest.data.CarDao
import com.example.belkatest.data.impl.Database
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(context: Context): Database {
        return Room.databaseBuilder(
            context,
            Database::class.java, "belka-database"
        ).build()
    }

    @Singleton
    @Provides
    fun provideCarDao(db: Database): CarDao {
        return db.carDao()
    }

    @Provides
    @Singleton
    fun providesSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("belka_pref", Context.MODE_PRIVATE)
    }

}