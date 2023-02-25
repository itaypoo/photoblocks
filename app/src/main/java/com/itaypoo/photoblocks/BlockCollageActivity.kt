package com.itaypoo.photoblocks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.postDelayed
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.CustomDialogMaker
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityBlockCollageBinding
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.BlockPost
import com.itaypoo.photoblockslib.User
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.Runnable
import java.net.URL

class BlockCollageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlockCollageBinding

    private lateinit var database: FirebaseFirestore
    private lateinit var block: Block

    private var postsList = mutableListOf<PostData>()
    private var handler: Handler? = null

    class PostData(val post: BlockPost, val user: User, val bitmap: Bitmap)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockCollageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.firestore

        if(intent.hasExtra(Consts.Extras.PASSED_BLOCK)) {
            block = intent.getSerializableExtra(Consts.Extras.PASSED_BLOCK) as Block
        }
        else{
            Log.e("CollageSettings", "No block passed")
            finish()
        }

        handler = Handler()
        getPostsAndUploaders()
    }

    private fun getPostsAndUploaders() {
        GlobalScope.launch(Dispatchers.IO) {
            // Get posts
            val postsRef = database.collection(Consts.DBPath.blockPosts)
            val postsQuery = postsRef.whereEqualTo("blockId", block.databaseId).orderBy("creationTime").get().await()
            postsList.clear()

            for(postDoc in postsQuery) {
                // get this posts image and uploader
                val post = FirebaseUtils.ObjectFromDoc.BlockPost(postDoc)
                val postImage = Glide.with(this@BlockCollageActivity).asBitmap().load(post.imageUrl).submit().get()
                val userDoc = database.collection(Consts.DBPath.users).document(post.creatorId).get().await()
                val user = FirebaseUtils.ObjectFromDoc.User(userDoc, contentResolver)

                postsList.add(PostData(post, user, postImage))

                withContext(Dispatchers.Main) {
                    startCollage()
                }
            }

        }
    }

    private fun startCollage() {
        // Loop through the post list and delay between each post
        for (i in postsList.indices) {
            handler?.postDelayed({

                // show post at index i
                var postData = postsList[i]
                binding.collageUploaderName.text = postData.user.name
                Glide.with(this@BlockCollageActivity).load(postData.user.profilePhotoUrl).into(binding.collageUploaderImage)
                binding.collageDescriptionText.text = postData.post.description
                binding.collageMainImage.setImageBitmap(postData.bitmap)
                binding.collageMainImage.scaleType = ImageView.ScaleType.CENTER_CROP

            }, (block.collageImageTime * (i+1)).toLong())

            handler?.postDelayed({

                // all posts are done
                Toast.makeText(this@BlockCollageActivity, "DONE", Toast.LENGTH_SHORT).show()

            }, (block.collageImageTime * (postsList.size + 1)).toLong())
        }
    }

    override fun onDestroy() {
        // When activity is exited, stop the timer (handler)
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

}