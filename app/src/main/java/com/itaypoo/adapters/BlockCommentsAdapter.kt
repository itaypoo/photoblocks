package com.itaypoo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewAnimator
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.itaypoo.helpers.ContactModel
import com.itaypoo.helpers.ObjectViewAnimator
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.BlockComment
import com.itaypoo.photoblockslib.User
import de.hdodenhof.circleimageview.CircleImageView

class BlockCommentsAdapter(private val commentsList: MutableList<Pair<BlockComment, User>>, private val context: Context):
    RecyclerView.Adapter<BlockCommentsAdapter.ViewHolder>() {

    // Listener for item click -UNUSED
    //var onItemClickListener: ((BlockComment) -> Unit)? = null

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePicture: ImageView
        val contentText: TextView
        val nameText: TextView

        init {
            // Get views
            profilePicture = view.findViewById(R.id.commentItem_profilePictureImage)
            contentText = view.findViewById(R.id.commentItem_contentText)
            nameText = view.findViewById(R.id.commentItem_nameText)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_block_comment, parent, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val comment = commentsList[position]

        // Load the profile photo
        Glide.with(context).load(comment.second.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(holder.profilePicture)
        holder.contentText.text = comment.first.content
        holder.nameText.text = comment.second.name

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int = commentsList.size


}