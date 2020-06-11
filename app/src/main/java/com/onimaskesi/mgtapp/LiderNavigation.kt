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
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import android.R.color
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.ListenerRegistration
import android.icu.lang.UProperty.DASH
import com.google.android.gms.maps.model.PatternItem
import java.util.*
import kotlin.collections.ArrayList


class LiderNavigation : AppCompatActivity(), OnMapReadyCallback {

    lateinit var docRef : DocumentReference
    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String

    private lateinit var mMap: GoogleMap
    lateinit var locationManager : LocationManager
    lateinit var locationListener: LocationListener

    var PointsArray : MutableList<LatLng> = ArrayList()
    var Takipci_circle_array : MutableList<Circle> = ArrayList()

    var takipci_konumları_dinleme_array : MutableList<ListenerRegistration> = ArrayList()


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

        //firebase rota kayıt için index
        var point_index = 0

        //rota polyline ayarlamaları
        val lineOptions = PolylineOptions().width(20F).color(rgb(38,153,251))
        var lineRoute = mMap.addPolyline(lineOptions)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {

                //mMap.clear()

                takipcileri_goster()

                val locationLatLng = LatLng(location?.latitude!!,location.longitude)
                val locationGeoPoint = GeoPoint(location?.latitude!!,location.longitude)
                //  rota oluşturma(polyline çizme)

                PointsArray.add( locationLatLng )
                lineRoute.points = PointsArray
                lineRoute.isVisible = true

                //  firebasedeki rotalara point kaydı
                point_index += 1
                docRef.get().addOnSuccessListener { documents ->

                    val rota_no = documents.getLong("Rota_sayisi")!!.toInt()

                    docRef.collection("Rotalar").document("Rota${rota_no}").update(point_index.toString(),locationGeoPoint)

                }

                //takipçilerin konum güncellemelerini dinleme ve haritada gösterme
                docRef.collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

                    for(takipci_no in querySnapshot ){

                        //her bir takipçi için konum değişikliği dinleme

                        val dinleyici = docRef.collection("Takipciler").document(takipci_no.id).addSnapshotListener{snapshot, exception ->

                            if (exception != null) {
                                toast(exception.toString())
                            }
                            if (snapshot != null && snapshot.exists()) {

                                //eğer takipçilerin konum değişikliği var ise haritada işaretle
                                takipcileri_goster()

                            }

                        }
                        takipci_konumları_dinleme_array.add(dinleyici)


                    }
                }



                // Set listeners for click events.
                //mMap.setOnPolylineClickListener(this)
                //mMap.setOnPolygonClickListener(this)


                val userLocation = LatLng(location?.latitude!!,location.longitude)

                mMap.addCircle(CircleOptions().fillColor(rgb(38,153,251)).visible(true).center(userLocation).radius(10.0))
                //mMap.addMarker(MarkerOptions().position(userLocation))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,17f))



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

    fun takipcileri_goster(){

        //her bir takipçiyi harita göster
        takipci_circle_temizle()
        docRef.collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

            for(document in querySnapshot ){

                db.collection("Kullanici").document( document.id ).get().addOnSuccessListener { documents ->


                    var location  = documents.get("konum") as GeoPoint
                    takipcileri_haritada_goster(location.latitude,location.longitude)

                }

            }
        }
    }

    fun takipcileri_haritada_goster(lat: Double, long : Double){

        var locationLatLng = LatLng(lat,long)
        val circle = mMap.addCircle(CircleOptions().fillColor(rgb(255,0,0)).visible(true).center(locationLatLng).radius(10.0))
        circle.zIndex = 1F
        Takipci_circle_array.add(circle)
    }

    fun takipci_circle_temizle(){

        for(circles in Takipci_circle_array){
            circles.remove()
        }
        Takipci_circle_array.clear()

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

    fun dinleyicileri_kapat(){

        for(dinleyici in takipci_konumları_dinleme_array){
            dinleyici.remove()
        }

    }

    fun exit_navigation_click(view : View){

        dinleyicileri_kapat()
        docRef.update("Navigasyon_basladi_mi",false)

        //databesedeki takipciler listesini sil
        docRef.collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

            for(takipci_no in querySnapshot ){

                docRef.collection("Takipciler")!!.document(takipci_no.id).delete().addOnFailureListener { exception ->
                    toast(exception.localizedMessage.toString())
                }

            }
        }

        PointsArray.clear()
        Takipci_circle_array.clear()
        takipci_konumları_dinleme_array.clear()

        val intent = Intent(applicationContext, AnaSayfaActivity::class.java)
        intent.putExtra("tel",Telefon)
        startActivity(intent)
        finish()
        //takipcilere rota kayıt etmek istermisiniz popup durumları...

    }

    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

}