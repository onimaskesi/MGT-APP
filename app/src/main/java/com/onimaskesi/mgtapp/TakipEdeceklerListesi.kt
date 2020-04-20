package com.onimaskesi.mgtapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_takip_edecekler_listesi.*
import kotlinx.android.synthetic.main.takipciler.view.*

class TakipEdeceklerListesi : AppCompatActivity() {

    private lateinit var db : FirebaseFirestore
    lateinit var Telefon : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takip_edecekler_listesi)

        Telefon = intent.getStringExtra("tel")
        //Takipci_list.add(intent.getStringExtra("takipci"))

        //takipciyi takipciler listesine ekle

        //takipciler_list.layoutManager = LinearLayoutManager(this)

        //takipciler_list.adapter = Adapter(Takipci_list, this)


    }

    class Adapter(items : List<String>,ctx: Context) : RecyclerView.Adapter<Adapter.ViewHolder>(){

        private var list = items
        private var context = ctx

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: Adapter.ViewHolder, position: Int) {

            holder.name.text = list[position]

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Adapter.ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.takipciler,parent,false))
        }


        class ViewHolder(v: View) : RecyclerView.ViewHolder(v){
            val name = v.takipci_name
        }
    }
}
