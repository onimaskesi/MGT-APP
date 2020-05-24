package com.onimaskesi.mgtapp

import android.content.Context
import android.content.Intent
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
import kotlinx.android.synthetic.main.activity_takip_edecekler_listesi.*
import kotlinx.android.synthetic.main.takip_istek_pop.view.*
import kotlinx.android.synthetic.main.takipciler.view.*

class TakipEdeceklerListesi : AppCompatActivity() {

    private lateinit var db : FirebaseFirestore
    lateinit var Telefon : String
    var Takipci_list : MutableList<ContactDTO> = ArrayList()
    lateinit var docRef : DocumentReference
    val userList : MutableList<ContactDTO> = mutableListOf()
    lateinit var registration : ListenerRegistration
    var takipciMi : Boolean = false
    var takip_edilen = "yok"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takip_edecekler_listesi)

        db = FirebaseFirestore.getInstance()
        Telefon = intent.getStringExtra("tel")
        docRef = db.collection("Kullanici").document(Telefon)

        sharedPref = getSharedPreferences("mgt-shared",0)

        takipciMi = intent.getBooleanExtra("takipciMi",false)

        CreateUserList()

        if(takipciMi){

            baslatBtn.isClickable = false
            baslatBtn.setBackgroundResource(R.drawable.layout_bg_takippasif)

            takip_edilen = intent.getStringExtra("takip_edilen_tel") as String
            db.collection("Kullanici").document(takip_edilen).collection("Takipciler").get().addOnSuccessListener { querySnapshot ->

                for(document in querySnapshot ){

                    listeye_takipci_ekle( numara_rehberde_mi(document.id) )

                }
            }

        } else {

            val ilk_takipci_tel = intent.getStringExtra("takipci")

            listeye_takipci_ekle(ilk_takipci_tel)

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

            db.collection("Kullanici").document(takip_edilen).collection("Takipciler").document(Telefon).delete().addOnFailureListener { exception ->
                toast(exception.localizedMessage.toString())
            }

        }else{

            registration.remove()

            for(takipci in Takipci_list){

                docRef.collection("Takipciler").document(takipci.number).delete().addOnFailureListener { exception ->
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
            //toast("Takip ediliyorsunuz!")


        }

        //registration.remove()

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
