package com.onimaskesi.mgtapp

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color.rgb
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint


class LiderNavigation : AppCompatActivity(), OnMapReadyCallback {

    lateinit var docRef : DocumentReference
    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String

    private lateinit var mMap: GoogleMap
    lateinit var locationManager : LocationManager
    lateinit var locationListener: LocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_navigation)

        db = FirebaseFirestore.getInstance()
        Telefon = intent.getStringExtra("tel") as String
        docRef = db.collection("Kullanici").document(Telefon)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        docRef.collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

            for(document in querySnapshot ){

                docRef.collection("Takipciler").document( document.id ).get().addOnSuccessListener { documents ->

                    var location  = documents.get("konum") as GeoPoint
                    var locationLatLng = LatLng(location.latitude,location.longitude)
                    mMap.addCircle(CircleOptions().fillColor(rgb(255,0,0)).visible(true).center(locationLatLng).radius(10.0))

                }

            }
        }


        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {

                //mMap.clear()

                val userLocation = LatLng(location?.latitude!!,location.longitude)

                mMap.addCircle(CircleOptions().fillColor(rgb(38,153,251)).visible(true).center(userLocation).radius(10.0))
                //mMap.addMarker(MarkerOptions().position(userLocation))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))



            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }

            override fun onProviderDisabled(provider: String?) {

            }


        }


        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
            //izin verilmediyse
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 7)


        }else { // izin verildiyse

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 15f, locationListener) // 10 saniye veya 15 metrede bir konum güncellemesi
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if(lastLocation != null){
                val lastLocationLatLng = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap.addCircle(CircleOptions().fillColor(rgb(38,153,251)).visible(true).center(lastLocationLatLng).radius(10.0).clickable(true))
                //mMap.addMarker(MarkerOptions().position(lastLocationLatLng))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLng,18f))
            }
        }


        /*
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        //mMap.addCircle(CircleOptions().fillColor(rgb(38,153,251)).visible(true).center(userLocation).radius(2.0))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,18f))*/
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if(requestCode == 7){
            if(grantResults.size > 0){

                if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,15f,locationListener) // 10 saniye veya 15 metrede bir konum güncellemesi

                }
            }

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
