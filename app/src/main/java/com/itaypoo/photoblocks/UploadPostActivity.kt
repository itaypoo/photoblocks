package com.itaypoo.photoblocks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.PostUploadAdapter
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.CustomDialogMaker
import com.itaypoo.photoblocks.databinding.ActivityUploadPostBinding
import com.itaypoo.photoblockslib.BlockPost
import com.itaypoo.photoblockslib.DayTimeStamp
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min


class UploadPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadPostBinding

    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var uploadToBlockId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get firebase connections
        database = Firebase.firestore
        storageRef = FirebaseStorage.getInstance().reference

        createNotificationChannel()

        // Get passed block id
        if(intent.hasExtra(Consts.Extras.UPLOADPOST_INPUT_BLOCKID)){
            uploadToBlockId = intent.getStringExtra(Consts.Extras.UPLOADPOST_INPUT_BLOCKID)!!
        }
        else{
            // No block passed. Cant perform activity.
            finish()
        }

        // Open image selection screen, Allow multiple images
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image_from)), Consts.RequestCode.GALLERY_PICKER_MULTIPLE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)

        val imageUriList = mutableListOf<Uri>()

        var itemCount = 0
        if(dataIntent?.clipData != null){
            // Multiple images picked
            itemCount = dataIntent.clipData!!.itemCount
            for(i in 0 until min(itemCount, 30)){
                // Get item at position i
                imageUriList.add(dataIntent.clipData!!.getItemAt(i).uri)
            }
        }
        else if(dataIntent != null && dataIntent.data != null){
            // Single image picked
            imageUriList.add(dataIntent.data!!)
        }

        // Set up title text
        if(imageUriList.size == 1){
            binding.uploadPostTitleText.text = getString(R.string.one_image_selected)
        }
        else{
            binding.uploadPostTitleText.text = buildString {
                append(imageUriList.size.toString())
                append(" ")
                append(getString(R.string.images_selected))
            }
            if(itemCount > 30){
                Snackbar.make(binding.postRecycler, getString(R.string.too_many_images_selected), Snackbar.LENGTH_SHORT).show()
            }
        }

        // Set up post recycler
        val adapter = PostUploadAdapter(imageUriList, this)
        binding.postRecycler.layoutManager = LinearLayoutManager(this)
        binding.postRecycler.adapter = adapter

        // Upload posts on upload button click
        binding.postUploadButton.setOnClickListener {
            uploadPosts(adapter.getImagesAndDescriptions())
        }
    }

    private fun uploadPosts(pairList: MutableList<UriStringPair>){
        if(pairList.size == 1){
            // Only one post, no need to open a service
            // Upload this post
            val uuid = UUID.randomUUID().toString()
            val pair = pairList[0]
            // First upload the image to storage
            val uploadTask = storageRef.child("blockPostImages/post$uuid").putFile(pair.uri)

            // Show loading dialog
            val d = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.uploading_image))
            d.dialog.show()

            uploadTask.addOnFailureListener{
                // Image upload failed
                if(it is FirebaseNetworkException) Snackbar.make(binding.root, getString(R.string.generic_network_error), Snackbar.LENGTH_LONG).show()
                else Snackbar.make(binding.root, getString(R.string.generic_unknown_error), Snackbar.LENGTH_LONG).show()
                d.dialog.dismiss()
            }
            uploadTask.addOnSuccessListener {
                // Image uploaded. Now get its URL...
                storageRef.child("blockPostImages/post$uuid").downloadUrl.addOnSuccessListener {

                    val url = it.toString()
                    // Now we can finally upload this post.
                    val post = BlockPost(
                        null,
                        DayTimeStamp(false),
                        url,
                        uploadToBlockId,
                        AppUtils.currentUser!!.databaseId!!,
                        pair.string
                    )
                    // Upload the post
                    database.collection("blockPosts").add(post.toHashMap()).addOnFailureListener {
                        // Upload failed
                        if(it is FirebaseNetworkException){ Snackbar.make(binding.root, getString(R.string.generic_network_error), Snackbar.LENGTH_LONG).show() }
                        else Snackbar.make(binding.root, getString(R.string.generic_unknown_error), Snackbar.LENGTH_LONG).show()
                        d.dialog.dismiss()
                    }.addOnSuccessListener {
                        // Upload success
                        finish()
                    }

                }
            }

            //done
        }

        else{
            // We have multiple posts to upload, lets do that in a service
            val serviceIntent = Intent(this, UploadImagesService::class.java)

            val uriList: ArrayList<Uri> = arrayListOf()
            val stringList: ArrayList<String> = arrayListOf()
            for(pair in pairList){
                uriList.add(pair.uri)
                stringList.add(pair.string)
            }

            val bundle = Bundle()
            bundle.putSerializable(Consts.Extras.SERVICE_POSTS_TO_UPLOAD_URI_LIST, uriList)
            bundle.putSerializable(Consts.Extras.SERVICE_POSTS_TO_UPLOAD_STRING_LIST, stringList)
            serviceIntent.putExtras(bundle)

            serviceIntent.putExtra(Consts.Extras.SERVICE_POSTS_BLOCK_ID, uploadToBlockId)

            startForegroundService(serviceIntent)
            finish()
        }

    }

    private fun createNotificationChannel() {
        val notifChannel: NotificationChannel = NotificationChannel(
            Consts.Notifs.FOREGROUND_SERVICE_CHANNEL_ID, "Image uploading progress", NotificationManager.IMPORTANCE_LOW)

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(notifChannel)
    }

    public class UriStringPair(val uri: Uri, val string: String): java.io.Serializable

}