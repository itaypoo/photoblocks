package com.itaypoo.photoblocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.HorizontalSwipeCallback
import com.itaypoo.adapters.NotificationListAdapter
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.CustomDialogMaker
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityNotificationsBinding
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.BlockMember
import com.itaypoo.photoblockslib.Notification
import com.itaypoo.photoblockslib.NotificationType

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

        // set onClicks
        binding.notifsBackButton.setOnClickListener {
            finish()
        }
    }

    private fun getNotifList() {
        notificationList = mutableListOf()
        val q = database.collection(Consts.BDPath.userNotifications).whereEqualTo("recipientId", AppUtils.currentUser!!.databaseId!!).get()

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
                Toast.makeText(this, docNotif.type.toString(), Toast.LENGTH_SHORT).show()
            }

            setUpNotifRecycler()
        }

    }

    private fun setUpNotifRecycler(){
        val adapter = NotificationListAdapter(notificationList, this@NotificationsActivity, database, contentResolver)
        binding.notificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecycler.adapter = adapter

        // Empty list indicator
        if(notificationList.size == 0){
            binding.notifsEmptyText.visibility = View.VISIBLE
        }
        else binding.notifsEmptyText.visibility = View.GONE

        // Horizontal swipe functionality
        val swipeDeleteCallback = object : HorizontalSwipeCallback(){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // On item swiped
                val position = viewHolder.adapterPosition
                // If notif is block invite, ask for confirmation
                if(notificationList[position].type != NotificationType.BLOCK_INVITATION){
                    removeNotif(position, adapter)
                }
                else{
                    // Open confirmation dialog
                    val d = CustomDialogMaker.makeYesNoDialog(
                        this@NotificationsActivity, getString(R.string.remove_invitation_confirm), getString(R.string.remove_invitation_desc)
                    )
                    d.dialog.show()
                    d.noButton.setOnClickListener {
                        d.dialog.dismiss()
                        finish()
                    }
                    d.yesButton.setOnClickListener { removeNotif(position, adapter) }
                }
            }

        }
        val touchHelper = ItemTouchHelper(swipeDeleteCallback)
        touchHelper.attachToRecyclerView(binding.notificationsRecycler)

        // onClick lambdas for different notification types
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
            // Join this block
            d.yesButton.setOnClickListener {
                d.dialog.dismiss()

                val loadingD = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.joining_block))
                loadingD.dialog.show()

                // First, delete this notification (to ensure no duplicate block members)
                database.collection(Consts.BDPath.userNotifications).document(itNotification.databaseId!!).delete().addOnSuccessListener {

                    // Add the current user as a member to this block
                    val memberModel = BlockMember(
                        null,
                        Timestamp.now().toDate(),
                        itBlock.databaseId!!,
                        AppUtils.currentUser!!.databaseId!!,
                        false
                    )
                    database.collection(Consts.BDPath.blockMembers).add(memberModel.toHashMap()).addOnSuccessListener {
                        finish()
                    }

                }
            }
            // Decline the invite
            d.noButton.setOnClickListener {
                d.dialog.dismiss()
                val loadingD = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.doing_stuff))
                loadingD.dialog.show()
                database.collection(Consts.BDPath.userNotifications).document(itNotification.databaseId!!).delete().addOnSuccessListener {
                    loadingD.dialog.dismiss()
                    // Remove the invite client side
                    notificationList.remove(itNotification)
                    setUpNotifRecycler()
                }
            }

            // done
        }

    }

    private fun removeNotif(position: Int, adapter: NotificationListAdapter){
        // Remove notif server side
        database.collection(Consts.BDPath.userNotifications).document(notificationList[position].databaseId!!).delete().addOnSuccessListener {
            AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.notification_removed))
        }
        // Remove notif client side
        notificationList.removeAt(position)
        adapter.notifyItemRemoved(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.homeScreenActivity.loadNotificationNumber()
    }
}