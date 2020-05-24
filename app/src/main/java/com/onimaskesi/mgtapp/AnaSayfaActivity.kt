package com.onimaskesi.mgtapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import kotlinx.android.synthetic.main.takip_istek_pop.*
import kotlinx.android.synthetic.main.takip_istek_pop.view.*
import kotlinx.android.synthetic.main.takip_istek_pop.view.KabulBtn



class AnaSayfaActivity : AppCompatActivity() , PermissionsListener , OnMapReadyCallback {

    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String
    lateinit var Konum : GeoPoint
    lateinit var sharedPref: SharedPreferences
    lateinit var docRef : DocumentReference
    lateinit var registration : ListenerRegistration
    val userList : MutableList<ContactDTO> = ArrayList()

    private var permissionsManager:PermissionsManager? = null
    private var locationComponent: LocationComponent? = null
    private var mapboxMap: MapboxMap? = null
    private var mapView: MapView? = null

    private var Lat : Double? = null
    private var Long : Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token_mapbox))
        setContentView(R.layout.activity_ana_sayfa)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("mgt-shared",0)

        Telefon = intent.getStringExtra("tel") as String

        docRef = db.collection("Kullanici").document(Telefon)

        makeOnlineTheUser()

        CreateUserList()

        mapView = findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

        db.collection("Kullanici").document(Telefon).update("AtilanIstekKabulEdildiMi",2)// 0 => red, 1 => kabul, 2 => beklemede

        registration = docRef.addSnapshotListener { snapshot, e ->

            if (e != null) {
                toast("Takip isteği dinleme hatası!")
            }

            if (snapshot != null && snapshot.exists()) {

                if(snapshot.get("IstekVarMi") == true){

                    TakipIstegiPop()

                }

            } else {
                toast( "İstek durumu null !")
            }
        }

    }


    fun CreateUserList(){
        val userSize = sharedPref.getInt("userSize",0)


        for (i in 0..userSize){
            val obj = ContactDTO()
            obj.name = sharedPref.getString("user${i} name","onimaskesi") as String
            obj.number = sharedPref.getString("user${i} tel","onimaskesi") as String

            userList.add(obj)
        }


    }

    fun numara_rehberde_mi(istekGonderenTel : String): String {

        for (userInRehber in userList){

            if(userInRehber.number == istekGonderenTel){
                return userInRehber.name
            }
        }
        return istekGonderenTel
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
            Lat = lastKnownLocation!!.latitude
            Long = lastKnownLocation!!.longitude

            //toast(Lat.toString() + "  " + Long.toString())
            Konum = GeoPoint(Lat!!, Long!!)
            docRef.update("konum", Konum)


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
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted:Boolean) {
        if (granted){
            enableLocationComponent(mapboxMap!!.style!!)
        }
        else{
            Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onMapReady(mapboxMap:MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day)
        ) { style ->
            enableLocationComponent(style)

        }
    }


    fun TakipIstegiPop(){

        var istekGonderenTel = ""
        val takipIstekView = LayoutInflater.from(this.applicationContext).inflate(R.layout.takip_istek_pop,null)
        var ilk_takipci = ""

        docRef.get().addOnSuccessListener {document ->

            if(document.get("IstekGonderenTel") != null){

                istekGonderenTel = document.get("IstekGonderenTel") as String

                ilk_takipci = numara_rehberde_mi(istekGonderenTel)
                takipIstekView.TakipIstekTv.setText("${ilk_takipci} \n Sizi Takip Etmek İstiyor")

            }

        }



        val alertBuilder = AlertDialog.Builder(this).setView(takipIstekView)

        val mAlertDialog = alertBuilder.show()

        takipIstekView.RedBtn.setOnClickListener {
            mAlertDialog.dismiss()
            db.collection("Kullanici").document(istekGonderenTel).update("AtilanIstekKabulEdildiMi",0)

            docRef.update("IstekVarMi",false)
        }

        takipIstekView.KabulBtn.setOnClickListener {

            docRef.update("IstekVarMi",false)
            db.collection("Kullanici").document(istekGonderenTel).update("AtilanIstekKabulEdildiMi",1)
            registration.remove()


            db.collection("Kullanici").document(istekGonderenTel).get().addOnSuccessListener { documents ->

                var takipci_location = documents.get("konum") as GeoPoint

                val takipci_values = hashMapOf(

                    "Telefon" to istekGonderenTel,
                    "konum" to takipci_location
                )

                val takipciler = docRef.collection("Takipciler")

                takipciler.document(istekGonderenTel).set(takipci_values).addOnSuccessListener {

                }.addOnFailureListener { exception ->

                    toast(exception.localizedMessage.toString())

                }

            }

            mAlertDialog.dismiss()

            val intent = Intent(applicationContext, TakipEdeceklerListesi::class.java )
            intent.putExtra("tel",Telefon)
            intent.putExtra("takipci",istekGonderenTel)
            intent.putExtra("takipciMi",false)
            startActivity(intent)
            finish()

        }

    }

    fun makeOfflineTheUser(){

        registration.remove()

        docRef.get().addOnSuccessListener { documentSnapshot ->
            if(documentSnapshot.get("AktifMi") != false){
                docRef.update("AktifMi",false)
            }
        }

    }

    fun makeOnlineTheUser(){

        docRef.get().addOnSuccessListener { documentSnapshot ->
            if(documentSnapshot.get("AktifMi") != true){

                docRef.update("AktifMi",true)

            }
        }

    }

    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

    fun logOut_click(view : View){
        //auth.signOut()

       makeOfflineTheUser()

        sharedPref.edit().putBoolean("giris",false).apply()

        var intent = Intent(applicationContext,MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun Kayitli_rotalar_click(view: View){

    }

    fun Rehber_click(view: View){

        makeOfflineTheUser()

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                1
            )
        }else {

            val intent = Intent(applicationContext, RehberActivity::class.java)
            intent.putExtra("tel",Telefon)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        makeOnlineTheUser()
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
        makeOfflineTheUser()
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
}
