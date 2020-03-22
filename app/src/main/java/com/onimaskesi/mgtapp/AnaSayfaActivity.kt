package com.onimaskesi.mgtapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AnaSayfaActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    lateinit var Telefon: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ana_sayfa)

        db = FirebaseFirestore.getInstance()

        Telefon = intent.getStringExtra("tel") as String
    }

    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }


    fun logOut_click(view : View){
        //auth.signOut()

        val docRef = db.collection("Kullanici").document(Telefon)
        docRef.update("AktifMi", false)
            .addOnSuccessListener { toast( "Çıkış Yapıldı") }
            .addOnFailureListener { e -> toast( "Çıkış yapılamadı: ${e}") }

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
