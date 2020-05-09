package com.onimaskesi.mgtapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_rehber.*
import kotlinx.android.synthetic.main.contact_child.view.*
import kotlinx.android.synthetic.main.contact_child.view.TakipEt_btn as TakipEt_btn1

lateinit var db : FirebaseFirestore
lateinit var Telefon : String
lateinit var sharedPref: SharedPreferences

class RehberActivity : AppCompatActivity() {


    val userList : MutableList<ContactDTO> = ArrayList()
    lateinit var docRef : DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rehber)

        db = FirebaseFirestore.getInstance()
        Telefon = intent.getStringExtra("tel")
        docRef = db.collection("Kullanici").document(Telefon)
        sharedPref = getSharedPreferences("mgt-shared",0)

        makeOfflineTheUser()

        contact_list.layoutManager = LinearLayoutManager(this)

        val contactList : MutableList<ContactDTO> = ArrayList()

        val contacts = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null)

        while (contacts?.moveToNext()!!){

            val name = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            var number = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

            if(number.get(0) == '+'){ //+90 ile başlayanları düzenleme

                number = number.substring(3)

            }else if(number.get(0) == '0'){ //0 ile başlayanları düzenleme
                number = number.substring(1)
            }

            if(number.length == 10){

                val obj = ContactDTO()
                obj.name = name
                obj.number = number

                /*val photo_uri = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                if(photo_uri != null){
                    obj.image = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(photo_uri))
                }*/

                contactList.add(obj)
            }

        }
        contacts.close()

        var userSize = 0

        for(contact in contactList){

            val docRefCompare = db.collection("Kullanici").document(contact.number)
            docRefCompare.get().addOnSuccessListener { document ->

                if (document.get("telefon") != null && document.get("parola") != null){

                    val obj = ContactDTO()
                    obj.name = contact.name
                    obj.number = contact.number
                    //obj.image = contact.image

                    if(sharedPref.getString("user${userSize} name",null) == null ){
                        sharedPref.edit().putString("user${userSize} name",contact.name).apply()
                        sharedPref.edit().putString("user${userSize} tel",contact.number).apply()
                    }

                    userSize++
                    sharedPref.edit().putInt("userSize",userSize).apply()

                    userList.add(obj)
                }
            }
        }

        contact_list.adapter = ContactAdapter(userList,this)

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

    fun GeriGit_click(view: View){

        makeOnlineTheUser()

        val intent = Intent(applicationContext, AnaSayfaActivity::class.java)
        intent.putExtra("tel",Telefon)
        startActivity(intent)
        finish()
    }

    fun yenile_click(view: View){
        contact_list.adapter = ContactAdapter(userList,this)
    }


    private fun toast(msg: String){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show()
    }

    class ContactAdapter(items : List<ContactDTO>,ctx: Context) : RecyclerView.Adapter<ContactAdapter.ViewHolder>(){

        private var list = items
        private var context = ctx

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ContactAdapter.ViewHolder, position: Int) {

            holder.name.text = list[position].name
            holder.number.text = list[position].number
            //holder.button.setImageResource(R.drawable.takipaktif)

            val docRef = db.collection("Kullanici").document(list[position].number)
            docRef.get().addOnSuccessListener { document ->

                if(document.get("AktifMi") == true){

                    holder.profile.setImageResource(R.drawable.green)
                    holder.button.setBackgroundResource(R.drawable.layout_bg_takipaktif)
                    holder.button.isClickable = true

                }else{

                    holder.profile.setImageResource(R.drawable.red)
                    holder.button.setBackgroundResource(R.drawable.layout_bg_takippasif)
                    holder.button.isClickable = false

                }
            }

            holder.button.setOnClickListener {

                //Toast.makeText(this.context,holder.number.text, Toast.LENGTH_LONG).show()

                docRef.get().addOnSuccessListener { documentSnapshot ->

                    if(documentSnapshot.get("IstekVarMi") != true){

                        docRef.update("IstekGonderenTel",Telefon)
                        docRef.update("IstekVarMi",true)

                    }else{
                        Toast.makeText(this.context,"hali hazırda istek var!",Toast.LENGTH_LONG).show()
                    }
                }

            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactAdapter.ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.contact_child,parent,false))
        }


        class ViewHolder(v: View) : RecyclerView.ViewHolder(v){
            val name = v.tv_name!!
            val number = v.tv_number!!
            val profile = v.iv_profile!!
            val button = v.TakipEt_btn1!!


        }
    }
}
