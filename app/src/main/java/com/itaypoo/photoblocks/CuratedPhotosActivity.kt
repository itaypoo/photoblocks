package com.itaypoo.photoblocks

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.CuratedPhotosAdapter
import com.itaypoo.helpers.Consts
import com.itaypoo.photoblocks.databinding.ActivityCuratedPhotosBinding

class CuratedPhotosActivity : AppCompatActivity() {
    /*

     --- Activity for selecting an image from the curated photos library ---

     INPUT EXTRAS: None
     OUTPUT EXTRAS: Chosen photo database id (String)

     the photos library can be found in firestore collection named "curatedPhotos"

     */

    private lateinit var binding: ActivityCuratedPhotosBinding
    private lateinit var storageRef: StorageReference
    private lateinit var database: FirebaseFirestore

    private val resIntent = Intent()

    // Init a list of pairs containing a photoUrl and downloads amount.
    private var photosList: MutableList<List<Any>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCuratedPhotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get firebase references
        storageRef = FirebaseStorage.getInstance().reference
        database = Firebase.firestore

        loadPhotosList()

        // Set cancel button onclick
        binding.curatedCancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun loadPhotosList() {
        // Get photos from database, ordered by download amount
        database.collection("curatedPhotos").orderBy("downloads", Query.Direction.DESCENDING).get().addOnSuccessListener {
            // Construct a list from the data gotten
            for(doc in it){
                val photo = listOf<Any>(
                    doc.id,
                    doc.get("photoUrl") as String,
                    doc.get("downloads") as Long
                 )
                photosList.add(photo)
            }
            
            // Setup RecyclerView
            binding.photosRecyclerView.layoutManager = LinearLayoutManager(this)
            val adapter = CuratedPhotosAdapter(photosList, this)
            binding.photosRecyclerView.adapter = adapter

            // Select a photo when clicking it
            adapter.onItemClickListener = {
                selectPhoto(it)
            }
        }
    }

    private fun selectPhoto(photo: List<Any>) {
        // Pass the photoUrl as result and finish activity.
        resIntent.putExtra(Consts.Extras.CURATED_OUTPUT_DATABASEID, photo[0] as String)
        setResult(RESULT_OK, resIntent)
        finish()
    }
}