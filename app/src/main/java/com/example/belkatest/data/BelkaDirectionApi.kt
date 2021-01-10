package com.example.belkatest.data

import com.mapbox.api.directions.v5.models.DirectionsResponse
import retrofit2.Response

interface BelkaDirectionApi {
    fun getDirection(originLat: Double, originLng: Double,
                     destinationLat: Double, destinationLng: Double): Response<DirectionsResponse>
}