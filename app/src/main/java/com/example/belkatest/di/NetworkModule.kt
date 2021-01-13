package com.example.belkatest.di

import android.content.Context
import com.example.belkatest.R
import com.example.belkatest.data.BelkaDirectionApi
import com.example.belkatest.data.BelkaRemoteApi
import com.example.belkatest.data.impl.BelkaDirectionApiImpl
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideBelkaService(): BelkaRemoteApi {
        return Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BelkaRemoteApi::class.java)
    }

    @Singleton
    @Provides
    @BelkaToken
    fun provideBelkaToken(context: Context): String {
        return context.getString(R.string.access_token)
    }

    @Singleton
    @Provides
    fun provideDirectionService(@BelkaToken belkaToken: String): BelkaDirectionApi {
        return BelkaDirectionApiImpl(belkaToken)
    }
}