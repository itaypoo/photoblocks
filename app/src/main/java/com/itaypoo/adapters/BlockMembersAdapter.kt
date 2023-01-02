package com.itaypoo.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.BlockMember

class BlockMembersAdapter(private val membersList: MutableList<BlockMember>, private val context: Context) :
    RecyclerView.Adapter<BlockMembersAdapter.ViewHolder>() {

    // Listener for item click
    var onRemoveButtonClicked: ((BlockMember) -> Unit)? = null

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView
        val roleText: TextView
        val profilePicture: ImageView
        val removeButton: Button

        init {
            nameText = view.findViewById(R.id.blockMemberItem_nameText)
            roleText = view.findViewById(R.id.blockMemberItem_roleText)
            profilePicture = view.findViewById(R.id.blockMemberItem_profilePicture)
            removeButton = view.findViewById(R.id.blockMemberItem_removeButton)
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
        val member = membersList[position]

        viewHolder.nameText.text = member.

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = pairList.size

}