package com.onimaskesi.mgtapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import com.google.firebase.firestore.ListenerRegistration

class TakipciHarita : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    lateinit var docRef : DocumentReference
    lateinit var docRefLider : DocumentReference
    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String
    lateinit var takip_edilen: String
    lateinit var lider_konum_dinleme : ListenerRegistration
    lateinit var liderLocation : GeoPoint

    lateinit var locationManager : LocationManager
    lateinit var locationListener: LocationListener

    var Rota_index : Int = 0

    lateinit var kullaniciCircle : Circle

    var PointsArray : MutableList<LatLng> = ArrayList()
    var Takipci_circle_array : MutableList<Circle> = ArrayList()

    var takipci_konumları_dinleme_array : MutableList<ListenerRegistration> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takipci_harita)

        db = FirebaseFirestore.getInstance()
        Telefon = intent.getStringExtra("tel") as String
        takip_edilen = intent.getStringExtra("takip_edilen_tel")
        docRef = db.collection("Kullanici").document(Telefon)
        docRefLider = db.collection("Kullanici").document(takip_edilen)

        docRefLider.get().addOnSuccessListener { documents ->
            Rota_index = documents.getLong("Rota_sayisi")!!.toInt()

        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //rota polyline ayarlamaları
        val lineOptions = PolylineOptions().width(20F).color(Color.rgb(255, 0, 0)).visible(true)
        var lineRoute = mMap.addPolyline(lineOptions)

        //liderin konumunu göster (kırmızı)
        docRefLider.get().addOnSuccessListener { documents ->

            liderLocation = documents.get("konum") as GeoPoint
            val liderLatLng = LatLng(liderLocation.latitude, liderLocation.longitude)
            mMap.addCircle(CircleOptions().fillColor(Color.rgb(255, 0, 0)).visible(true).center(liderLatLng).radius(10.0).clickable(true))

            PointsArray.add( liderLatLng )
            lineRoute.points = PointsArray
            lineRoute.isVisible = true

        }

        //diğer kullanıcıları göster (sarı)
        takipcileri_goster()


        lider_konum_dinleme = docRefLider.addSnapshotListener{snapshot, e ->

            if (e != null) {
                toast(e.toString())
            }
            if (snapshot != null && snapshot.exists()) {

                //eğer liderin konum değişikliği var ise haritada işaretle

                if(liderLocation != snapshot.get("konum")){

                    liderLocation = snapshot.get("konum") as GeoPoint
                    val LiderLatLng = LatLng(liderLocation.latitude, liderLocation.longitude)
                    mMap.addCircle(CircleOptions().fillColor(Color.rgb(255, 0, 0)).visible(true).center(LiderLatLng).radius(10.0).clickable(true))

                    PointsArray.add( LiderLatLng )
                    lineRoute.points = PointsArray
                    lineRoute.isVisible = true

                }

            }

        }

        locationListener = object : LocationListener{

            override fun onLocationChanged(location: Location?) {

                toast(Rota_index.toString())

                val locationLatLng = LatLng(location!!.latitude,location!!.longitude)

                kullaniciCircle.remove()
                kullaniciCircle = mMap.addCircle(CircleOptions().fillColor(Color.rgb(38, 153, 251)).visible(true).center(locationLatLng).radius(10.0).clickable(true))
                kullaniciCircle.zIndex = 1F
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng,17f))

                takipcileri_goster()

                lineRoute.points = PointsArray
                lineRoute.isVisible = true

                /*
                docRefLider.collection("Rotalar").document("Rota${Rota_index}").get().addOnSuccessListener { documents ->
                    var i = 0
                    while(documents.get(i.toString()) != liderLocation){

                        var point = documents.get(i.toString()) as GeoPoint
                        var pointLatLng = LatLng(point.latitude, point.longitude)
                        PointsArray.add( pointLatLng )

                    }
                    lineRoute.points = PointsArray
                    lineRoute.isVisible = true
                }*/

            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }

            override fun onProviderDisabled(provider: String?) {

            }

        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 15f, locationListener) // 10 saniye veya 15 metrede bir konum güncellemesi
        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if(lastLocation != null){

            //kullanıcıyı haritada göster (mavi)
            val lastLocationLatLng = LatLng(lastLocation.latitude,lastLocation.longitude)
            kullaniciCircle = mMap.addCircle(CircleOptions().fillColor(Color.rgb(38, 153, 251)).visible(true).center(lastLocationLatLng).radius(10.0).clickable(true))
            kullaniciCircle.zIndex = 1F
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLng,17f))

            //lineRoute.points = PointsArray
            //lineRoute.isVisible = true
        }

    }

    fun takipcileri_goster(){

        //kullanıcı dışındaki takipçileri harita göster
        takipci_circle_temizle()
        docRefLider.collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

            for(document in querySnapshot ){

                if(document.id != Telefon){

                    db.collection("Kullanici").document( document.id ).get().addOnSuccessListener { documents ->

                        var location  = documents.get("konum") as GeoPoint
                        takipcileri_haritada_goster(location.latitude,location.longitude)

                    }

                }


            }
        }
    }

    fun takipcileri_haritada_goster(lat: Double, long : Double){

        var locationLatLng = LatLng(lat,long)
        val circle = mMap.addCircle(CircleOptions().fillColor(Color.rgb(255, 255, 0)).visible(true).center(locationLatLng).radius(10.0))
        circle.zIndex = 0.8F
        Takipci_circle_array.add(circle)
    }

    fun takipci_circle_temizle(){

        if(Takipci_circle_array != null){

            for(circles in Takipci_circle_array){
                circles.remove()
            }
            Takipci_circle_array.clear()
        }

    }

    fun exit_takipci_navigation_click(view : View) {

        lider_konum_dinleme.remove()
        docRef.update("AtilanIstekKabulEdildiMi", 2)

        //databesedeki takipciler listesinden çıkma
        docRefLider.collection("Takipciler")!!.document(Telefon).delete()
            .addOnFailureListener { exception ->
                toast(exception.localizedMessage.toString())
            }

        PointsArray.clear()
        Takipci_circle_array.clear()
        takipci_konumları_dinleme_array.clear()

        val intent = Intent(applicationContext, AnaSayfaActivity::class.java)
        intent.putExtra("tel",Telefon)
        startActivity(intent)
        finish()
    }

    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

}
