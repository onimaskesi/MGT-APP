package com.onimaskesi.mgtapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_telefon_kayit.*
import kotlin.text.Typography.tm
import androidx.core.content.ContextCompat.getSystemService



class MainActivity : AppCompatActivity() {
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()


        //Telefon numaraları görme izni alma ve kullanıcı telefon numarasını çekme bazı ülkelerde telefon numarası fizikselolarak kaydedilmediği için bu işlem işe yaramaz ör. türkiye
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_PHONE_STATE),1)

        } else {

            val tm = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val telNumber = tm.line1Number
            if (telNumber != null){
                PhoneTxt.setText(telNumber)
            }

        }

    }



    private fun toast(msg: String){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
    }

    fun kayitOl_click(view: View) {
        val intent = Intent(applicationContext,TelefonKayitActivity ::class.java)
        startActivity(intent)
        finish()
    }

    fun Go_click(view: View){
        val intent = Intent(applicationContext,AnaSayfaActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun girisYap_click(view : View){
        val telefon = PhoneTxt.text.toString()
        val parola = ParolaTxt.text.toString()

        if(telefon != "" && parola != ""){

            val docRef = db.collection("Kullanici").document(PhoneTxt.text.toString())
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.get("telefon") != null && document.get("parola") != null) {
                        val Telefon = document.get("telefon").toString()
                        val Parola = document.get("parola").toString()

                        if(Parola == ParolaTxt.text.toString() && Telefon == PhoneTxt.text.toString()){

                            docRef.update("AktifMi", true)
                                .addOnSuccessListener { toast( "Hoşgeldiniz :)") }
                                .addOnFailureListener { e -> toast( "Hata: ${e}") }

                            val intent = Intent(applicationContext, AnaSayfaActivity:: class.java)
                            intent.putExtra("tel",Telefon)
                            startActivity(intent)
                            finish()
                        } else {
                            toast("Parola Hatalı !!")
                        }

                        //toast("DocumentSnapshot data: ${document.data}")
                    } else {
                        toast("Böyle bir üyelik yok lütfen kayıt olunuz!")
                    }
                }
                .addOnFailureListener { exception ->
                    toast("get failed with ${exception.localizedMessage}")
                }

        }else{
            toast("Email ve/veya Parola boş bırakılamaz!")
        }

    }
}
