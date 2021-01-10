package com.example.belkatest.di

import android.content.Context
import com.example.belkatest.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val application: App) {

    @Provides
    @Singleton
    fun providesApplication(): App = application

    @Provides
    @Singleton
    fun providesApplicationContext(): Context = application
}