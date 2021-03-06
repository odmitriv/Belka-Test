package com.example.belkatest.data

import androidx.room.*
import com.example.belkatest.data.model.Car
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface CarDao {
    @Query("SELECT * FROM car")
    fun getAll(): Single<List<Car>>

    @Query("SELECT * FROM car WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): Single<List<Car>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertAll(cars: List<Car>): Completable

    @Delete
    fun delete(car: Car): Completable
}