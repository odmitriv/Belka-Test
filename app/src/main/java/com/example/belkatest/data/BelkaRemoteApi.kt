package com.example.belkatest.data

import com.example.belkatest.data.model.Car
import io.reactivex.Observable
import retrofit2.http.*

interface BelkaRemoteApi {
    @GET
    fun getCarList(@Url url: String): Observable<List<Car>>
}