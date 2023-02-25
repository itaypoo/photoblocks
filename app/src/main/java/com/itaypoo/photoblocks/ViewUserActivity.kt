package com.itaypoo.photoblocks

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.adapters.BlockListAdapter
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityViewUserBinding
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.User
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ViewUserActivity : AppCompatActivity() {
    private val TAG = "ViewUserActivity"

    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var binding: ActivityViewUserBinding
    private lateinit var viewUser: User
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.firestore
        storageRef = FirebaseStorage.getInstance().reference

        // Get passed user
        if(intent.hasExtra(Consts.Extras.PASSED_USER)){
            viewUser = intent.getSerializableExtra(Consts.Extras.PASSED_USER) as User
        }
        else{
            // Error: No user passed
            Log.e(TAG, "onCreate: No user passed!", )
            finish()
        }

        loadBannerImage()
        loadUserStats()
        Glide.with(this).load(viewUser.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(binding.viewUserProfilePhoto)
        binding.viewUsernamePreview.text = viewUser.name

        // Load user block list only if that user is not private.
        if(viewUser.isPrivate){
            binding.viewUserPrivateProfileCard.visibility = View.VISIBLE
        }
        else{
            binding.viewUserPrivateProfileCard.visibility = View.GONE
            loadUserBlocks()
        }
        
        binding.viewUserBackButton.setOnClickListener { finish() }
    }
    
    private fun loadUserStats() {
        // Init views
        binding.viewUserJoinDateText.text = ""
        binding.viewUserBlockCountText.text = ""
        binding.viewUserImageCountText.text = ""
        binding.viewUserCommentCountText.text = ""

        // Show when the user joined the app
        val dateString = AppUtils.DateString(viewUser.creationTime)
        binding.viewUserJoinDateText.text = buildString {
            append(getString(R.string.joined))
            append(" ")
            append(dateString.dayMonthText())
            append(", ")
            append(dateString.year)
        }

        // Count blocks the user is in
        val query = database.collection(Consts.DBPath.blockMembers).whereEqualTo("memberId", viewUser.databaseId!!).count().get(
            AggregateSource.SERVER)
        query.addOnSuccessListener {
            binding.viewUserBlockCountText.text = it.count.toString() + " " + getString(R.string.stats_blocks_joined)
        }

        // Count posts uploaded
        val query2 = database.collection(Consts.DBPath.blockPosts).whereEqualTo("creatorId", viewUser.databaseId!!).count().get(
            AggregateSource.SERVER)
        query2.addOnSuccessListener {
            binding.viewUserImageCountText.text = it.count.toString() + " " + getString(R.string.images_uploaded)
        }

        // Count comments written
        val query3 = database.collection(Consts.DBPath.blockComments).whereEqualTo("authorId", viewUser.databaseId!!).count().get(
            AggregateSource.SERVER)
        query3.addOnSuccessListener {
            binding.viewUserCommentCountText.text = it.count.toString() + " " + getString(R.string.comments_written)
        }
    }

    private fun loadBannerImage() {
        if(viewUser.bannerImageUrl == null){
            // User does not have a banner. Replace it with a generated color from their pfp.
            binding.viewUserBannerGradient.visibility = View.INVISIBLE
            Glide.with(this)
                .load(viewUser.profilePhotoUrl).placeholder(R.drawable.default_profile_photo)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean
                    ): Boolean {
                        val colors = Palette.from(resource!!.toBitmap(100, 100)).generate()
                        val dominant = colors.getDominantColor(getColor(R.color.on_background_variant))
                        binding.viewUserBannerImage.setImageResource(R.drawable.gray)
                        binding.viewUserBannerImage.setColorFilter(dominant)
                        window.statusBarColor = dominant
                        return false
                    }

                })
                .into(binding.viewUserProfilePhoto)
        }
        else{
            // User has a banner image. Load it...
            Glide.with(this)
                .load(viewUser.bannerImageUrl).placeholder(R.drawable.gray)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean
                    ): Boolean {
                        // Set status bar color
                        val colors = Palette.from(resource!!.toBitmap(100, 100)).generate()
                        val dominant = colors.getDominantColor(getColor(R.color.on_background_variant))
                        window.statusBarColor = dominant

                        // Set gradient color
                        binding.viewUserBannerGradient.setColorFilter(dominant)
                        binding.viewUserBannerGradient.visibility = View.VISIBLE

                        binding.viewUserBannerImage.colorFilter = null
                        return false
                    }

                })
                .into(binding.viewUserBannerImage)
        }
    }

    private fun loadUserBlocks() {

        val memberModelsRef = database.collection(Consts.DBPath.blockMembers).whereEqualTo("memberId", viewUser.databaseId)

        GlobalScope.launch(Dispatchers.IO) {
            // Await all blocks ids that the user is in
            val docs = memberModelsRef.get().await()

            val idList = mutableListOf<String>()
            for(doc in docs){
                idList.add(doc.get("blockId") as String)
            }

            // Await all blocks
            val blockList = mutableListOf<Block>()
            for(id in idList){
                val q = database.collection(Consts.DBPath.blocks).document(id)
                val doc = q.get().await()
                blockList.add(FirebaseUtils.ObjectFromDoc.Block(doc))
            }

            // Set up recycler
            withContext(Dispatchers.Main){  // Only update UI in the main dispatcher!
                val adapter = BlockListAdapter(blockList, database,this@ViewUserActivity)
                binding.viewUserBlockRecycler.layoutManager = LinearLayoutManager(this@ViewUserActivity)
                binding.viewUserBlockRecycler.adapter = adapter

                // On block clicked
                adapter.onItemClickListener = {
                    val viewBlockIntent = Intent(this@ViewUserActivity, ViewBlockActivity::class.java)
                    val bundle = Bundle()

                    bundle.putSerializable(Consts.Extras.PASSED_BLOCK, it)
                    viewBlockIntent.putExtras(bundle)

                    startActivityForResult(viewBlockIntent, Consts.RequestCode.VIEW_BLOCK_NO_RETURN)
                }
            }
        }
    }

}