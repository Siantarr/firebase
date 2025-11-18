package com.example.fireeeee

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {
    var dataProvinsi = ArrayList<daftarProvinsi>()
    var data : MutableList<Map<String, Any>> = ArrayList()

//    lateinit var lvAdapter : ArrayAdapter<daftarProvinsi>
    lateinit var lvAdapter : SimpleAdapter
    lateinit var _etProvinsi : EditText
    lateinit var _etIbuKota : EditText
    lateinit var _btnSimpan : Button
    lateinit var _lvData : ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val db = Firebase.firestore
        _etProvinsi=findViewById(R.id.etProvinsi)
        _etIbuKota=findViewById(R.id.etIbuKota)
        _btnSimpan=findViewById(R.id.btnSimpan)
        _lvData=findViewById(R.id.lvData)

//        lvAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_2, dataProvinsi)
        lvAdapter = SimpleAdapter(
            this,
            data,
            R.layout.list_item_with_image,
            arrayOf("Img", "Pro", "Ibu"),
            intArrayOf(R.id.imageLogo, R.id.text1, R.id.text2)
        )

        lvAdapter.setViewBinder { view, data, _ ->
            if (view.id == R.id.imageLogo) {
                val imgView = view as ImageView
                val defaultImage = com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_dark
                if (data is String && data.isNotEmpty()) {
                }else{
                    imgView.setImageResource(defaultImage)
                }
                return@setViewBinder true
            }
            false
        }



        _lvData.setOnItemClickListener { adapterView, view, i, l ->
            _etProvinsi.setText(data[i].get("Pro").toString())
            _etIbuKota.setText(data[i].get("Ibu").toString())
        }

        _lvData.setOnItemLongClickListener { adapterView, view, i, l ->
            val namaProvinsi : String = data[i]["Pro"].toString()
            db.collection("tbProvinsi")
                .document(namaProvinsi)
                .delete()
                .addOnSuccessListener {
                    Log.d("Firebase", "Data Berhasil Dihapus")
                    ReadData(db)
                }
                .addOnFailureListener {
                    Log.d("Firebase", it.message.toString())
                }
            true
        }
        _lvData.adapter = lvAdapter

        _btnSimpan.setOnClickListener {
            TambahData(db, _etProvinsi.text.toString(), _etIbuKota.text.toString())
        }

        ReadData(db)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun TambahData(db: FirebaseFirestore, provinsi : String, ibuKota : String){
        val dataBaru = daftarProvinsi(provinsi, ibuKota)
        db.collection("tbProvinsi")
            .document(_etProvinsi.text.toString())
            .set(dataBaru)
            .addOnSuccessListener {
                dataProvinsi.add(dataBaru)
                lvAdapter.notifyDataSetChanged()
                _etProvinsi.setText("")
                _etIbuKota.setText("")
                ReadData(db)
                Log.d("Firebase", dataBaru.provinsi + " Berhasil Ditambahkan")
            }
            .addOnFailureListener {
                Log.d("Firebase", it.message.toString())
            }
    }

    fun ReadData(db: FirebaseFirestore){
        db.collection("tbProvinsi")
            .get()
            .addOnSuccessListener {
                result ->
                data.clear()
                for (item in result) {

//                    val itemData = daftarProvinsi(
//                        item.data.get("provinsi").toString(),
//                        item.data.get("ibuKota").toString()
//                    )
//                    dataProvinsi.add(itemData)

                    val itemdata : MutableMap<String, Any> = HashMap(2)
                    itemdata.put("Pro", item.data.get("provinsi").toString())
                    itemdata.put("Ibu", item.data.get("ibuKota").toString())
                    data.add(itemdata)

                }
                lvAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.d("Firebase", it.message.toString())
            }
    }
}