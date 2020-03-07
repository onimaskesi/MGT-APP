package com.onimaskesi.mgtapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_telefon_kayit.*
import java.util.concurrent.TimeUnit

class TelefonKayitActivity : AppCompatActivity() {

    private lateinit var  auth : FirebaseAuth
    lateinit var mCallbaks  : PhoneAuthProvider.OnVerificationStateChangedCallbacks//tel onay
    var verificationId = ""
    var phoneNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telefon_kayit)

        auth = FirebaseAuth.getInstance()

        Onayla.visibility = View.INVISIBLE
        Onayla.isClickable = false
        onayText.visibility = View.INVISIBLE
        onayText.isClickable = false
        textView6.visibility = View.INVISIBLE

    }

    fun tamam_click(view : View){
        OnayView()
        verify()
    }

    private  fun  verify(){
        verificationCallbacks()
        FirebaseAuth.getInstance().setLanguageCode("tr") //Gönderilecek SMS'in dil ayarı


        auth.useAppLanguage()

        phoneNumber = """+90${this.TelefonTxt.text}"""

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber, // Phone number to verify
            60, // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            this,// Activity (for callback binding)
            mCallbaks ) // OnVerificationStateChangedCallbacks


    }

    fun OnayView(){
        Onayla.visibility = View.VISIBLE
        Onayla.isClickable = true
        onayText.visibility = View.VISIBLE
        onayText.isClickable = true
        textView6.visibility = View.VISIBLE
        Tamambtn.visibility = View.INVISIBLE
        Tamambtn.isClickable = false
        TelefonTxt.isClickable = false
    }

    private  fun verificationCallbacks(){
        mCallbaks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {

                //Sendbtn.visibility=View.VISIBLE
                //singIN(credential )
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                toast(p0.localizedMessage.toString())
                toast("Geçerli bir telefon numarası giriniz!")
            }

            override fun onCodeSent(verfication: String, p1: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verfication, p1)
                verificationId= verfication
                //Sendbtn.visibility=View.INVISIBLE
            }

        }
    }

    fun onayla_click(view: View){
        val verifiNo = onayText.text.toString()

        val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, verifiNo)

        singIN(credential)
    }

    private fun singIN(credential: PhoneAuthCredential){
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                    task: Task<AuthResult> ->
                if(task.isSuccessful){
                    toast(msg = "Sms onay işlemi başarılı :)")
                    val intent = Intent(applicationContext,ParolaOlusturmaActivity::class.java)
                    intent.putExtra("tel",TelefonTxt.text.toString())
                    startActivity(intent)
                    finish()

                }
            }.addOnFailureListener { exception ->
                //toast(exception.localizedMessage.toString())
                toast("Onay işlemi başarısız!!")
            }
    }

    private fun toast (msg: String){

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }




}
