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
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.takip_istek_pop.view.*

class AnaSayfaActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String
    lateinit var sharedPref: SharedPreferences
    lateinit var docRef : DocumentReference
    lateinit var registration : ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ana_sayfa)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("mgt-shared",0)

        Telefon = intent.getStringExtra("tel") as String

        docRef = db.collection("Kullanici").document(Telefon)

        makeOnlineTheUser()

        registration = docRef.addSnapshotListener { snapshot, e ->

            if (e != null) {
                toast("Listen failed.")
            }

            if (snapshot != null && snapshot.exists()) {

                if(snapshot.get("IstekVarMi") == true){

                    TakipIstegiPop()

                }

            } else {
                toast( "Current data: null")
            }
        }

    }


    fun TakipIstegiPop(){

        val takipIstekView = LayoutInflater.from(this.applicationContext).inflate(R.layout.takip_istek_pop,null)

        val mBuilder = AlertDialog.Builder(this).setView(takipIstekView)

        val mAlertDialog = mBuilder.show()

        takipIstekView.RedBtn.setOnClickListener {
            mAlertDialog.dismiss()
            docRef.update("IstekVarMi",false)
        }
        takipIstekView.KabulBtn.setOnClickListener {
            //kabul butonuna basılınca yapılacaklar: listeye almaya başlar(activity ile) ve start butonuna basması halinde takip başlar [t.eden edilen kim kimdir vs.. database ile iletişim halinde ilerler]
            mAlertDialog.dismiss()
            toast("İyi yolculuklar xD")
            docRef.update("IstekVarMi",false)
        }

    }

    override fun onResume() {
        super.onResume()
        makeOnlineTheUser()
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

    override fun onSaveInstanceState(outState: Bundle) { //Arka planda olması
        super.onSaveInstanceState(outState)
        makeOfflineTheUser()
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
}
