package com.example.fireeeee

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.io.File
import java.net.URI

class MainActivity : AppCompatActivity() {
    var dataProvinsi = ArrayList<daftarProvinsi>()
    var data : MutableList<Map<String, Any>> = ArrayList()

//    lateinit var lvAdapter : ArrayAdapter<daftarProvinsi>
    lateinit var lvAdapter : SimpleAdapter
    lateinit var _etProvinsi : EditText
    lateinit var _etIbuKota : EditText
    lateinit var _btnSimpan : Button
    lateinit var _lvData : ListView
    lateinit var _ivUpload : ImageView
    lateinit var _progressBarUpload : ProgressBar


    private val CLOUDINARY_CLOUD_NAME = "dd4jhfpnf"
    private val UNSIGNED_UPLOAD_PRESET = "preset1"

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val db = Firebase.firestore
        _etProvinsi=findViewById(R.id.etProvinsi)
        _etIbuKota=findViewById(R.id.etIbuKota)
        _btnSimpan=findViewById(R.id.btnSimpan)
        _lvData=findViewById(R.id.lvData)
        _ivUpload = findViewById(R.id.ivUpload)
        _progressBarUpload = findViewById(R.id.progressBarUpload)




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
                    Glide.with(this@MainActivity)
                        .load(data)
                        .placeholder(defaultImage)
                        .error(defaultImage)
                        .into(imgView)
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
//            TambahData(db, _etProvinsi.text.toString(), _etIbuKota.text.toString())
            if (selectedImageUri!= null){
                uploadToCloudinary(db, selectedImageUri!!)
            }else {
                TambahData(db, _etProvinsi.text.toString(), _etIbuKota.text.toString(), "")
            }
        }

        ReadData(db)

        val config = mapOf("cloud_name" to CLOUDINARY_CLOUD_NAME, "upload_preset" to UNSIGNED_UPLOAD_PRESET)
        MediaManager.init(this, config)

        _ivUpload.setOnClickListener {
            showImagePickDialog()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) {uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                _ivUpload.setImageURI(it)
            }
        }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if(it){
            selectedImageUri = cameraImageUri
            _ivUpload.setImageURI(cameraImageUri)
        }
    }
    private fun createImageUri() : Uri?{
        val imageFile = File(
            cacheDir,
            "temp_image_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile
        )
    }

    private fun showImagePickDialog(){
        val options = arrayOf("Pilih dari Gallery", "Ambil Foto")
        AlertDialog.Builder(this)
            .setTitle("Pilih Gambar")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> pickImageFromGallery.launch("image/*")
                    1 -> {
                        createImageUri()?.let { uri ->
                            cameraImageUri = uri
                            takePicture.launch(uri)
                        }
                    }
                }
            }.show()
    }

    private fun uploadToCloudinary(db: FirebaseFirestore, uri: Uri){
        MediaManager.get().upload(uri)
            .unsigned(UNSIGNED_UPLOAD_PRESET)
            .option("folder", "cobaFirebase")
            .callback(object : UploadCallback{
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "Upload Started : $requestId")

                    runOnUiThread {
                        _progressBarUpload.visibility= View.VISIBLE
                        _progressBarUpload.progress = 0
                        _progressBarUpload.max = 100

                    }
                }

                override fun onProgress(
                    requestId: String?,
                    bytes: Long,
                    totalBytes: Long
                ) {
                    val progress = (bytes * 100 / totalBytes).toInt()
                    runOnUiThread {
                        _progressBarUpload.progress = progress
                    }
                }

                override fun onSuccess(
                    requestId: String?,
                    resultData: Map<*, *>?
                ) {
                    var url = resultData?.get("secure_url").toString()
                    Log.d("Cloudinary", "Upload Success : $url")
                    TambahData(db, _etProvinsi.text.toString(), _etIbuKota.text.toString(), url.toString())
                    runOnUiThread {
                        _progressBarUpload.visibility = View.GONE
                    }
                }

                override fun onError(
                    requestId: String?,
                    error: ErrorInfo?
                ) {
                    Log.d("Cloudinary", "Upload Error : ${error.toString()}")
                    runOnUiThread {
                        _progressBarUpload.visibility = View.GONE
                    }
                }

                override fun onReschedule(
                    requestId: String?,
                    error: ErrorInfo?
                ) {
                    Log.d("Cloudinary", "Upload Reschedule : ${error.toString()}")
                }

            }).dispatch()
        Log.d("Cloudinary", "Upload with preset : $UNSIGNED_UPLOAD_PRESET")
    }

    fun TambahData(db: FirebaseFirestore, provinsi : String, ibuKota : String, imageUrl : String ){
        val dataBaru = daftarProvinsi(provinsi, ibuKota, imageUrl)
        db.collection("tbProvinsi")
            .document(_etProvinsi.text.toString())
            .set(dataBaru)
            .addOnSuccessListener {
                dataProvinsi.add(dataBaru)
                lvAdapter.notifyDataSetChanged()
                _etProvinsi.setText("")
                _etIbuKota.setText("")
                _ivUpload.setImageResource(com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_dark)
                selectedImageUri = null
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
                    itemdata.put("Img", item.data.get("imageUrl").toString())
                    Log.d("Firebase", item.data.get("provinsi").toString())
                    data.add(itemdata)

                }
                lvAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.d("Firebase", it.message.toString())
            }
    }
}