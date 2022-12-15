package com.itaypoo.photoblocks

import android.app.ActivityOptions
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.BlockListAdapter
import com.itaypoo.helpers.*
import com.itaypoo.photoblocks.databinding.ActivityHomeScreenBinding
import com.itaypoo.photoblockslib.Block


class HomeScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeScreenBinding
    private lateinit var menuDialog: Dialog

    private lateinit var storageRef: StorageReference
    private lateinit var database: FirebaseFirestore

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
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

        // Show welcome or welcome back message
        val type = intent.getIntExtra(Consts.Extras.SIGNIN_TYPE, Consts.LoginType.NO_LOGIN)
        if(type != Consts.LoginType.NO_LOGIN){
            if(type == Consts.LoginType.NEW_USER){
                // New user login
                val text = getString(R.string.welcome_message) + AppUtils.currentUser!!.name
                Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT).show()
            }
            else{
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
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this, binding.fab, "sharedview_button").toBundle())
        }

    }

    private fun setupTransitions(){
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            // set an exit transition
            exitTransition = Fade()
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun setupMenuDialog() {
        menuDialog = Dialog(this, R.style.CustomDialog)
        menuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        menuDialog.setContentView(R.layout.dialog_sidebar_menu)

        // Set dialog window width, height, background and position
        menuDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        menuDialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        menuDialog.window?.setGravity(Gravity.RIGHT)

        // Get button views
        val buttonContacts = menuDialog.findViewById<Button>(R.id.nbs_contactsButton)
        val buttonJoinWithCode = menuDialog.findViewById<Button>(R.id.nbs_joinWithCodeButton)
        val buttonOptions = menuDialog.findViewById<Button>(R.id.nbs_optionsButton)

        // Button on click listeners
        buttonContacts.setOnClickListener {
            menuDialog.dismiss()
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

    private fun loadBlocksJoined() {
        // Get all blocks that the current user is a member of
        database.collection("blockMembers").whereEqualTo("memberId", AppUtils.currentUser!!.databaseId).get()
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
        val finalBlockList: MutableList<Block> = mutableListOf()

        // Load all blocks that were found to contain the user as a member
        database.collection("blocks").get().addOnFailureListener {

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
                    finalBlockList.add(newblock)
                }

            }

            // All relevant blocks are loaded. Now lets fill the RecyclerView with them -
            val adapter = BlockListAdapter(finalBlockList, this)
            binding.blockRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.blockRecyclerView.adapter = adapter

            adapter.onItemClickListener = {
                // On block clicked
                AppUtils.passedBlock = it
                startActivity(Intent(this, ViewBlockActivity::class.java))
            }

        }
    }

}