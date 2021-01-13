package com.example.belkatest.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.belkatest.data.model.Car
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

private const val CAR_LIST_URL =
    "https://raw.githubusercontent.com/Gary111/TrashCan/master/2000_cars.json"
private const val CACHE_LIFE_TIME = "cache_life_time"
private const val CACHE_LIFE_TIME_MS: Long = 60 * 60 * 1000

@ExperimentalCoroutinesApi
class Repository(
    private val belkaRemoteApi: BelkaRemoteApi,
    private val carDao: CarDao,
    private val belkaDirectionApi: BelkaDirectionApi,
    private val sharedPreferences: SharedPreferences
) {
    fun getRemoteCarList(): Flow<List<Car>> {
        return flow {
            val carList = belkaRemoteApi.getCarList(CAR_LIST_URL)
            emit(carList)
        }
            .map {
                carDao.insertAll(it)
                sharedPreferences.edit(commit = true) {
                    putLong(CACHE_LIFE_TIME, System.currentTimeMillis())
                }
                it
            }.flowOn(Dispatchers.IO)
    }

    fun getCachedCarList(): Flow<List<Car>> {
        return flow {
            val currentTime = System.currentTimeMillis()
            val lastTime = sharedPreferences.getLong(CACHE_LIFE_TIME, 0)
            emit(if ((currentTime - lastTime) > CACHE_LIFE_TIME_MS) {
                listOf()
            } else {
                carDao.getAll()
            })
        }.flowOn(Dispatchers.IO)
    }
}