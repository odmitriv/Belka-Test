package com.example.belkatest.di

import android.content.SharedPreferences
import com.example.belkatest.data.BelkaDirectionApi
import com.example.belkatest.data.CarDao
import com.example.belkatest.data.BelkaRemoteApi
import com.example.belkatest.ui.main.MainViewModel
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ViewModelFactoryModule {

    @ActivityScope
    @Provides
    fun provideViewModelFactoryModule(
        belkaRemoteApi: BelkaRemoteApi,
        belkaDirectionApi: BelkaDirectionApi,
        carDao: CarDao,
        sharedPreferences: SharedPreferences
    ):
            MainViewModel.MainViewModelFactory {
        return MainViewModel.MainViewModelFactory(
            belkaRemoteApi,
            belkaDirectionApi,
            carDao,
            sharedPreferences
        )
    }
}