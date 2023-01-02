package com.itaypoo.photoblocks

import android.animation.TimeInterpolator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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
        registerForContextMenu(binding.moreButton)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.uploadPostFAB.setOnClickListener {
            val postIntent = Intent(this, UploadPostActivity::class.java)
            postIntent.putExtra(Consts.Extras.UPLOADPOST_INPUT_BLOCKID, currentBlock.databaseId)
            startActivity(postIntent)
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
        val popupMenu = PopupMenu(this, binding.moreButton)
        popupMenu.menuInflater.inflate(R.menu.menu_block, popupMenu.menu)
        // On menu item clicked
        popupMenu.setOnMenuItemClickListener {
            if(it.itemId == R.id.blockMenuItem_comments){
                // Comments item clicked
                val d = createCommentsBottomSheetDialog()
                d.show()
            }
            else if(it.itemId == R.id.blockMenuItem_members){
                // Members item clicked
                Toast.makeText(this, "Members", Toast.LENGTH_SHORT).show()
            }
            return@setOnMenuItemClickListener true
        }
        // Show menu
        binding.moreButton.setOnClickListener {
            popupMenu.show()
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun loadBlockPosts(){
        database.collection("blockPosts").whereEqualTo("blockId", currentBlock.databaseId).get().addOnSuccessListener {
            postCreatorList = mutableListOf()

            // Init recycler
            val adapter = PostListAdapter(postCreatorList, this, database)
            binding.postRecycler.layoutManager = LinearLayoutManager(this)
            binding.postRecycler.adapter = adapter

            // Loop through all posts
            for(doc in it){
                val post = FirebaseUtils.ObjectFromDoc.BlockPost(doc)
                // Get the creator of this post
                database.collection("users").document(post.creatorId).get().addOnSuccessListener {
                    val creator = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                    postCreatorList.add(Pair(post, creator))

                    // Update recycler
                    adapter.notifyDataSetChanged()
                }
            }

            // Adapter onClick methods
            adapter.onLikeButtonClicked = { post: BlockPost, creator: User ->
                // Upload a like for this post
                val newLike = postLike(
                    null, Timestamp.now().toDate(),
                    AppUtils.currentUser!!.databaseId!!, post.databaseId!!)

                database.collection("postLikes").add(newLike.toHashMap())
            }
            adapter.onUnlikeButtonClicked = { post: BlockPost, creator: User ->
                // Delete all likes for this post (by this user)
                val q = database.collection("postLikes").whereEqualTo("userId", AppUtils.currentUser!!.databaseId!!)
                q.whereEqualTo("postId", post.databaseId!!).get().addOnSuccessListener {
                    for(doc in it){
                        database.collection("postLikes").document(doc.id).delete()
                    }
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun loadCommentsList(){
        database.collection("blockComments").whereEqualTo("blockId", currentBlock.databaseId).get().addOnSuccessListener {
            // add all comments to the list
            for(doc in it){
                val comment = FirebaseUtils.ObjectFromDoc.BlockComment(doc)
                // load the comments user

                database.collection("users").document(comment.authorId).get().addOnSuccessListener {
                    val author = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                    commentsList.add(Pair(comment, author))

                    // Sort list by date
                    commentsList = sortCommentsByDate(commentsList)
                }

            }
        }
    }

    private fun loadMembersList(){
        database.collection("blockMembers").whereEqualTo("blockId", currentBlock.databaseId).get().addOnSuccessListener {
            val memberList = mutableListOf<BlockMember>()
            for(doc in it){
                val member = FirebaseUtils.ObjectFromDoc.BlockMember(doc)
                memberList.add(member)
            }
            // We have all the BlockMember classes. Now load the member's User classes.
            loadMembersUsers(memberList)
        }
    }
    private fun loadMembersUsers(memberList: MutableList<BlockMember>){
        val queriesAmount = memberList.size
        var queriesComplete = 0

        for(member in memberList){
            database.collection("users").document(member.memberId).get().addOnSuccessListener {
                val user = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                memberUserList.add(Pair(member, user))
                queriesComplete += 1

                // Check if all users were loaded
                if(queriesComplete == queriesAmount){
                    // Done loading members and users

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
    private fun createCommentsBottomSheetDialog(): BottomSheetDialog{
        val dialog = BottomSheetDialog(this)

        val contentView = layoutInflater.inflate(R.layout.bottom_dialog_block_comments, binding.root, false)
        dialog.setContentView(contentView)

        val addCommentButton = contentView.findViewById<Button>(R.id.commentsBottomDialog_addCommentButton)
        val quitButton = contentView.findViewById<Button>(R.id.commentsBottomDialog_quitButton)
        val commentsRecycler = contentView.findViewById<RecyclerView>(R.id.commentsBottomDialog_commentsRecycler)

        quitButton.setOnClickListener {
            dialog.dismiss()
        }
        addCommentButton.setOnClickListener {
            // open a text input dialog, then upload a new comment
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

        return dialog
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
        database.collection("blockComments").add(commentModel.toHashMap()).addOnFailureListener {
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
        }

    }

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