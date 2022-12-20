package com.itaypoo.photoblocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityNotificationsBinding
import com.itaypoo.photoblockslib.Notification

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var notificationList: MutableList<Notification>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(AppUtils.currentUser?.databaseId == null){
            Toast.makeText(this, "failed getting user data.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Get firebase references
        storageRef = FirebaseStorage.getInstance().reference
        database = Firebase.firestore

        // Get notifications
        getNotifList()
    }

    private fun getNotifList() {
        notificationList = mutableListOf()
        val q = database.collection("userNotifications").whereEqualTo("recipientId", AppUtils.currentUser!!.databaseId!!).get()

        q.addOnFailureListener {
        // Failed getting data.
            if(it is FirebaseNetworkException){
                Snackbar.make(binding.root, getString(R.string.generic_network_error), Snackbar.LENGTH_LONG).show()
            }
            else{
                Snackbar.make(binding.root, getString(R.string.generic_unknown_error), Snackbar.LENGTH_LONG).show()
            }
        }.addOnSuccessListener {
            // Getting data success
            for(doc in it){
                // Add all notifications to the list
                val docNotif = FirebaseUtils.ObjectFromDoc.Notification(doc)
                notificationList.add(docNotif)
                Toast.makeText(this, docNotif.content, Toast.LENGTH_SHORT).show()
            }
        }

    }
}