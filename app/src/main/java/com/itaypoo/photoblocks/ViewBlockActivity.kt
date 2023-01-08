package com.itaypoo.photoblocks

import android.animation.TimeInterpolator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.BlockCommentsAdapter
import com.itaypoo.adapters.BlockMembersAdapter
import com.itaypoo.adapters.PostListAdapter
import com.itaypoo.helpers.*
import com.itaypoo.photoblocks.databinding.ActivityViewBlockBinding
import com.itaypoo.photoblockslib.*


class ViewBlockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewBlockBinding

    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var currentBlock: Block
    private lateinit var postCreatorList: MutableList<Pair<BlockPost, User>>

    private var commentsList = mutableListOf<Pair<BlockComment, User>>()
    private var memberUserList = mutableListOf<Pair<BlockMember, User?>>()
    private var currentUserIsAdmin = false
    private var currentUserIsMember = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = getColor(R.color.background_variant)

        // Get firebase connections
        database = Firebase.firestore
        storageRef = FirebaseStorage.getInstance().reference

        // Get passed block
        if(intent.hasExtra(Consts.Extras.PASSED_BLOCK)){
            // Safely use currentBlock.
            currentBlock = intent.getSerializableExtra(Consts.Extras.PASSED_BLOCK) as Block
        }
        else{
            Toast.makeText(this, "Error getting block data.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Init block view
        initTopBarUi()
        topBarAnimator.openTopBar(binding, true)

        loadBlockPosts()
        loadCommentsList()
        loadMembersList()
        registerForContextMenu(binding.moreButton)
        binding.uploadPostFAB.visibility = View.GONE

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.uploadPostFAB.setOnClickListener {
            if(currentUserIsMember){
                val postIntent = Intent(this, UploadPostActivity::class.java)
                postIntent.putExtra(Consts.Extras.UPLOADPOST_INPUT_BLOCKID, currentBlock.databaseId)
                startActivity(postIntent)
            }
            else Snackbar.make(binding.root, getString(R.string.not_block_member), Snackbar.LENGTH_SHORT).show()
        }

        binding.postRecycler.setOnScrollChangeListener { view, i, i2, i3, dy ->
            // I have no idea what i,i2,i3 are but i know i4 is delta y

            if(dy > 60){
                topBarAnimator.openTopBar(binding, false)
            }
            else if(dy < -30){
                topBarAnimator.closeTopBar(binding)
            }
        }

        // Create popup menu
        var popupMenu = PopupMenu(this, binding.moreButton)
        if(currentBlock.creatorId == AppUtils.currentUser!!.databaseId!!){
            popupMenu = createBlockCreatorPopupMenu()
        }
        else { popupMenu = createNormalPopupMenu() }
        
        // Show menu
        binding.moreButton.setOnClickListener {
            popupMenu.show()
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun createNormalPopupMenu(): PopupMenu{
        val popupMenu = PopupMenu(this, binding.moreButton)
        popupMenu.menuInflater.inflate(R.menu.menu_block, popupMenu.menu)
        // On menu item clicked
        popupMenu.setOnMenuItemClickListener {
            if(it.itemId == R.id.blockMenuItem_comments){
                // Comments item clicked
                openCommentsBottomSheetDialog()
            }
            else if(it.itemId == R.id.blockMenuItem_members){
                // Members item clicked
                openMembersBottomSheetDialog()
            }
            return@setOnMenuItemClickListener true
        }
        
        return popupMenu
    }
    
    private fun createBlockCreatorPopupMenu(): PopupMenu{
        val popupMenu = PopupMenu(this, binding.moreButton)
        popupMenu.menuInflater.inflate(R.menu.menu_block_creator, popupMenu.menu)
        // On menu item clicked
        popupMenu.setOnMenuItemClickListener {
            if(it.itemId == R.id.blockMenuItem_comments){
                // Comments item clicked
                openCommentsBottomSheetDialog()
            }
            else if(it.itemId == R.id.blockMenuItem_members){
                // Members item clicked
                openMembersBottomSheetDialog()
            }
            else if(it.itemId == R.id.blockMenuItem_collage){
                // Collage settings item clicked
                Toast.makeText(this, "Todo", Toast.LENGTH_SHORT).show()
            }
            else if(it.itemId == R.id.blockMenuItem_delete){
                // Delete block item clicked
                openDeleteBlockDialog()
            }
            return@setOnMenuItemClickListener true
        }

        return popupMenu
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    private fun openDeleteBlockDialog(){
        val confirmDialog = CustomDialogMaker.makeYesNoDialog(
            this,
            getString(R.string.are_you_sure_delete_block),
            getString(R.string.delete_block_desc)
        )
        confirmDialog.dialog.show()
        confirmDialog.noButton.setOnClickListener { confirmDialog.dialog.dismiss() }
        
        confirmDialog.yesButton.setOnClickListener { 
            // Go to delete block activity
            confirmDialog.dialog.dismiss()
            val deleteIntent = Intent(this, DeleteBlockActivity::class.java)
            val bundle = Bundle()

            bundle.putSerializable(Consts.Extras.PASSED_BLOCK, currentBlock)
            deleteIntent.putExtras(bundle)

            startActivity(deleteIntent)
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    private fun loadBlockPosts(){
        database.collection(Consts.BDPath.blockPosts).whereEqualTo("blockId", currentBlock.databaseId).get().addOnSuccessListener {
            postCreatorList = mutableListOf()

            // Init recycler
            val adapter = PostListAdapter(postCreatorList, this, database)
            binding.postRecycler.layoutManager = LinearLayoutManager(this)
            binding.postRecycler.adapter = adapter

            // Loop through all posts
            for(doc in it){
                val post = FirebaseUtils.ObjectFromDoc.BlockPost(doc)
                // Get the creator of this post
                database.collection(Consts.BDPath.users).document(post.creatorId).get().addOnSuccessListener {
                    val creator = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                    postCreatorList.add(Pair(post, creator))

                    // Update recycler
                    adapter.notifyDataSetChanged()
                }
            }

            // Adapter onClick methods
            adapter.onLikeButtonClicked = { post: BlockPost, creator: User ->
                // Upload a like for this post
                val newLike = PostLike(
                    null, Timestamp.now().toDate(),
                    AppUtils.currentUser!!.databaseId!!, post.databaseId!!)

                database.collection(Consts.BDPath.postLikes).add(newLike.toHashMap())

                // Upload a post like notif
                val likeNotif = Notification(null, Timestamp.now().toDate(),
                post.creatorId, AppUtils.currentUser!!.databaseId!!, NotificationType.POST_LIKE, post.databaseId!!)

                database.collection(Consts.BDPath.userNotifications).add(likeNotif.toHashMap())
            }
            adapter.onUnlikeButtonClicked = { post: BlockPost, creator: User ->
                // Delete all likes for this post (by this user)
                val q = database.collection(Consts.BDPath.postLikes).whereEqualTo("userId", AppUtils.currentUser!!.databaseId!!)
                q.whereEqualTo("postId", post.databaseId!!).get().addOnSuccessListener {
                    for(doc in it){
                        database.collection(Consts.BDPath.postLikes).document(doc.id).delete()
                    }
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun loadCommentsList(){
        database.collection(Consts.BDPath.blockComments).whereEqualTo("blockId", currentBlock.databaseId).get().addOnSuccessListener {
            // add all comments to the list
            for(doc in it){
                val comment = FirebaseUtils.ObjectFromDoc.BlockComment(doc)
                // load the comments user

                database.collection(Consts.BDPath.users).document(comment.authorId).get().addOnSuccessListener {
                    val author = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                    commentsList.add(Pair(comment, author))

                    // Sort list by date
                    commentsList = sortCommentsByDate(commentsList)
                }

            }
        }
    }

    private fun loadMembersList(){
        database.collection(Consts.BDPath.blockMembers).whereEqualTo("blockId", currentBlock.databaseId).get().addOnSuccessListener {
            val memberList = mutableListOf<BlockMember>()
            for(doc in it){
                val member = FirebaseUtils.ObjectFromDoc.BlockMember(doc)
                memberList.add(member)

                if(member.memberId == AppUtils.currentUser?.databaseId) {
                    currentUserIsAdmin = member.isAdmin
                    currentUserIsMember = true
                    binding.uploadPostFAB.visibility = View.VISIBLE
                }
            }
            // We have all the BlockMember classes. Now load the member's User classes.
            if(currentBlock.creatorId == AppUtils.currentUser?.databaseId) currentUserIsAdmin = true
            loadMembersUsers(memberList)
        }
    }
    private fun loadMembersUsers(memberList: MutableList<BlockMember>){
        val queriesAmount = memberList.size
        var queriesComplete = 0

        for(member in memberList){
            database.collection(Consts.BDPath.users).document(member.memberId).get().addOnSuccessListener {
                val user = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                memberUserList.add(Pair(member, user))
                queriesComplete += 1
                Log.d("Members load", "$queriesComplete complete")

                // Check if all users were loaded
                if(queriesComplete == queriesAmount){
                    // Done loading members and users
                    Log.d("Members load", "ALL complete")
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun initTopBarUi() {
        val bgColor = currentBlock.secondaryColor.toInt()
        val btnColor = currentBlock.primaryColor.toInt()
        val bgInvertedColor = AppUtils.invertColor(bgColor, 255)

        Glide.with(this).load(currentBlock.coverImageUrl).into(binding.blockImagePreview)

        binding.titleTextSmall.text = currentBlock.title
        binding.titleTextBig.text = currentBlock.title

        window.statusBarColor = bgColor
        binding.topBarCardView.setBackgroundColor( bgColor )
        binding.gradientImage.imageTintList = ColorStateList.valueOf( bgColor )

        binding.backButton.imageTintList = ColorStateList.valueOf( bgInvertedColor )
        binding.moreButton.imageTintList = ColorStateList.valueOf( bgInvertedColor )
        binding.titleTextBig.setTextColor( bgInvertedColor )
        binding.titleTextSmall.setTextColor( bgInvertedColor )

        binding.uploadPostFAB.backgroundTintList = ColorStateList.valueOf( btnColor )

    }

    // Top bar animator object
    internal object topBarAnimator{
        var mainInterpolator: TimeInterpolator = DecelerateInterpolator()
        private var animDuration: Long = 300

        private var open = true
        private var animating = false
        private val onTimerComplete = {
            animating = false
        }

        fun openTopBar(binding: ActivityViewBlockBinding, isInstant: Boolean){
            if(animating) return
            if(open) return
            if(isInstant) animDuration = 1
            open = true
            animating = true

            // Scale up card
            ObjectViewAnimator.animateViewHeight(binding.topBarCardView, 500, animDuration, mainInterpolator)
            // Fade in big title, preview image
            ObjectViewAnimator.fadeView(binding.titleTextBig, 0.0f, 1.0f, animDuration, mainInterpolator)
            ObjectViewAnimator.fadeView(binding.blockImagePreview, 0.0f, 0.3f, animDuration, mainInterpolator)
            // Fade out small title
            ObjectViewAnimator.fadeView(binding.titleTextSmall, 1.0f, 0.0f, animDuration, mainInterpolator)

            ObjectViewAnimator.startTimer(animDuration, 0, onTimerComplete)

            if(isInstant) animDuration = 300
        }

        fun closeTopBar(binding: ActivityViewBlockBinding){
            if(animating) return
            if(!open) return
            open = false
            animating = true

            // Scale down card
            ObjectViewAnimator.animateViewHeight(binding.topBarCardView, 180, animDuration, mainInterpolator)
            // Fade out big title, preview image
            ObjectViewAnimator.fadeView(binding.titleTextBig, 1.0f, 0.0f, animDuration, mainInterpolator)
            ObjectViewAnimator.fadeView(binding.blockImagePreview, 0.3f, 0.0f, animDuration, mainInterpolator)
            // Fade in small title
            ObjectViewAnimator.fadeView(binding.titleTextSmall, 0.0f, 1.0f, animDuration, mainInterpolator)

            ObjectViewAnimator.startTimer(animDuration, 0, onTimerComplete)
        }
    }

    // Comments Bottom Sheet Dialog
    private fun openCommentsBottomSheetDialog(){
        val dialog = BottomSheetDialog(this)

        val contentView = layoutInflater.inflate(R.layout.bottom_dialog_block_comments, binding.root, false)
        dialog.setContentView(contentView)

        val addCommentButton = contentView.findViewById<Button>(R.id.commentsBottomDialog_addCommentButton)
        val quitButton = contentView.findViewById<Button>(R.id.commentsBottomDialog_quitButton)
        val commentsRecycler = contentView.findViewById<RecyclerView>(R.id.commentsBottomDialog_commentsRecycler)

        if(!currentUserIsMember) addCommentButton.visibility = View.GONE

        quitButton.setOnClickListener {
            dialog.dismiss()
        }
        addCommentButton.setOnClickListener {
            // open a text input dialog, then upload a new comment
            if(!currentUserIsMember){
                Snackbar.make(binding.root, getString(R.string.not_block_member), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newCommentDialog = CustomDialogMaker.makeTextInputDialog(
                this,
                getString(R.string.new_comment_title),
                getString(R.string.new_comment_hint)
            )

            newCommentDialog.cancelButton.setOnClickListener {
                newCommentDialog.dialog.dismiss()
            }
            newCommentDialog.doneButton.setOnClickListener {
                newCommentDialog.hideError()
                val commentText = newCommentDialog.editText.text.toString()
                if(commentText.isBlank()){
                    newCommentDialog.setError(getString(R.string.new_comment_no_text_error))
                }
                else{
                    uploadComment(commentText)
                    newCommentDialog.dialog.dismiss()
                }
            }

            dialog.dismiss()
            newCommentDialog.dialog.show()
        }

        val adapter = BlockCommentsAdapter(commentsList, this)
        commentsRecycler.layoutManager = LinearLayoutManager(this)
        commentsRecycler.adapter = adapter

        dialog.show()
    }

    private fun uploadComment(commentText: String) {
        // Show loading dialog
        val d = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.uploading_comment))
        d.dialog.show()

        // Upload comment
        val commentModel = BlockComment(
            null,
            Timestamp.now().toDate(),
            AppUtils.currentUser!!.databaseId!!,
            currentBlock.databaseId!!,
            commentText,
        )
        database.collection(Consts.BDPath.blockComments).add(commentModel.toHashMap()).addOnFailureListener {
            if(it is FirebaseNetworkException){
                Snackbar.make(binding.root, getString(R.string.uploading_comment_failed_network), Snackbar.LENGTH_SHORT).show()
            }
            else{
                Snackbar.make(binding.root, getString(R.string.uploading_comment_failed), Snackbar.LENGTH_SHORT).show()
            }
            d.dialog.dismiss()

        }.addOnSuccessListener {
            // Add this comment to the comments list
            commentModel.databaseId = it.id
            commentsList.add(Pair(commentModel, AppUtils.currentUser!!))
            // Sort the list by date
            commentsList = sortCommentsByDate(commentsList)
            d.dialog.dismiss()

            // Upload a comment notif
            val notif = Notification(null, Timestamp.now().toDate(),
                currentBlock.creatorId, AppUtils.currentUser!!.databaseId!!,
                NotificationType.BLOCK_COMMENT, commentModel.databaseId!!)
            database.collection(Consts.BDPath.userNotifications).add(notif.toHashMap())
        }


    }

    // Members Bottom Sheet Dialog
    private fun openMembersBottomSheetDialog(){
        val dialog = BottomSheetDialog(this)

        val contentView = layoutInflater.inflate(R.layout.bottom_dialog_block_members, binding.root, false)
        dialog.setContentView(contentView)

        val addMemberButton = contentView.findViewById<Button>(R.id.membersBottomDialog_addMemberButton)
        val quitButton = contentView.findViewById<Button>(R.id.membersBottomDialog_quitButton)
        val membersRecycler = contentView.findViewById<RecyclerView>(R.id.membersBottomDialog_membersRecycler)

        addMemberButton.visibility = if(currentUserIsAdmin) { View.VISIBLE } else { View.GONE }

        quitButton.setOnClickListener {
            dialog.dismiss()
        }
        addMemberButton.setOnClickListener {
            if(!currentUserIsMember){
                Snackbar.make(binding.root, getString(R.string.not_block_member), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            // choose a contact to invite
            val contactIntent = Intent(this, ChooseContactActivity::class.java)
            contactIntent.putExtra(Consts.Extras.CHOOSECONTECT_INPUT_CHOOSE_TYPE, Consts.ChooseType.CHOOSE_ANY_USER)
            startActivityForResult(contactIntent, Consts.RequestCode.CHOOSE_CONTACT_ACTIVITY)
        }

        val adapter = BlockMembersAdapter(memberUserList, currentBlock.creatorId, this)
        membersRecycler.layoutManager = LinearLayoutManager(this)
        membersRecycler.adapter = adapter

        dialog.show()

        adapter.onItemClicked = {
            if(it.first.memberId != AppUtils.currentUser?.databaseId){
                if(currentUserIsAdmin && !it.first.isAdmin){
                    dialog.dismiss()
                    // If the current user is an admin, and the selected user is not
                    openMemberClickedDialog(it)
                }
                else if(currentBlock.creatorId == AppUtils.currentUser?.databaseId){
                    dialog.dismiss()
                    // If the current user is the creator, and the selected is an admin
                    openMemberClickedDialog(it)
                }
                else{
                    dialog.dismiss()
                    // If the current user is not an admin OR both selected and current are admins
                    val d = CustomDialogMaker.makeUserProfileDialog(
                        this, it.second, false, false, getString(R.string.view_profile), getString(R.string.back_button))
                    d.dialog.show()
                    d.noButton.setOnClickListener { d.dialog.dismiss() }
                }
            }
        }

    }

    private fun openMemberClickedDialog(pair: Pair<BlockMember, User>){
        val member = pair.first; val user = pair.second

        val options: Array<String>
        if(AppUtils.currentUser?.databaseId == currentBlock.creatorId){
            // Current user is the creator
            val promoteDemote = if(member.isAdmin) { getString(R.string.demote_member) } else { getString(R.string.promote_member) }
            options = arrayOf(getString(R.string.view_profile), getString(R.string.remove_member), promoteDemote)
        }
        else{
            // Current user is an admin
            options = arrayOf(getString(R.string.view_profile), getString(R.string.remove_member))
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(user.name)
            setItems(options) { dialog, which ->
                when(which){
                    0 -> {
                        // View profile
                    }
                    1 -> {
                        // Remove member
                        removeMember(member)
                    }
                    2 -> {
                        // Promote / Demote member
                        promoteDemoteMember(member)
                    }
                }

            }
        }.show()
    }

    private fun removeMember(member: BlockMember){
        // Show loading dialog
        val d = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.removing_member))
        d.dialog.show()
        // Remove at server side
        database.collection(Consts.BDPath.blockMembers).document(member.databaseId!!).delete().addOnSuccessListener {
            d.dialog.dismiss()
            openMembersBottomSheetDialog()
        }
        // Remove at client side
        var pairToRemove: Pair<BlockMember, User?>? = null
        for(pair in memberUserList){
            if(pair.first == member) pairToRemove = pair
        }
        if(pairToRemove != null )memberUserList.remove(pairToRemove)
    }
    private fun promoteDemoteMember(member: BlockMember){
        val d = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.doing_stuff))
        d.dialog.show()
        // Server side
        database.collection(Consts.BDPath.blockMembers).document(member.databaseId!!).update("isAdmin", !member.isAdmin).addOnSuccessListener {
            d.dialog.dismiss()
            openMembersBottomSheetDialog()
        }
        // Client side
        for(pair in memberUserList){
            if(pair.first == member) pair.first.isAdmin = !pair.first.isAdmin
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == Consts.RequestCode.CHOOSE_CONTACT_ACTIVITY && resultCode == RESULT_OK && data != null){
            val chosenUser = data.getSerializableExtra(Consts.Extras.CHOOSECONTACT_OUTPUT_USER) as User

            // Show loading dialog
            val d = CustomDialogMaker.makeLoadingDialog(this, getString(R.string.inviting) + " " + chosenUser.name)
            d.dialog.show()

            // Check if the user is not already in this block
            val usersList = mutableListOf<User?>()
            for(pair in memberUserList){
                usersList.add(pair.second)
            }
            var contains = false
            for(user in usersList){
                if(user?.databaseId == chosenUser.databaseId){
                    contains = true
                }
            }
            if(contains){
                d.dialog.dismiss()
                Snackbar.make(binding.root, getString(R.string.user_already_member_of_block), Snackbar.LENGTH_LONG).show()
            }
            else{
                val inviteNotif = Notification(null, Timestamp.now().toDate(),
                    chosenUser.databaseId!!,              // Recipient ID
                    AppUtils.currentUser!!.databaseId!!,  // Sender ID
                    NotificationType.BLOCK_INVITATION,    // Notif type
                    currentBlock.databaseId!!             // Notif content - Block ID
                )
                database.collection(Consts.BDPath.userNotifications).add(inviteNotif).addOnSuccessListener {
                    d.dialog.dismiss()
                }
            }

        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    fun sortCommentsByDate(entityList: MutableList<Pair<BlockComment, User>>): MutableList<Pair<BlockComment, User>>{

        val list = entityList

        // Bubble sort
        for(i in 0 until list.size-1){

            // last i elements are already in place
            for(h in 0 until list.size-i-1){
                // if list[h+1] < list[h]
                if(list[h+1].first.creationTime.before(list[h].first.creationTime)){
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