package com.onimaskesi.mgtapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

// haritayı başlatmak için gerekli sınıflar
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback

// konum(location) bileşenini eklemek için gerekli sınıflar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode

// işaretçi(marker) eklemek için gerekli sınıflar
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage

// rota hesaplamak için sınıflar
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log

// navigasyon arayüzünü(navigation UI) başlatmak için gerekli sınıflar
import android.view.View
import android.widget.Button
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.mapbox.services.android.navigation.ui.v5.*
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState
import com.mapbox.turf.TurfMeasurement
import com.onimaskesi.mgtapp.R


class TakipciNavigasyon:AppCompatActivity(), OnMapReadyCallback, PermissionsListener, OnNavigationReadyCallback {

    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String
    lateinit var takip_edilen: String
    lateinit var Konum : GeoPoint
    lateinit var sharedPref: SharedPreferences
    lateinit var docRef : DocumentReference
    lateinit var docRefLider : DocumentReference

    // konum katmanı(location layer) eklemek için değişkenler
    private var mapView:MapView? = null
    private var mapboxMap:MapboxMap? = null

    // konum katmanı(location layer) eklemek için değişkenler
    private var permissionsManager:PermissionsManager? = null
    private var locationComponent:LocationComponent? = null

    // rota hesaplama ve çizim değişkenleri
    private var currentRoute:DirectionsRoute? = null
    private var navigationMapRoute:NavigationMapRoute? = null

    var point_index : Int = -1

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token_mapbox))
        setContentView(R.layout.activity_takipci_navigasyon)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("mgt-shared",0)

        Telefon = intent.getStringExtra("tel") as String
        takip_edilen = intent.getStringExtra("takip_edilen_tel")

        docRef = db.collection("Kullanici").document(Telefon)
        docRefLider = db.collection("Kullanici").document(takip_edilen)

        mapView = findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap:MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(getString(R.string.navigation_guidance_day)
        ) { style ->
            enableLocationComponent(style)

            //addDestinationIconSymbolLayer(style)

            //navigasyon varış ve başlangıç noktalarının ayarlanması
            //başlangıç noktası olarak kendi konumunu, varış noktası olarak ise databaseden takip edeceği kullanıcının konumunu alır

            docRefLider.get().addOnSuccessListener { documents ->

                val destinationGeoPoint = documents.get("konum") as GeoPoint

                val destination = Point.fromLngLat(destinationGeoPoint.longitude, destinationGeoPoint.latitude)
                val origin = Point.fromLngLat(locationComponent!!.lastKnownLocation!!.longitude,
                    locationComponent!!.lastKnownLocation!!.latitude
                )

                //rota oluşturma ve navigasyonu başlatma işlemleri

                NavigationRoute.builder(this)
                    .accessToken(Mapbox.getAccessToken()!!)
                    .origin(origin)
                    .destination(destination)
                    .build()
                    .getRoute(object:Callback<DirectionsResponse> {
                        override fun onResponse(call:Call<DirectionsResponse>, response:Response<DirectionsResponse>) {
                            // You can get the generic HTTP info about the response
                            Log.d(TAG, "Response code: " + response.code())
                            if (response.body() == null) {
                                Log.e(TAG, "No routes found, make sure you set the right user and access token.")
                                return
                            } else if (response.body()!!.routes().size < 1) {
                                Log.e(TAG, "No routes found")
                                return
                            }

                            currentRoute = response.body()!!.routes()[0]

                            // Draw the route on the map
                            if (navigationMapRoute != null){
                                navigationMapRoute!!.removeRoute()
                            } else {
                                navigationMapRoute = mapView?.let {
                                    mapboxMap?.let { it1 ->
                                        NavigationMapRoute(null,
                                            it, it1, R.style.NavigationMapRoute)
                                    }
                                }
                            }
                            navigationMapRoute!!.addRoute(currentRoute)
                            navigeEt()
                        }

                        override fun onFailure(call:Call<DirectionsResponse>, throwable:Throwable) {
                            Log.e(TAG, "Error: " + throwable.message)
                        }
                    })

            }

        }

    }

    fun navigeEt(){

        val simulateRoute = false
        val options = NavigationLauncherOptions.builder()
            .directionsRoute(currentRoute)
            .shouldSimulateRoute(simulateRoute)
            .build()
        // Call this method with Context from within an Activity

        /*
        val Viewoptions = NavigationViewOptions.builder()
        Viewoptions.progressChangeListener { location, routeProgress ->
            if(routeProgress.currentState()?.equals(RouteProgressState.ROUTE_ARRIVED)!!){
                toast("VARDINIZ!!!")
            }
        }
        Viewoptions.directionsRoute(currentRoute).shouldSimulateRoute(simulateRoute)

        navigationView.startNavigation(Viewoptions.build())
         */

        NavigationLauncher.startNavigation(this, options)

    }

    fun haritaya_geri_click(view:View){

        val intent = Intent(applicationContext, TakipciHarita::class.java)
        intent.putExtra("tel",Telefon)
        intent.putExtra("takip_edilen_tel",takip_edilen)
        startActivity(intent)
        finish()

    }

    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

    private fun enableLocationComponent(loadedMapStyle:Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)){
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap!!.locationComponent
            locationComponent!!.activateLocationComponent(this, loadedMapStyle)
            locationComponent!!.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent!!.cameraMode = CameraMode.TRACKING
            var lastKnownLocation = locationComponent!!.lastKnownLocation
            var lklLat = lastKnownLocation!!.latitude
            var lklLong = lastKnownLocation!!.longitude
            //toast(lklLat.toString() + "  " + lklLong.toString())
            var konum = GeoPoint(lklLat,lklLong)
            docRef.update("konum",konum)
            docRefLider.collection("Takipciler").document(Telefon).update("konum",konum)
        }
        else
        {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<String>, grantResults:IntArray) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain:List<String>) {
        toast( R.string.user_location_permission_explanation.toString() )
    }

    override fun onPermissionResult(granted:Boolean) {
        if (granted){
            enableLocationComponent(mapboxMap!!.style!!)
        }
        else{
            toast( R.string.user_location_permission_explanation.toString() )
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onSaveInstanceState(outState:Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    companion object {
        private val TAG = "DirectionsActivity"
    }

    override fun onNavigationReady(isRunning: Boolean) {

    }

}
