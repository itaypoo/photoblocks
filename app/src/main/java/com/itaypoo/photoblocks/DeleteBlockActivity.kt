package com.itaypoo.photoblocks

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.CustomDialogMaker
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityDeleteBlockBinding
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.BlockComment
import com.itaypoo.photoblockslib.BlockPost
import com.itaypoo.photoblockslib.NotificationType

class DeleteBlockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeleteBlockBinding
    private lateinit var loadingDialog: CustomDialogMaker.LoadingDialog
    private lateinit var block: Block

    private lateinit var database: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var queriesStarted = 0
    private var queriesComplete = 0

    val tag = "Block deletion process"

    /*

    Steps of deleting a block:
    (all steps will start at the same time, asynchronously)

    loop through all posts:           Step 1
        delete post like notif        1a
        delete post likes             1b
        delete post image             1c
        delete the post               1d

    loop through all comments:        Step 2
        delete comment notif          2a
        delete the comment            2b

    delete all block invitations      Step 3
    delete all pending invitaitons    Step 4
    delete all block members          Step 5
    delete block cover image          Step 6

    delete the block.                 Final step 7


     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get firebase connections
        database = Firebase.firestore
        storage = FirebaseStorage.getInstance()

        if (intent.hasExtra(Consts.Extras.PASSED_BLOCK)) {
            block = intent.getSerializableExtra(Consts.Extras.PASSED_BLOCK) as Block
        } else {
            // No block passed
            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.deleteBlockCancelButton.setOnClickListener {
            finish()
        }

        binding.deleteBlockConfirmButton.setOnClickListener {
            if (binding.deleteBlockNameEditText.text.toString() != block.title) {
                binding.deleteBlockErrorText.visibility = View.VISIBLE
                binding.deleteBlockErrorText.text = getString(R.string.names_do_not_match)
            } else {
                // Start deletion process
                loadingDialog =
                    CustomDialogMaker.makeLoadingDialog(this, getString(R.string.deleting_block), true)
                loadingDialog.dialog.show()
                loadingDialog.setProgressMin(0)

                getAllPosts()              // Step 1
                getAllComments()           // Step 2
                deleteBlockInvitations()   // Step 3
                deletePendingInvitations() // Step 4
                deleteBlockMembers()       // Step 5
                deleteCoverImage()         // Step 6
            }
        }

    }

    private fun queryCompleted() {
        queriesComplete += 1
        loadingDialog.setProgressMax(queriesStarted)
        loadingDialog.setProgress(queriesComplete)
        if (queriesComplete >= queriesStarted) {
            Toast.makeText(this, "deletion complete", Toast.LENGTH_SHORT).show()

            // Now all steps are complete - delete the block itself
            database.collection(Consts.DBPath.blocks).document(block.databaseId!!).delete().addOnSuccessListener {
                loadingDialog.dialog.dismiss()
                startActivity(Intent(this, HomeScreenActivity::class.java))
            }
        }
    }

    // Post deletion //////////////////////////////////////////////////////////////////////////////

    private fun getAllPosts() {  // Step 1
        queriesStarted += 1
        database.collection(Consts.DBPath.blockPosts).whereEqualTo("blockId", block.databaseId).get()
            .addOnSuccessListener {
                val postsList = mutableListOf<BlockPost>()
                for (doc in it) {
                    val post = FirebaseUtils.ObjectFromDoc.BlockPost(doc)
                    postsList.add(post)
                }

                for (post in postsList) {
                    queriesStarted += 4
                    deletePostLikeNotif(post) // 1a
                    deletePostLikes(post)     // 1b
                    deletePostImage(post)     // 1c
                    deletePost(post)          // 1d
                }

                queryCompleted()
            }
    }

    private fun deletePostLikeNotif(post: BlockPost) {  // Step 1a
        database.collection(Consts.DBPath.userNotifications).whereEqualTo("content", post.databaseId).get()
            .addOnSuccessListener {
                val notifIdList = mutableListOf<String>()
                for (doc in it) {
                    notifIdList.add(doc.id)
                }

                // Delete all notifs in the list
                if(notifIdList.size == 0){
                    queryCompleted()
                }
                for (notifId in notifIdList) {
                    database.collection(Consts.DBPath.userNotifications).document(notifId).delete()
                        .addOnSuccessListener {
                            Log.d(tag, "Deleted post like notification (ID $notifId)")
                            notifIdList.remove(notifId)
                            if (notifIdList.size == 0) {
                                // All like notifs for this post deleted
                                queryCompleted()
                            }
                        }
                }
            }

    }

    private fun deletePostLikes(post: BlockPost) {  // Step 1b
        database.collection(Consts.DBPath.postLikes).whereEqualTo("postId", post.databaseId).get()
            .addOnSuccessListener {
                val likesIdList = mutableListOf<String>()
                for (doc in it) {
                    likesIdList.add(doc.id)
                }

                // Delete all likes in the list
                if(likesIdList.size == 0){
                    queryCompleted()
                }
                for (likeId in likesIdList) {
                    database.collection(Consts.DBPath.postLikes).document(likeId).delete()
                        .addOnSuccessListener {
                            Log.d(tag, "Delteed post like (ID $likeId)")
                            likesIdList.remove(likeId)
                            if (likesIdList.size == 0) {
                                // ALl likes for this post deleted
                                queryCompleted()
                            }
                        }
                }
            }

    }

    private fun deletePostImage(post: BlockPost) { // Step 1c
        val imageRef = storage.getReferenceFromUrl(post.imageUrl)
        imageRef.delete().addOnSuccessListener {
            // Post image deleted
            Log.d(tag, "Post image deleted (Url ${post.imageUrl})")
            queryCompleted()
        }
    }

    private fun deletePost(post: BlockPost) { // Step 1d
        database.collection(Consts.DBPath.blockPosts).document(post.databaseId!!).delete()
            .addOnSuccessListener {
                // Post deleted
                Log.d(tag, "Deleted post (ID ${post.databaseId})")
                queryCompleted()
            }
    }

    // Comment deletion ///////////////////////////////////////////////////////////////////////////

    private fun getAllComments() { // Step 2
        queriesStarted += 1
        database.collection(Consts.DBPath.blockComments).whereEqualTo("blockId", block.databaseId).get()
            .addOnSuccessListener {
                val commentsList = mutableListOf<BlockComment>()
                for (doc in it) {
                    val comment = FirebaseUtils.ObjectFromDoc.BlockComment(doc)
                    commentsList.add(comment)
                }

                for (comment in commentsList) {
                    queriesStarted += 2
                    deleteCommentNotif(comment)
                    deleteComment(comment)
                }

                queryCompleted()
            }
    }

    private fun deleteCommentNotif(comment: BlockComment) {  // Step 2a
        database.collection(Consts.DBPath.userNotifications).whereEqualTo("content", comment.databaseId).get()
            .addOnSuccessListener {
                val notifIdList = mutableListOf<String>()
                for (doc in it) {
                    notifIdList.add(doc.id)
                }

                // Delete all notifs in the list
                if(notifIdList.size == 0){
                    queryCompleted()
                }
                for (notifId in notifIdList) {
                    database.collection(Consts.DBPath.userNotifications).document(notifId).delete()
                        .addOnSuccessListener {
                            Log.d(tag, "Deleted block comment notification (ID $notifId)")
                            notifIdList.remove(notifId)
                            if (notifIdList.size == 0) {
                                // All comment notifs for this block deleted
                                queryCompleted()
                            }
                        }
                }
            }

    }

    private fun deleteComment(comment: BlockComment) {  // Step 2b
        database.collection(Consts.DBPath.blockComments).document(comment.databaseId!!).delete()
            .addOnSuccessListener {
                // Comment deleted
                Log.d(tag, "Deleted comment (ID ${comment.databaseId})")
                queryCompleted()
            }
    }

    // Misc deletions /////////////////////////////////////////////////////////////////////////////

    private fun deleteBlockInvitations() {
        queriesStarted += 1
        database.collection(Consts.DBPath.userNotifications)
            .whereEqualTo("type", NotificationType.BLOCK_INVITATION)
            .whereEqualTo("content", block.databaseId).get().addOnSuccessListener {
            val notifIdList = mutableListOf<String>()
            for (doc in it) {
                notifIdList.add(doc.id)
            }

            // Delete all notifs in the list
            if(notifIdList.size == 0){
                queryCompleted()
            }
            for(notifId in notifIdList) {
                database.collection(Consts.DBPath.userNotifications).document(notifId).delete()
                    .addOnSuccessListener {
                        Log.d(tag, "Deleted block invitation notification (ID $notifId)")
                        notifIdList.remove(notifId)
                        if (notifIdList.size == 0) {
                            // All block invitation notifs for this block deleted
                            queryCompleted()
                        }
                    }
            }
        }
    }

    private fun deletePendingInvitations() {
        queriesStarted += 1
        database.collection(Consts.DBPath.pendingBlockInvitations).whereEqualTo("blockId", block.databaseId).get().addOnSuccessListener {
            val inviteIdList = mutableListOf<String>()
            for (doc in it) {
                inviteIdList.add(doc.id)
            }

            // Delete all notifs in the list
            if(inviteIdList.size == 0){
                queryCompleted()
            }
            for(inviteId in inviteIdList) {
                database.collection(Consts.DBPath.pendingBlockInvitations).document(inviteId).delete()
                    .addOnSuccessListener {
                        Log.d(tag, "Deleted pending block invitation (ID $inviteId)")
                        inviteIdList.remove(inviteId)
                        if (inviteIdList.size == 0) {
                            // All block invitation notifs for this block deleted
                            queryCompleted()
                        }
                    }
            }
        }
    }

    private fun deleteBlockMembers() {
        queriesStarted += 1
        database.collection(Consts.DBPath.blockMembers).whereEqualTo("blockId", block.databaseId).get().addOnSuccessListener {
            val memberIdList = mutableListOf<String>()
            for(doc in it){
                memberIdList.add(doc.id)
            }

            for(memberId in memberIdList){
                database.collection(Consts.DBPath.blockMembers).document(memberId).delete().addOnSuccessListener {
                    Log.d(tag, "Deleted block member (ID $memberId)")
                    memberIdList.remove(memberId)
                    if(memberIdList.size == 0) {
                        // All block members for this block deleted
                        queryCompleted()
                    }
                }
            }
        }
    }

    private fun deleteCoverImage(){
        // Get all curated photos, to check if the cover image is not one of them.
        queriesStarted += 1
        database.collection(Consts.DBPath.curatedPhotos).get().addOnSuccessListener {
            val curatedPhotosList = mutableListOf<String>()
            for(doc in it){
                curatedPhotosList.add(doc.get("photoUrl") as String)
            }

            if(curatedPhotosList.contains(block.coverImageUrl)){
                Log.d(tag, "Block cover image is a Curated Photo. Not deleting image.")
            }
            else{
                queriesStarted += 1
                storage.getReferenceFromUrl(block.coverImageUrl).delete().addOnSuccessListener {
                    // Removed cover image
                    queryCompleted()
                }
            }
            queryCompleted()
        }

    }

}