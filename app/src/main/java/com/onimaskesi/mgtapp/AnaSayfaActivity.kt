package com.onimaskesi.mgtapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AnaSayfaActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ana_sayfa)

        db = FirebaseFirestore.getInstance()
    }

    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

    fun Rehber_click(view: View){

    }

    fun logOut_click(view : View){
        //auth.signOut()
        val Telefon = intent.getStringExtra("tel") as String

        val docRef = db.collection("Kullanici").document(Telefon)
        docRef.update("AktifMi", false)
            .addOnSuccessListener { toast( "Çıkış Yapıldı") }
            .addOnFailureListener { e -> toast( "Çıkış yapılamadı: ${e}") }

        var intent = Intent(applicationContext,MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
