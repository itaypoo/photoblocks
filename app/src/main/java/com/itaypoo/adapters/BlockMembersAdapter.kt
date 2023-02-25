package com.itaypoo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.itaypoo.helpers.AppUtils
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.BlockMember
import com.itaypoo.photoblockslib.User

class BlockMembersAdapter(private val memberUserList: MutableList<Pair<BlockMember, User?>>, private val blockCreatorId: String, private val context: Context) :
    RecyclerView.Adapter<BlockMembersAdapter.ViewHolder>() {

    // Listener for item click
    var onItemClicked: ((Pair<BlockMember, User>) -> Unit)? = null

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView
        val roleText: TextView
        val profilePicture: ImageView
        val leaveIcon: ImageView

        init {
            nameText = view.findViewById(R.id.blockMemberItem_nameText)
            roleText = view.findViewById(R.id.blockMemberItem_roleText)
            profilePicture = view.findViewById(R.id.blockMemberItem_profilePicture)
            leaveIcon = view.findViewById(R.id.blockMemberItem_leaveIcon)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_item_block_member, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val member = memberUserList[position].first
        val user = memberUserList[position].second

        viewHolder.nameText.text = user?.name
        viewHolder.roleText.visibility = if(member.isAdmin) { View.VISIBLE } else { View.GONE }
        if(user?.databaseId == blockCreatorId){ viewHolder.roleText.text = context.getString(R.string.block_creator) }
        else{ viewHolder.roleText.text = context.getString(R.string.block_admin) }

        if(user?.databaseId == AppUtils.currentUser?.databaseId) {
            if(user?.databaseId != blockCreatorId) viewHolder.leaveIcon.visibility = View.VISIBLE
        }
        else { viewHolder.leaveIcon.visibility = View.GONE }

        Glide.with(context).load(user?.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(viewHolder.profilePicture)

        viewHolder.itemView.setOnClickListener {
            onItemClicked?.invoke(Pair(member, user!!))
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = memberUserList.size

}