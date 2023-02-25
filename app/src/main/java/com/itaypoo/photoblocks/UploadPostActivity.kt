package com.itaypoo.photoblocks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
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
import java.util.*
import kotlin.collections.ArrayList


class UploadPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadPostBinding

    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var uploadToBlockId: String

    private val uriList = mutableListOf<Uri>()

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

        openImagePicker()
        binding.postAddImagesButton.setOnClickListener {
            openImagePicker()
        }

        binding.postBackButton.setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker(){
        // Open image selection screen, Allow multiple images
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image_from)), Consts.RequestCode.GALLERY_PICKER_MULTIPLE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)

        var pickedTooMany = false
        var pickedTheSame = false
        var itemCount = 0
        if(dataIntent?.clipData != null){
            // Multiple images picked
            itemCount = dataIntent.clipData!!.itemCount
            for(i in 0 until itemCount){
                if(uriList.size < 30){
                    if(!uriList.contains(dataIntent.clipData!!.getItemAt(i).uri)){
                        // Add item at position i
                        uriList.add(dataIntent.clipData!!.getItemAt(i).uri)
                    }
                    else pickedTheSame = true
                }
                else pickedTooMany = true
            }
        }
        else if(dataIntent != null && dataIntent.data != null){
            // Single image picked
            if(uriList.size < 30){
                if(!uriList.contains(dataIntent.data!!)){
                    uriList.add(dataIntent.data!!)
                }
                else pickedTheSame = true
            }
            else pickedTooMany = true
        }

        // Set up title text
        if(uriList.size == 1){
            binding.uploadPostTitleText.text = getString(R.string.one_image_selected)
        }
        else{
            binding.uploadPostTitleText.text = buildString {
                append(uriList.size.toString())
                append(" ")
                append(getString(R.string.images_selected))
            }
            if(pickedTooMany){
                AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.too_many_images_selected))
            }
        }
        if(pickedTheSame){
            AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.same_image_picked))
        }

        setUpRecycler()
    }

    private fun setUpRecycler(){
        // Set up post recycler
        val adapter = PostUploadAdapter(uriList, this)
        binding.postRecycler.layoutManager = LinearLayoutManager(this)
        binding.postRecycler.adapter = adapter

        // Upload posts on upload button click
        binding.postUploadButton.setOnClickListener {
            uploadPosts(adapter.getImageTextList())
        }

        adapter.onRemoveButtonClicked = {
            // Remove image from upload list
            uriList.remove(it)

            binding.uploadPostTitleText.text = buildString {
                append(uriList.size.toString())
                append(" ")
                append(getString(R.string.images_selected))
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun uploadPosts(pairList: MutableList<Pair<Uri, String>>){
        if(pairList.size == 1){
            // Only one post, no need to open a service
            // Upload this post
            val uuid = UUID.randomUUID().toString()
            val pair = pairList[0]
            // First upload the image to storage
            val uploadTask = storageRef.child("blockPostImages/post$uuid").putFile(pair.first)

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
                        Timestamp.now().toDate(),
                        url,
                        uploadToBlockId,
                        AppUtils.currentUser!!.databaseId!!,
                        pair.second
                    )
                    // Upload the post
                    database.collection(Consts.DBPath.blockPosts).add(post.toHashMap()).addOnFailureListener {
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
                uriList.add(pair.first)
                stringList.add(pair.second)
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

}