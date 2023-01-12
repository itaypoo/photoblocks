package com.itaypoo.photoblocks

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.BlockListAdapter
import com.itaypoo.helpers.*
import com.itaypoo.photoblocks.databinding.ActivityHomeScreenBinding
import com.itaypoo.photoblockslib.*
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit


class HomeScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeScreenBinding
    private lateinit var menuDialog: Dialog

    private lateinit var blockList: MutableList<Block>

    private lateinit var storageRef: StorageReference
    private lateinit var database: FirebaseFirestore

    private var notifAmount: Int = 0

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        AppUtils.homeScreenActivity = this
        // Check if a user is saved (should always be true)
        // From now on we can always access AppUtils.currentUser!!.databaseId!!
        if(AppUtils.currentUser == null){
            Toast.makeText(this, "Error getting user data.", Toast.LENGTH_SHORT).show()
            finish()
        }
        else if( AppUtils.currentUser!!.databaseId == null){
            Toast.makeText(this, "Error getting user data.", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupTransitions()
        super.onCreate(savedInstanceState)
        binding = ActivityHomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get firebase references
        storageRef = FirebaseStorage.getInstance().reference
        database = Firebase.firestore

        // Load user pfp with Glide
        Glide.with(this).load(AppUtils.currentUser?.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(binding.profilePicture)

        val creationDate = AppUtils.DateString(AppUtils.currentUser!!.creationTime)
        val nowDate = AppUtils.DateString(Timestamp.now().toDate())
        // Check if block day is today
        if(creationDate.dayOfMonth == nowDate.dayOfMonth && creationDate.monthName == nowDate.monthName && creationDate.year != nowDate.year){
            // Today is block day!
            binding.blockDayCard.visibility = View.VISIBLE

            val age = nowDate.year - creationDate.year
            binding.blockDayText.text = getString(R.string.joined_today) + ", " + age + " " + getString(R.string.years_ago)

            AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.happy_block_day))
            showKonfetti()
        }
        else{
            // Check if user joined today
            if(creationDate.dayOfMonth == nowDate.dayOfMonth && creationDate.monthName == nowDate.monthName){
                binding.blockDayCard.visibility = View.VISIBLE
                binding.blockDayText.text = getString(R.string.joined_today)
            }
            else{
                // Block day is not today
                binding.blockDayCard.visibility = View.GONE
            }
        }

        // Show welcome or welcome back message
        val type = intent.getIntExtra(Consts.Extras.SIGNIN_TYPE, Consts.LoginType.NO_LOGIN)
        if(type != Consts.LoginType.NO_LOGIN){
            if(type == Consts.LoginType.NEW_USER){
                // New user login
                val text = getString(R.string.welcome_message) + AppUtils.currentUser!!.name
                Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT).show()
                showKonfetti()

                // Check if this user has any pending invites
                database.collection(Consts.BDPath.pendingBlockInvitations).whereEqualTo("phoneNumber", AppUtils.currentUser!!.phoneNumber).get().addOnSuccessListener {
                    val pendingInviteList = mutableListOf<PendingBlockInvitation>()
                    for(doc in it){
                        val pInvite = FirebaseUtils.ObjectFromDoc.PendingBlockInvitation(doc)
                        pendingInviteList.add(pInvite)
                    }
                    if(pendingInviteList.size > 0){
                        // Convert the pending invites to real invites
                        loadPendingInvitations(pendingInviteList)
                    }
                }
            }
            else if(type == Consts.LoginType.EXISTING_USER){
                // Existing user login
                val text = getString(R.string.welcome_back_message) + AppUtils.currentUser!!.name
                Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT).show()
            }

        }

        // Load block list
        loadBlocksJoined()

        // Open menu on menu button click
        setupMenuDialog()
        binding.menuButton.setOnClickListener {
            menuDialog.show()
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, CreateBlockActivity::class.java)
            startActivity(intent)
        }
        // Open notification list on pfp click
        val notifListener = View.OnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.profilePicture.setOnClickListener(notifListener)
        binding.notificationDot.setOnClickListener(notifListener)
        binding.notificationDotOutline.setOnClickListener(notifListener)

        // Load notification amount
        loadNotificationNumber()

        // Search bar functionality
        binding.searchBarEditText.addTextChangedListener {

            val searchText: String = binding.searchBarEditText.text.toString().lowercase()
            // filter out all blocks that do not contain the text in their title

            val filetredList = mutableListOf<Block>()

            for(block in blockList){
                if(block.title.lowercase().contains(searchText)){
                    filetredList.add(block)
                }
            }

            // set the button to a search button or a delete button
            if(searchText.isBlank()){
                binding.searchBarImageButton.setImageResource(R.drawable.icon_search)
            }
            else{
                binding.searchBarImageButton.setImageResource(R.drawable.icon_close)
            }

            // refresh the recyclerView
            setUpBlockRecycler(filetredList)
        }

        // search bar delete button
        binding.searchBarImageButton.setOnClickListener {
            if(binding.searchBarEditText.text.toString().isNotBlank()){
                binding.searchBarImageButton.setImageResource(R.drawable.icon_search)
                binding.searchBarEditText.text = null
            }
        }

    }

    private fun setupTransitions(){
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            // set an exit transition
            exitTransition = Fade()
        }
    }

    private fun showKonfetti(){
        val p = Party(
            angle = 90,
            size = listOf(Size.LARGE, Size.MEDIUM),
            speed = 10f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 165,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(0.5, 0.0),
            emitter = Emitter(duration = 1, TimeUnit.SECONDS).max(50)
        )
        binding.cakeDayKonfetti.start(p)
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun setupMenuDialog() {
        menuDialog = Dialog(this, R.style.CustomDialog)
        menuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        menuDialog.setContentView(R.layout.dialog_sidebar_menu)

        // Set dialog window width, height, background and position
        menuDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        menuDialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        menuDialog.window?.setGravity(Gravity.END)

        // Get button views
        val buttonContacts = menuDialog.findViewById<Button>(R.id.nbs_contactsButton)
        val buttonJoinWithCode = menuDialog.findViewById<Button>(R.id.nbs_joinWithCodeButton)
        val buttonOptions = menuDialog.findViewById<Button>(R.id.nbs_optionsButton)

        // Button on click listeners
        buttonContacts.setOnClickListener {
            menuDialog.dismiss()
            val contactIntent = Intent(this, ChooseContactActivity::class.java)
            contactIntent.putExtra(Consts.Extras.CHOOSECONTECT_INPUT_CHOOSE_TYPE, Consts.ChooseType.CHOOSE_ANY_USER)
            startActivityForResult(contactIntent, Consts.RequestCode.CHOOSE_CONTACT_ACTIVITY)
        }
        buttonJoinWithCode.setOnClickListener {
            menuDialog.dismiss()
        }
        buttonOptions.setOnClickListener {
            menuDialog.dismiss()
            val intent = Intent(this@HomeScreenActivity, UserSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

     fun loadBlocksJoined() {
        // Get all blocks that the current user is a member of
        database.collection(Consts.BDPath.blockMembers).whereEqualTo("memberId", AppUtils.currentUser!!.databaseId).get()
        .addOnFailureListener {

            // Loading members list failed
            if(it is FirebaseNetworkException)
                Snackbar.make(this, binding.root, getString(R.string.falied_loading_blocks_network), Snackbar.LENGTH_LONG).show()
            else Snackbar.make(this, binding.root, getString(R.string.falied_loading_blocks), Snackbar.LENGTH_LONG).show()

        }.addOnSuccessListener {

            // Loading members list complete
            // Generate a list of all the block ID's
            val blockIdList: MutableList<String> = mutableListOf()
            for(doc in it){
                blockIdList.add(doc.get("blockId") as String)
            }

            // Now we have all the block the user is a member of. Lets load them..
            loadBlocksList(blockIdList)

        }
    }

    private fun loadBlocksList(blockIdList: MutableList<String>){
        blockList = mutableListOf()

        // Load all blocks that were found to contain the user as a member
        database.collection(Consts.BDPath.blocks).get().addOnFailureListener {

            // Loading blocks list failed
            if(it is FirebaseNetworkException)
                Snackbar.make(this, binding.root, getString(R.string.falied_loading_blocks_network), Snackbar.LENGTH_LONG).show()
            else Snackbar.make(this, binding.root, getString(R.string.falied_loading_blocks), Snackbar.LENGTH_LONG).show()
        }.addOnSuccessListener {

            // Loaded all blocks
            // Loop through all blocks loaded
            for(doc in it){

                // If this block is in blockIdList, add it to finalBlockList.
                if(blockIdList.contains(doc.id)){
                    val newblock = FirebaseUtils.ObjectFromDoc.Block(doc)
                    blockList.add(newblock)
                }

            }

            // Sort list by date
            blockList = sortByDateBlock(blockList)

            // All relevant blocks are loaded. Now lets fill the RecyclerView with them -
            setUpBlockRecycler(blockList)

        }
    }

    private fun setUpBlockRecycler(blockList: MutableList<Block>){
        val adapter = BlockListAdapter(blockList, this)
        binding.blockRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.blockRecyclerView.adapter = adapter

        // Empty list indicator
        if(blockList.size == 0){
            binding.blockListEmptyText.visibility = View.VISIBLE
        }
        else binding.blockListEmptyText.visibility = View.GONE

        adapter.onItemClickListener = {
            // On block clicked
            val viewBlockIntent = Intent(this, ViewBlockActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(Consts.Extras.PASSED_BLOCK, it)
            viewBlockIntent.putExtras(bundle)
            startActivity(viewBlockIntent)
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    fun loadNotificationNumber() {
        if(notifAmount < 1){
            binding.notificationDot.visibility = View.INVISIBLE
            binding.notificationDotOutline.visibility = View.INVISIBLE
        }

        val q =database.collection(Consts.BDPath.userNotifications).whereEqualTo("recipientId", AppUtils.currentUser?.databaseId).count()
        q.get(AggregateSource.SERVER).addOnSuccessListener {
            notifAmount = it.count.toInt()
            if(notifAmount > 0) {
                binding.notificationDot.visibility = View.VISIBLE
                binding.notificationDotOutline.visibility = View.VISIBLE
                binding.notificationAmountText.text = it.count.toString()
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun loadPendingInvitations(pendingInviteList: MutableList<PendingBlockInvitation>) {
        val tasksDoneList = mutableListOf<Boolean>()
        for(p in pendingInviteList){
            tasksDoneList.add(false)
        }

        // Loop through all pending invites
        for(i in 0 until pendingInviteList.size){
            // First, delete this pending invite
            val pInvite = pendingInviteList[i]
            database.collection(Consts.BDPath.pendingBlockInvitations).document(pInvite.databaseId!!).delete().addOnSuccessListener {
                // Now, upload a real invite
                val inviteNotif = Notification(
                    null,
                    Timestamp.now().toDate(),
                    AppUtils.currentUser!!.databaseId!!, // the recipient for this notif is the current user
                    pInvite.inviterId, // the sender of this notif is the sender of the pending invite
                    NotificationType.BLOCK_INVITATION,
                    pInvite.blockId
                )
                database.collection(Consts.BDPath.userNotifications).add(inviteNotif.toHashMap()).addOnSuccessListener {
                    // This pending invite is complete!
                    tasksDoneList[i] = true

                    if(!tasksDoneList.contains(false)){
                        // Reload notification amount display
                        loadNotificationNumber()
                        // All invites are complete! alert the user of this
                        val d = CustomDialogMaker.makeYesNoDialog(
                            this,
                            getString(R.string.pending_invites_converted_title),
                            getString(R.string.pending_invites_converted_message),
                            true,
                            false,
                            getString(R.string.okay)
                        )
                        d.yesButton.setOnClickListener { d.dialog.dismiss() }
                        d.dialog.show()
                    }
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    fun sortByDateBlock(entityList: MutableList<Block>): MutableList<Block>{

        val list = entityList

        // Bubble sort
        for(i in 0 until list.size-1){

            // last i elements are already in place
            for(h in 0 until list.size-i-1){
                // if list[h+1] < list[h]
                if(list[h+1].creationTime.before(list[h].creationTime)){
                    // swap list[h+1] and list[h]
                    val swapped = list[h+1]
                    list[h+1] = list[h]
                    list[h] = swapped
                }
            }
        }

        list.reverse()
        return list

    }

}