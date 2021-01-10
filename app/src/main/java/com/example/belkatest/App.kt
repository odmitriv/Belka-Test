package com.example.belkatest

import android.app.Application
import com.example.belkatest.di.AppModule

class App : Application() {
    val appComponent = DaggerAppComponent.builder()
        .appModule(AppModule(this))
        .build()
}