package com.example.belkatest.data.impl

import com.example.belkatest.data.BelkaDirectionApi
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import retrofit2.Response

class BelkaDirectionApiImpl(private val belkaAccessToken: String) : BelkaDirectionApi {
    override fun getDirection(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ): Response<DirectionsResponse> {
        val directions = MapboxDirections.builder()
            .origin(Point.fromLngLat(originLng, originLat))
            .destination(Point.fromLngLat(destinationLng, destinationLat))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(belkaAccessToken)
            .build()
        return directions.executeCall()
    }
}