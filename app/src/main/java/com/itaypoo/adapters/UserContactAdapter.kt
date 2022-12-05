package com.itaypoo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.itaypoo.helpers.ContactModel
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.User
import de.hdodenhof.circleimageview.CircleImageView

class UserContactAdapter(private val userContactList: MutableList<Pair<ContactModel, User?>>, private val context: Context):
    RecyclerView.Adapter<UserContactAdapter.ViewHolder>() {

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val addIcon: ImageView
        val messageIcon: ImageView
        val profilePhoto: CircleImageView
        val topNameText: TextView
        val bottomNameText: TextView

        init {
            // Define click listener for the ViewHolder's View.
            addIcon = view.findViewById(R.id.contactItem_addIcon)
            messageIcon = view.findViewById(R.id.contactItem_messageIcon)
            profilePhoto = view.findViewById(R.id.contactItem_profilePhoto)
            topNameText = view.findViewById(R.id.contactItem_topNameText)
            bottomNameText = view.findViewById(R.id.contactItem_bottomNameText)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_contact_user, parent, false)

        return UserContactAdapter.ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val pair = userContactList[position]
        val contact: ContactModel = pair.first
        val user: User? = pair.second

        // init views
        if(user != null){
            // This pair is a contact that has a user

            holder.bottomNameText.visibility = View.VISIBLE
            holder.addIcon.visibility = View.VISIBLE
            holder.messageIcon.visibility = View.INVISIBLE

            holder.topNameText.text = contact.displayName
            holder.bottomNameText.text = contact.phoneNumber

            Glide.with(context).load(user.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(holder.profilePhoto)
        }
        else{
            // This pair is a contact that has not signed up to photoblocks

            holder.bottomNameText.visibility = View.GONE
            holder.addIcon.visibility = View.INVISIBLE
            holder.messageIcon.visibility = View.VISIBLE

            holder.topNameText.text = contact.displayName
            holder.bottomNameText.text = contact.phoneNumber

            holder.profilePhoto.setImageResource(R.drawable.unknown_user_profile_photo)
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int = userContactList.size


}