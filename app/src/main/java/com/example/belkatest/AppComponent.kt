package com.example.belkatest

import com.example.belkatest.di.*
import com.example.belkatest.ui.main.MainActivity
import dagger.Component
import javax.inject.Singleton

@ActivityScope
@Singleton
@Component(modules = [AppModule::class, NetworkModule::class, DatabaseModule::class, ViewModelFactoryModule::class])
interface AppComponent {
    fun inject(obj: MainActivity)
}