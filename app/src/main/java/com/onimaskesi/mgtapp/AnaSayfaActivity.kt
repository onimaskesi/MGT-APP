package com.onimaskesi.mgtapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class AnaSayfaActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String
    lateinit var sharedPref: SharedPreferences
    lateinit var docRef : DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ana_sayfa)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("mgt-shared",0)

        Telefon = intent.getStringExtra("tel") as String

        docRef = db.collection("Kullanici").document(Telefon)

        makeOnlineTheUser()

    }

    override fun onResume() {
        super.onResume()
        toast(Telefon)
        makeOnlineTheUser()
    }

    fun makeOfflineTheUser(){

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
