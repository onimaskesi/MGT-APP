package com.onimaskesi.mgtapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_parola_olusturma.*
import java.util.*

class ParolaOlusturmaActivity : AppCompatActivity() {

    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parola_olusturma)

        db = FirebaseFirestore.getInstance()
    }

    private fun toast(msg: String){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
    }

    fun tamam_click(view : View){

        val parola = parola.text.toString()
        val parolaTekrar = ParolaTekrar.text.toString()

        if(parola == parolaTekrar){

            val phoneNumber = intent.getStringExtra("tel") as String



            val kullanici = db.collection("Kullanici")

            val data1 = hashMapOf(
                "telefon" to phoneNumber,
                "parola" to parola,
                "AktifMi" to true
            )
            kullanici.document(phoneNumber).set(data1).addOnCompleteListener { task ->

                if(task.isSuccessful){
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

            }.addOnFailureListener { exception ->
                toast(exception.localizedMessage.toString())
            }

        }else{
            toast("Parolalar Eşleşmedi")
        }
    }
}
