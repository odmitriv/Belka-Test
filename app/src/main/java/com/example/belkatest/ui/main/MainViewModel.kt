package com.example.belkatest.ui.main

import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.belkatest.data.BelkaDirectionApi
import com.example.belkatest.data.BelkaRemoteApi
import com.example.belkatest.data.CarDao
import com.example.belkatest.data.model.Car
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


const val TAG = "MainViewModel"

private const val CAR_LIST_URL =
    "https://raw.githubusercontent.com/Gary111/TrashCan/master/2000_cars.json"
private const val CACHE_LIFE_TIME = "cache_life_time"
private const val CACHE_LIFE_TIME_MS: Long = 60 * 60 * 1000

class MainViewModel(
    private val belkaRemoteApi: BelkaRemoteApi,
    private val belkaDirectionApi: BelkaDirectionApi,
    private val carDao: CarDao,
    private val sharedPreferences: SharedPreferences
) :
    ViewModel() {
    val featureCollectionData = MutableLiveData<FeatureCollection>()
    val routeData = MutableLiveData<String?>()
    val carDataError = MutableLiveData<Throwable>()
    val carData = MutableLiveData<CarData>()
    private var subscriptions: CompositeDisposable = CompositeDisposable()


    /**
     *
     * Функция берет список автомобилей с сервера,
     * трансформирует в объекты для карты и обновляет содержимое кэш и LiveData
     * для показа. В процессе опращивается и возвращается кэш на диске в случае,
     * если он есть и кэш создан не более часа
     * назад.
     *
     */
/*
    fun requestMarkerList() {
        subscriptions.clear()
        subscriptions.add(
            belkaRemoteApi.getCarList(CAR_LIST_URL).toObservable()
                .flatMap {
                    carDao.insertAll(it)
                        .andThen(Observable.just(it))
                }
                .map {
                    sharedPreferences.edit(commit = true) {
                        putLong(CACHE_LIFE_TIME, System.currentTimeMillis())
                    }
                    it
                }
                .startWith(getCachedCarList().map {
                    it
                })
                // Исключает двойную перерисовку карты,
                // если сетевые данные придут раньше, чем через 2 секунды
                .debounce(2, TimeUnit.SECONDS)
                .flatMap {
                    transformCarListToMapFeatureList(it)
                }
                .map {
                    FeatureCollection.fromFeatures(it)
                }
                .subscribeOn(Schedulers.io())
                .subscribe({ list ->
                    featureCollectionData.postValue(list)
                }, { error ->
                    Log.d(TAG, error.stackTraceToString())
                    carDataError.postValue(error)
                })
        )
    }
*/

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun requestMarkerList() {
        viewModelScope.launch {
            flow {
                emit(getCachedCarList())
                emit(getRemoteCarList())
            }
                .debounce(2000)
                .map {
                    flowTransformCarListToMapFeatureList(it)
                }
                .map {
                    FeatureCollection.fromFeatures(it)
                }
                .flowOn(Dispatchers.IO)
                .collect {
                    Log.d(TAG, "Result")
                    featureCollectionData.postValue(it)
                }
        }
    }

    private suspend fun getRemoteCarList(): List<Car> {
        val carList = belkaRemoteApi.getCarList(CAR_LIST_URL)
        carDao.insertAll(carList)
        sharedPreferences.edit(commit = true) {
            putLong(CACHE_LIFE_TIME, System.currentTimeMillis())
        }
        Log.d(TAG, "get Car List from remote DB")
        return carList
    }

    /**
     * Трансформация списка моделей автомобиля к списку для карты. В процессе добавляются свойства,
     * которые показываются при выборе автомобиля и прорисовки маршрута.
     *
     * @param carList список автомобилей с сервера.
     */
    private fun transformCarListToMapFeatureList(carList: List<Car>): Observable<List<Feature>> {
        return Observable.fromIterable(carList)
            .map {
                val feature = Feature.fromGeometry(
                    Point.fromLngLat(
                        it.longitude,
                        it.latitude
                    )
                )
                feature.addStringProperty(
                    ICON_PROPERTY,
                    if (it.color == "blue")
                        ICON_BLUE_CAR_ID
                    else
                        ICON_BLACK_CAR_ID
                )
                feature.addNumberProperty(
                    ICON_ROTATE_PROPERTY,
                    it.angle
                )
                feature.addNumberProperty("id", it.id)
                feature.addStringProperty("name", it.name)
                feature.addStringProperty("plateNumber", it.plateNumber)
                feature.addNumberProperty("fuelPercentage", it.fuelPercentage)
                feature.addNumberProperty("latitude", it.latitude)
                feature.addNumberProperty("longitude", it.longitude)
                feature.addStringProperty("carPictureUrl", "https://picsum.photos/300/200")
                return@map feature
            }
            .toList()
            .toObservable()
    }

    private suspend fun flowTransformCarListToMapFeatureList(carList: List<Car>): List<Feature> {
        return carList.asFlow()
            .map {
                val feature = Feature.fromGeometry(
                    Point.fromLngLat(
                        it.longitude,
                        it.latitude
                    )
                )
                feature.addStringProperty(
                    ICON_PROPERTY,
                    if (it.color == "blue")
                        ICON_BLUE_CAR_ID
                    else
                        ICON_BLACK_CAR_ID
                )
                feature.addNumberProperty(
                    ICON_ROTATE_PROPERTY,
                    it.angle
                )
                feature.addNumberProperty("id", it.id)
                feature.addStringProperty("name", it.name)
                feature.addStringProperty("plateNumber", it.plateNumber)
                feature.addNumberProperty("fuelPercentage", it.fuelPercentage)
                feature.addNumberProperty("latitude", it.latitude)
                feature.addNumberProperty("longitude", it.longitude)
                feature.addStringProperty("carPictureUrl", "https://picsum.photos/300/200")
                return@map feature
            }
            .toList()
    }

    /**
     * Возвращает список из кэш или пустой, если кэш не создан или
     * хранится более одного часа.
     */
    private fun getCachedCarList(): List<Car> {
        val currentTime = System.currentTimeMillis()
        val lastTime = sharedPreferences.getLong(CACHE_LIFE_TIME, 0)
        return if ((currentTime - lastTime) > CACHE_LIFE_TIME_MS) {
            listOf()
        } else {
            val carList = carDao.getAll()
            Log.d(TAG, "get Car List from local DB")
            carList
        }
    }

    /**
     * Функция строит маршрут от origin координаты к объекту на карте feature,
     * обновляет LiveData модели автомобиля.
     *
     * @param originLocation стартовая координата
     * @param feature объект на карте
     *
     */
    fun selectCar(originLocation: Location?, feature: Feature) {
        if (originLocation != null) {
            val name = feature.getStringProperty("name")
            val plateNumber = feature.getStringProperty("plateNumber")
            val fuelPercentage = feature.getNumberProperty("fuelPercentage")
                .toInt()
            val latitude = feature.getNumberProperty("latitude")
                .toDouble()
            val longitude = feature.getNumberProperty("longitude")
                .toDouble()
            val carPictureUrl = feature.getStringProperty("carPictureUrl")

            val carData = CarData(true, name, plateNumber, fuelPercentage, carPictureUrl)
            this.carData.value = carData

            drawRoute(originLocation, LatLng(latitude, longitude))
        }
    }


    /**
     * Функция строит маршрут от origin координаты к объекту на карте feature.
     *
     * @param originLocation стартовая координата
     * @param destinationLocation целевая координата
     *
     */
    private fun drawRoute(originLocation: Location, destinationLocation: LatLng) {
        subscriptions.add(
            Observable.just(Pair(originLocation, destinationLocation))
                .map {
                    belkaDirectionApi.getDirection(
                        it.first.latitude,
                        it.first.longitude,
                        it.second.latitude,
                        it.second.longitude
                    )
                }
                .map {
                    it.body()
                }
                .subscribeOn(Schedulers.io())
                .subscribe({ body ->
                    body?.routes()?.get(0)?.geometry().let {
                        routeData.postValue(it)
                    }
                }, { error ->
                    Log.d(TAG, error.stackTraceToString())
                    carDataError.postValue(error)
                })
        )
    }

    /**
     * Очищает LiveData модели автомобиля
     */
    fun clearRoute() {
        carData.value = CarData()
        routeData.value = ""
    }

    /**
     * Финальная очистка подписок при закрытии модели.
     *
     */
    override fun onCleared() {
        subscriptions.clear()
    }

    /**
     * Модель данных автомобиля, применяется во View.
     *
     */
    data class CarData(
        val showPanel: Boolean = false,
        val name: String? = null,
        val plateNumber: String? = null,
        val fuelPercentage: Int = 0,
        val carPictureUrl: String? = null
    )

    /**
     * Фабрика для создания модели. Инжектится во View.
     */
    @Suppress("UNCHECKED_CAST")
    open class MainViewModelFactory(
        private val belkaRemoteApi: BelkaRemoteApi,
        private val belkaDirectionApi: BelkaDirectionApi,
        private val carDao: CarDao,
        private val sharedPreferences: SharedPreferences
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MainViewModel(belkaRemoteApi, belkaDirectionApi, carDao, sharedPreferences) as T
    }

    companion object {
        const val ICON_ROTATE_PROPERTY = "icon_rotate_property"
        const val ICON_PROPERTY = "icon_property"
        const val ICON_BLACK_CAR_ID = "icon_black_car"
        const val ICON_BLUE_CAR_ID = "icon_blue_car"
    }
}