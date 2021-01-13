package com.example.belkatest.data

import com.example.belkatest.data.model.Car
import retrofit2.http.*

interface BelkaRemoteApi {
    @GET
    suspend fun getCarList(@Url url: String): List<Car>
}