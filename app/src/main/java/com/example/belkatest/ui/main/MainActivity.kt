package com.example.belkatest.ui.main

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.os.PersistableBundle
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.belkatest.App
import com.example.belkatest.R
import com.example.belkatest.di.BelkaToken
import com.example.belkatest.ui.main.MainViewModel.Companion.ICON_BLACK_CAR_ID
import com.example.belkatest.ui.main.MainViewModel.Companion.ICON_BLUE_CAR_ID
import com.example.belkatest.ui.main.MainViewModel.Companion.ICON_PROPERTY
import com.example.belkatest.ui.main.MainViewModel.Companion.ICON_ROTATE_PROPERTY
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import javax.inject.Inject


private const val MAPBOX_STYLE_BELKA = "mapbox://styles/belkacar/ckdj89h8c0rk61jlgb850lece"
private const val ROUTE_LAYER_ID = "route-layer-id"
private const val ROUTE_SOURCE_ID = "route-source-id"
private const val MARKERS_LAYER_ID = "markers-layer-id"
private const val MARKERS_SOURCE_ID = "markers-source-id"

class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    private var featureCollection: FeatureCollection? = null
    private var route: String? = null
    private var markerViewManager: MarkerViewManager? = null
    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mapView: MapView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var tvName: TextView
    private lateinit var tvPlateNumber: TextView
    private lateinit var tvFuelPercentage: TextView
    private lateinit var ivCar: ImageView
    private lateinit var viewModel: MainViewModel

    @Inject
    lateinit var viewModelFactory: MainViewModel.MainViewModelFactory

    @field:[Inject BelkaToken]
    @BelkaToken
    lateinit var belkaToken: String

    /**
     * Инициализация и отслеживание статуса ViewModel
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as App).appComponent.inject(this)

        Mapbox.getInstance(this, belkaToken)

        setContentView(R.layout.main_activity)

        mapView = findViewById(R.id.mapView)
        tvName = findViewById(R.id.tvName)
        tvPlateNumber = findViewById(R.id.tvPlateNumber)
        tvFuelPercentage = findViewById(R.id.tvFuelPercentage)
        ivCar = findViewById(R.id.ivCar)

        val llBottomSheet: LinearLayout = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(MainViewModel::class.java)

        viewModel.featureCollectionData.observe(this, {
            this.featureCollection = it
            addMarkers()
        })
        viewModel.routeData.observe(this, {
            this.route = it
            showRoute()
        })
        viewModel.carData.observe(this, {
            tvName.text = it?.name
            tvPlateNumber.text = it?.plateNumber
            tvFuelPercentage.text =
                getString(R.string.fuel_percentage_text, it?.fuelPercentage ?: 0)

            Glide.with(this)
                .load(it.carPictureUrl)
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                )
                .into(ivCar)

            bottomSheetBehavior.state =
                if (it.showPanel)
                    BottomSheetBehavior.STATE_EXPANDED
                else
                    BottomSheetBehavior.STATE_HIDDEN
        })

        viewModel.requestMarkerList()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap?.getStyle { style -> enableLocationComponent(style) }
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        markerViewManager = MarkerViewManager(mapView, mapboxMap)
        this.mapboxMap = mapboxMap
        mapboxMap.addOnMapClickListener {
            handleClickMarker(mapboxMap.projection.toScreenLocation(it))
        }
        mapboxMap.setStyle(Style.Builder().fromUri(MAPBOX_STYLE_BELKA)) { style ->
            initLayers(style)
            enableLocationComponent(style)
            addImageToStyle(style, ICON_BLACK_CAR_ID, R.drawable.ic_map_black_car)
            addImageToStyle(style, ICON_BLUE_CAR_ID, R.drawable.ic_map_blue_car)
            addMarkers()
            showRoute()
        }
    }

    /**
     * Функция вычисляет нажатия на маркеры.
     *
     * @param screenPoint указывает на координаты экрана
     */
    private fun handleClickMarker(screenPoint: PointF): Boolean {
        val features = mapboxMap?.queryRenderedFeatures(screenPoint, MARKERS_LAYER_ID)
        return if (features?.isNotEmpty() == true) {
            viewModel.selectCar(locationComponent?.lastKnownLocation, features[0])
            true
        } else {
            viewModel.clearRoute()
            false
        }
    }

    /**
     * Добавляет иконку в коллекцию стиля.
     *
     * @param loadedMapStyle стиль карты
     * @param imageId идентификатор
     * @param icon идентификатор ресурса
     */
    private fun addImageToStyle(loadedMapStyle: Style, imageId: String, icon: Int) {
        loadedMapStyle.addImage(
            imageId,
            BitmapUtils.getBitmapFromDrawable(
                ContextCompat.getDrawable(
                    this,
                    icon
                )
            )!!,
            false
        )
    }

    /**
     * Инициализация слоёв. Используется 2 слоя, первый - для отрисовки маршрута, второй -
     * для отрисовки автомобилей.
     */
    private fun initLayers(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
        loadedMapStyle.addSource(GeoJsonSource(MARKERS_SOURCE_ID))

        loadedMapStyle.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)
                .withProperties(
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_MITER),
                    lineOpacity(.7f),
                    lineWidth(7f),
                    lineColor(Color.parseColor("#3bb2ff"))
                )
        )

        loadedMapStyle.addLayer(
            SymbolLayer(MARKERS_LAYER_ID, MARKERS_SOURCE_ID)
                .withProperties(
                    iconImage(
                        match(
                            get(ICON_PROPERTY), literal(ICON_BLACK_CAR_ID),
                            stop(ICON_BLUE_CAR_ID, ICON_BLUE_CAR_ID),
                            stop(ICON_BLACK_CAR_ID, ICON_BLACK_CAR_ID)
                        )
                    ),
                    iconAllowOverlap(false),
                    iconAnchor(Property.ICON_ANCHOR_CENTER),
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    iconRotate(
                        number(get(ICON_ROTATE_PROPERTY))
                    )
                )
        )
    }

    /**
     * Показ маркеров автомобилей
     */
    private fun addMarkers() {
        featureCollection?.let {
            mapboxMap?.getStyle { style ->
                val source = style.getSourceAs<GeoJsonSource>(MARKERS_SOURCE_ID)
                source?.setGeoJson(it)
            }
        }
    }

    /**
     * Показ маршрута
     */
    private fun showRoute() {
        if (route != null) {
            mapboxMap?.getStyle { style ->
                val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
                source?.setGeoJson(
                    LineString.fromPolyline(
                        route!!,
                        PRECISION_6
                    )
                )
            }
        }
    }

    /**
     * Функция проверяет разрешение на определение координат. В случае, если разрешение дано,
     * рисуется маркер текущего положения устройства, в противном случае запрашивается разрешение
     * на определение местоположения.
     */
    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val locationComponentOptions =
                LocationComponentOptions.builder(this)
                    .pulseEnabled(true)
                    .pulseColor(Color.GREEN)
                    .pulseAlpha(.4f)
                    .pulseInterpolator(BounceInterpolator())
                    .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(locationComponentOptions)
                    .useDefaultLocationEngine(true)
                    .build()
            locationComponent = mapboxMap!!.locationComponent
            locationComponent?.activateLocationComponent(locationComponentActivationOptions)
            locationComponent?.isLocationComponentEnabled = true
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }
}