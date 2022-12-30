package com.itaypoo.photoblocks

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.photoblockslib.BlockPost
import com.itaypoo.photoblockslib.DayTimeStamp
import java.util.*
import kotlin.collections.ArrayList


class UploadImagesService : Service() {
    private lateinit var storageRef: StorageReference
    private lateinit var database: FirebaseFirestore

    private lateinit var notifManager: NotificationManager
    private lateinit var notifBuilder: NotificationCompat.Builder


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun startForegroundWithNotif(){
        val notifIntent = Intent(this, HomeScreenActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,
            0, notifIntent, FLAG_IMMUTABLE)

        notifBuilder = NotificationCompat.Builder(this, Consts.Notifs.FOREGROUND_SERVICE_CHANNEL_ID)
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifBuilder.apply {
            setContentTitle("Uploading photos...")
            setSmallIcon(R.drawable.icon_add_photo)
            setContentIntent(pendingIntent)
            build()
        }

        startForeground(1, notifBuilder.notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Called when service starts
        startForegroundWithNotif()
        // Notify the user
        Toast.makeText(applicationContext, R.string.uploading_images_started, Toast.LENGTH_SHORT).show()

        // Get extras
        if(intent?.hasExtra(Consts.Extras.SERVICE_POSTS_TO_UPLOAD_URI_LIST) == false){
            Toast.makeText(this, "Service did not receive any data.", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
        else{
            // Get data to upload
            val uriList = intent!!.getSerializableExtra(Consts.Extras.SERVICE_POSTS_TO_UPLOAD_URI_LIST) as ArrayList<Uri>
            val stringList = intent!!.getSerializableExtra(Consts.Extras.SERVICE_POSTS_TO_UPLOAD_STRING_LIST) as ArrayList<String>
            val uploadToBlockId = intent!!.getStringExtra(Consts.Extras.SERVICE_POSTS_BLOCK_ID)!!

            // Get firebase references
            storageRef = FirebaseStorage.getInstance().reference
            database = Firebase.firestore

            // Set notif progress
            notifBuilder.setProgress(uriList.size*3, 0, false)
            notifManager.notify(1, notifBuilder.build())

            uploadPosts(uriList, stringList, uploadToBlockId)
        }

        return START_STICKY
    }

    private fun uploadPosts(uriList: ArrayList<Uri>, stringList: ArrayList<String>, uploadToBlockId: String){
        val tasksDoneList = arrayListOf<Boolean>()
        var progress = 0

        for(uri in uriList) tasksDoneList.add(false)
        Log.d("SERVICE", "Starting upload ${uriList.size} items")
        
        // Loop through all images that need uploading
        for(i in 0 until uriList.size){
            val imageUri = uriList[i]
            val imageString = stringList[i]
            val uuid = UUID.randomUUID().toString()

            // First, upload the image to storage
            storageRef.child("blockPostImages/post$uuid").putFile(imageUri).addOnFailureListener{

                // Something went wrong, stop this service
                if(it is FirebaseNetworkException) Toast.makeText(this, R.string.uploading_image_failed_network, Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, R.string.uploading_image_failed, Toast.LENGTH_SHORT).show()
                stopSelf()
                
            }.addOnSuccessListener {

                // Uploading image complete
                // Set notif progress
                progress += 1
                notifBuilder.setProgress(uriList.size*3, progress, false)
                notifManager.notify(1, notifBuilder.build())
                // Now get its url...
                storageRef.child("blockPostImages/post$uuid").downloadUrl.addOnFailureListener {

                    // Something went wrong, stop this service
                    if(it is FirebaseNetworkException) Toast.makeText(this, R.string.uploading_image_failed_network, Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, R.string.uploading_image_failed, Toast.LENGTH_SHORT).show()
                    stopSelf()
                }.addOnSuccessListener {

                    // Url gotten successfully
                    val imageUrl = it.toString()
                    // Set notif progress
                    progress += 1
                    notifBuilder.setProgress(uriList.size*3, progress, false)
                    notifManager.notify(1, notifBuilder.build())
                    
                    // Now we can upload the post itself
                    val newPost = BlockPost(
                        null,
                        DayTimeStamp(false),
                        imageUrl,
                        uploadToBlockId,
                        AppUtils.currentUser!!.databaseId!!,
                        imageString
                    )
                    database.collection("blockPosts").add(newPost.toHashMap()).addOnFailureListener {

                        // Something went wrong, stop this service
                        if(it is FirebaseNetworkException) Toast.makeText(this, R.string.uploading_image_failed_network, Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this, R.string.uploading_image_failed, Toast.LENGTH_SHORT).show()
                    }.addOnSuccessListener {

                        Log.d("SERVICE", "Upload post $i complete")
                        // Set notif progress
                        progress += 1
                        notifBuilder.setProgress(uriList.size*3, progress, false)
                        notifManager.notify(1, notifBuilder.build())

                        // this upload is done! check if there are any other that are not done
                        tasksDoneList[i] = true
                        if(!tasksDoneList.contains(false)) allUploadsDone()
                    }
                    
                }
            }
        }
        
        
    }

    private fun allUploadsDone(){
        Toast.makeText(this, R.string.uploading_images_complete, Toast.LENGTH_SHORT).show()
        stopSelf()
    }

}