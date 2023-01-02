package com.itaypoo.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.itaypoo.helpers.AppUtils
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.BlockPost
import com.itaypoo.photoblockslib.User

class PostListAdapter(private val postCreatorList: MutableList<Pair<BlockPost, User>>, private val context: Context, val database: FirebaseFirestore) :
    RecyclerView.Adapter<PostListAdapter.ViewHolder>() {

    // Listener for item click
    var onPostImageClicked: ((post: BlockPost, creator: User) -> Unit)? = null
    var onLikeButtonClicked: ((post: BlockPost, creator: User) -> Unit)? = null
    var onUnlikeButtonClicked: ((post: BlockPost, creator: User) -> Unit)? = null

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postImage: ImageView
        val creatorPicture: ImageView
        val creatorNameText: TextView
        val descriptionText: TextView
        val likeButton: Button
        val unlikeButton: Button
        val likeAnimImage: ImageView

        init {
            // Define click listener for the ViewHolder's View.
            postImage = view.findViewById(R.id.postItem_postImage)
            creatorPicture = view.findViewById(R.id.postItem_uploaderPicture)
            creatorNameText = view.findViewById(R.id.postItem_uploaderNameText)
            descriptionText = view.findViewById(R.id.postItem_descriptionText)
            likeButton = view.findViewById(R.id.postItem_likeButton)
            unlikeButton = view.findViewById(R.id.postItem_unlikeButton)
            likeAnimImage = view.findViewById(R.id.postItem_likeAnimImage)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_item_post, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val post = postCreatorList[position].first
        val creator = postCreatorList[position].second

        val dateString = AppUtils.DateString(post.creationTime)
        val nameDateString = buildString {
            // Example string: creator | 6:30am | dec. 3rd 2022
            append(creator.name)
            append(" | ")
            append(dateString.hourAMPM)
            append(":")
            append(dateString.minute)
            append(dateString.AMPM)
            append(" | ")
            append(dateString.dayMonthText())
            append(" ")
            append(dateString.year.toString())
        }

        if(post.description == ""){
            viewHolder.descriptionText.visibility = View.GONE
            viewHolder.creatorNameText.text = nameDateString
        }
        else{
            viewHolder.descriptionText.visibility = View.VISIBLE
            viewHolder.creatorNameText.text = nameDateString
            viewHolder.descriptionText.text = post.description
        }

        Glide.with(context).load(creator.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(viewHolder.creatorPicture)
        Glide.with(context).load(post.imageUrl).placeholder(R.drawable.gray).into(viewHolder.postImage)

        viewHolder.likeAnimImage.visibility = View.GONE
        viewHolder.unlikeButton.visibility = View.GONE
        viewHolder.likeButton.visibility = View.GONE

        // Invoke onClick lambdas
        viewHolder.postImage.setOnClickListener {
            onPostImageClicked?.invoke(post, creator)
        }

        checkPostLikedState(viewHolder, position)
    }

    // Check if a post was liked or not by the user, update UI
    private fun checkPostLikedState(viewHolder: ViewHolder, position: Int){
        val post = postCreatorList[position].first
        val creator = postCreatorList[position].second

        val q = database.collection("postLikes").whereEqualTo("userId", AppUtils.currentUser!!.databaseId!!)
        q.whereEqualTo("postId", post.databaseId!!).get().addOnSuccessListener {
            var liked = false
            if(it.size() == 0){
                // User has not liked this post
                liked = false
                viewHolder.unlikeButton.visibility = View.GONE
                viewHolder.likeButton.visibility = View.VISIBLE
                Log.d("Post", "NO")
            }
            else{
                // User HAS liked this post
                liked = true
                viewHolder.unlikeButton.visibility = View.VISIBLE
                viewHolder.likeButton.visibility = View.GONE
                Log.d("Post", "YES")
            }

            viewHolder.unlikeButton.setOnClickListener {
                onUnlikeButtonClicked?.invoke(post, creator)
                liked = false
                viewHolder.unlikeButton.visibility = View.GONE
                viewHolder.likeButton.visibility = View.VISIBLE
            }
            viewHolder.likeButton.setOnClickListener {
                onLikeButtonClicked?.invoke(post, creator)
                liked = true
                viewHolder.unlikeButton.visibility = View.VISIBLE
                viewHolder.likeButton.visibility = View.GONE

                // Start like anim
                viewHolder.likeAnimImage.visibility = View.VISIBLE
                val bounceAnim = AnimationUtils.loadAnimation(context, R.anim.like_bounce_anim)
                viewHolder.likeAnimImage.startAnimation(bounceAnim)
                bounceAnim.setAnimationListener(object : AnimationListener{
                    override fun onAnimationStart(p0: Animation?) {}
                    override fun onAnimationRepeat(p0: Animation?) {}
                    // On anim end
                    override fun onAnimationEnd(p0: Animation?) {
                        viewHolder.likeAnimImage.visibility = View.GONE
                    }
                })

            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = postCreatorList.size

}