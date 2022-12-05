package com.itaypoo.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.itaypoo.helpers.ContactModel
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.User

class UserContactAdapter(private val userContactList: MutableList<Pair<ContactModel?, User?>>, private val context: Context):
    RecyclerView.Adapter<UserContactAdapter.ViewHolder>() {

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coverImagePreview: ImageView
        val imageGradient: ImageView

        init {
            // Define click listener for the ViewHolder's View.
            view.alpha = 0.0F
            coverImagePreview = view.findViewById(R.id.blockItem_photoPreview)
            imageGradient = view.findViewById(R.id.blockItem_gradient)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        TODO("Not yet implemented")
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int = userContactList.size


}