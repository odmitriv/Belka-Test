package com.example.belkatest.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "car")
data class Car(
    @PrimaryKey
    val id: Int,
    // Номер автомобиля
    @SerializedName("plate_number")
    @ColumnInfo(name = "plate_number")
    val plateNumber: String?,
    // Название автомобиля
    @ColumnInfo(name = "name")
    val name: String?,
    @ColumnInfo(name = "color")
    val color: String?,
    @ColumnInfo(name = "angle")
    val angle: Int,
    @SerializedName("fuel_percentage")
    @ColumnInfo(name = "fuel_percentage")
    val fuelPercentage: Int,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude")
    val longitude: Double
)