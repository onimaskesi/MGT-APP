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
import android.R.id.edit
import android.content.SharedPreferences
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.widget.EditText


class MainActivity : AppCompatActivity() {
    private lateinit var db : FirebaseFirestore
    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("mgt-shared",0)

        if(intent.getStringExtra("tel") != null){
            val tel = intent.getStringExtra("tel") as String

            PhoneTxt.post(  Runnable(){
                PhoneTxt.setText(tel)
            })
            toast("Bu telefon numarasına kayıtlı bir hesap mevcuttur.")

        }


        //kullanıcı daha önce giriş yapmış ise tekrar giriş yapmaksızın  ana sayfaya yönlendirilir
        if (sharedPref.getBoolean("giris", false)) {

            val intent = Intent(this, AnaSayfaActivity::class.java)
            val tel = sharedPref.getString("tel",null) as String
            intent.putExtra("tel",tel)
            startActivity(intent)
            finish()

        }

        //////////////

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
    /*
    override fun onStart() {
        super.onStart()
        toast("onStart")
    }

    override fun onResume() {
        super.onResume()
        toast( "onResume")
    }

    override fun onPause() {
        super.onPause()
        toast( "onPause")
    }

    override fun onStop() {
        super.onStop()
        toast( "onStop")
    }

    override fun onRestart() {
        super.onRestart()
        toast( "onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        toast( "onDestroy")
    }

    override fun onSaveInstanceState(outState: Bundle) { //Arka planda olması
        super.onSaveInstanceState(outState)
        toast( "onSaveInstanceState")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        toast( "onRestoreInstanceState")
    }*/


    private fun toast(msg: String){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
    }

    fun kayitOl_click(view: View) {
        val intent = Intent(applicationContext,TelefonKayitActivity ::class.java)
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

                            sharedPref.edit().putBoolean("giris",true).apply()
                            sharedPref.edit().putString("tel",Telefon).apply()

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
            toast("Telefon ve/veya Parola boş bırakılamaz!")
        }

    }
}
