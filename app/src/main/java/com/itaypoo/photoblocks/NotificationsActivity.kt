package com.itaypoo.photoblocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.NotificationListAdapter
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.CustomDialogMaker
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityNotificationsBinding
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.BlockMember
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

            setUpNotifRecycler()
        }

    }

    private fun setUpNotifRecycler(){
        val adapter = NotificationListAdapter(notificationList, this@NotificationsActivity, database, contentResolver)
        binding.notificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecycler.adapter = adapter

        // OnClick for block invite notifications
        adapter.onBlockInviteClickedListener = { itBlock: Block, itNotification: Notification ->
            // Create block view dialog asking the user if they want to accept the invite
            val d = CustomDialogMaker.makeBlockViewDialog(
                this,
                itBlock,
                false,
                false,
                getString(R.string.join_block),
                getString(R.string.decline)
            )
            d.dialog.show()

            d.noButton.setOnClickListener {
                d.dialog.dismiss()
            }
            d.yesButton.setOnClickListener {
                d.dialog.dismiss()

                val d = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.joining_block))
                d.dialog.show()

                // First, delete this notification (to ensure no duplicate block members)
                database.collection("userNotifications").document(itNotification.databaseId!!).delete().addOnSuccessListener {

                    // Add the current user as a member to this block
                    val memberModel = BlockMember(
                        null,
                        itBlock.databaseId!!,
                        AppUtils.currentUser!!.databaseId!!,
                        AppUtils.currentTimeString(),
                        false
                    )
                    database.collection("blockMembers").add(memberModel.toHashMap()).addOnSuccessListener {
                        finish()
                    }

                }
            }

            // done
        }
    }
}