package com.onimaskesi.mgtapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.util.ArrayUtils.removeAll
import com.google.firebase.firestore.*
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.synthetic.main.activity_takip_edecekler_listesi.*
import kotlinx.android.synthetic.main.takip_istek_pop.view.*
import kotlinx.android.synthetic.main.takipciler.view.*

class TakipEdeceklerListesi : AppCompatActivity() {

    private lateinit var db : FirebaseFirestore
    lateinit var Telefon : String
    lateinit var Konum : GeoPoint
    var Takipci_list : MutableList<ContactDTO> = ArrayList()
    lateinit var docRef : DocumentReference
    val userList : MutableList<ContactDTO> = mutableListOf()
    lateinit var takip_istek_dinleme : ListenerRegistration
    lateinit var navigasyon_baslama_dinleme : ListenerRegistration
    var rota_sayisi = 0
    var takipciMi : Boolean = false
    var takip_edilen = "yok"

    //private lateinit var mMap: GoogleMap
    lateinit var locationManager : LocationManager
    lateinit var locationListener: LocationListener

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takip_edecekler_listesi)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener{

            override fun onLocationChanged(location: Location?) {

                Konum = GeoPoint(location!!.latitude,location!!.longitude)
                docRef.update("konum",Konum)

            }

            override fun onProviderDisabled(provider: String?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 15f, locationListener) // 10 saniye veya 15 metrede bir konum güncellemesi
        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if(lastLocation != null){

            Konum = GeoPoint(lastLocation.latitude,lastLocation.longitude)
            //val lastLocationLatLng = LatLng(lastLocation.latitude,lastLocation.longitude)//LatLng(lastLocation.latitude,lastLocation.longitude)
            //mMap.addCircle(CircleOptions().fillColor(rgb(38,153,251)).visible(true).center(lastLocationLatLng).radius(2.0).clickable(true))
        }

        db = FirebaseFirestore.getInstance()
        Telefon = intent.getStringExtra("tel")
        docRef = db.collection("Kullanici").document(Telefon)

        sharedPref = getSharedPreferences("mgt-shared",0)

        takipciMi = intent.getBooleanExtra("takipciMi",false)

        CreateUserList()

        if(takipciMi){

            baslatBtn.isClickable = false
            baslatBtn.setBackgroundResource(R.drawable.layout_bg_takippasif)

            takip_edilen = intent.getStringExtra("takip_edilen_tel") as String //rehberden tekip isteği attığı son kişiyi takip edilen olarak alır

            //takipci listesini firebaseden çekerek gösteriyor
            db.collection("Kullanici").document(takip_edilen).collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

                for(document in querySnapshot ){

                    listeye_takipci_ekle( numara_rehberde_mi(document.id) )

                }
            }

            //navigasyonun başlayıp baslamadığını dinler
            navigasyon_baslama_dinleme = db.collection("Kullanici").document(takip_edilen).addSnapshotListener { snapshot, e ->

                if (e != null) {
                    toast("dinleme hatası!")
                }

                if (snapshot != null && snapshot.exists()) {

                    if(snapshot.get("Navigasyon_basladi_mi") == true){

                        //navigasyonu başlat (takipçi tarafının intenti)
                        toast("TAKİP BAŞLADI...")

                    }

                } else {
                    toast( "navigasyon baslama durumu null !")
                }
            }

        } else {

            val ilk_takipci_tel = intent.getStringExtra("takipci")

            listeye_takipci_ekle(ilk_takipci_tel)

            takip_istek_dinleme = docRef.addSnapshotListener { snapshot, e ->

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

    }

    fun listeye_takipci_ekle(ilk_takipci_tel : String){

        var takipci = ContactDTO()
        takipci.number = ilk_takipci_tel


        if( numara_rehberde_mi( ilk_takipci_tel ) != "NO" ){

            takipci.name = numara_rehberde_mi( ilk_takipci_tel )

        }else{
            takipci.name = null.toString()
        }

        Takipci_list.add(takipci)

        takipciler_list.layoutManager = LinearLayoutManager(this)

        takipciler_list.adapter = TakipciAdapter(Takipci_list, this)


    }

    fun liste_yenile(){

        Takipci_list.clear()

        if(takipciMi){

            db.collection("Kullanici").document(takip_edilen).collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

                for(document in querySnapshot ){

                    listeye_takipci_ekle( numara_rehberde_mi(document.id) )

                }
            }

        } else {

            docRef.collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

                for(document in querySnapshot ){

                    listeye_takipci_ekle( numara_rehberde_mi(document.id) )

                }
            }

        }
    }

    fun liste_yenile_click(view: View){

        liste_yenile()

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

            listeye_takipci_ekle(istekGonderenTel)

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

        if(istekGonderenTel == Telefon){
            return "Ben"
        }

        for (userInRehber in userList){

            if(userInRehber.number == istekGonderenTel){
                return userInRehber.name
            }
        }
        return istekGonderenTel
    }

    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

    fun iptal_click(view: View){

        if(takipciMi){

            navigasyon_baslama_dinleme.remove()

            db.collection("Kullanici").document(takip_edilen).collection("Takipciler").document(Telefon).delete().addOnFailureListener { exception ->
                toast(exception.localizedMessage.toString())
            }

        }else{

            takip_istek_dinleme.remove()
            docRef.update("Navigasyon_basladi_mi",false)

            for(takipci in Takipci_list){

                docRef.collection("Takipciler")!!.document(takipci.number).delete().addOnFailureListener { exception ->
                    toast(exception.localizedMessage.toString())
                }
            }

        }

        Takipci_list.clear()

        val intent = Intent(applicationContext, AnaSayfaActivity::class.java)
        intent.putExtra("tel",Telefon)
        startActivity(intent)
        finish()
    }

    fun baslat_click(view: View){

        if(!takipciMi){

            liste_yenile()
            docRef.update("Navigasyon_basladi_mi",true)

            docRef.get().addOnSuccessListener { documents ->

                rota_sayisi =  documents.getLong("Rota_sayisi")!!.toInt()
                rota_sayisi++
                toast(rota_sayisi.toString())
                docRef.update("Rota_sayisi", rota_sayisi)

                val Rota_values = hashMapOf(

                    "0" to Konum
                )

                val rotalar = docRef.collection("Rotalar")

                rotalar.document("Rota${rota_sayisi}").set(Rota_values).addOnSuccessListener {

                    //googlemap intente geçiş
                    takip_istek_dinleme.remove()
                    val intent = Intent(applicationContext, LiderNavigation::class.java)
                    intent.putExtra("tel",Telefon)
                    startActivity(intent)
                    finish()

                }.addOnFailureListener { exception ->

                    toast(exception.localizedMessage.toString())

                }

            }

        }

    }

    class TakipciAdapter(items : List<ContactDTO>,ctx: Context) : RecyclerView.Adapter<TakipciAdapter.ViewHolder>(){

        private var list = items
        private var context = ctx

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: TakipciAdapter.ViewHolder, position: Int) {

            if(list[position].name != null.toString()){

                holder.name.text = list[position].name

            }else{
                holder.name.text = list[position].number
            }

            //holder.button.setImageResource(R.drawable.takipaktif)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TakipciAdapter.ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.takipciler,parent,false))
        }

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v){

            val name = v.takipci_name

        }
    }
}
