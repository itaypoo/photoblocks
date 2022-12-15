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
import com.itaypoo.photoblockslib.User
import de.hdodenhof.circleimageview.CircleImageView

class UserPhotoAdapter(private val userList: MutableList<User>, private val context: Context):
    RecyclerView.Adapter<UserPhotoAdapter.ViewHolder>() {

    // Listener for item click
    var onItemClickListener: ((User) -> Unit)? = null

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView
        val nameText: TextView

        init {
            // Get views
            imageView = view.findViewById(R.id.userPhoto_imageView)
            nameText = view.findViewById(R.id.userPhoto_nameText)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_user_photo, parent, false)

        return UserPhotoAdapter.ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val user = userList[position]

        // Load the profile photo
        Glide.with(context).load(user.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(holder.imageView)
        holder.nameText.text = user.name

        // Invoke listener when view is clicked, pass the current photo pair
        holder.imageView.setOnClickListener {
            onItemClickListener?.invoke(user)
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int = userList.size


}