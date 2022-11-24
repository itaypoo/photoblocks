package com.itaypoo.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.itaypoo.photoblocks.R

class CuratedPhotosAdapter(private val pairList: MutableList<List<Any>>, private val context: Context) :
    RecyclerView.Adapter<CuratedPhotosAdapter.ViewHolder>() {

    // Listener for item click
    var onItemClickListener: ((List<Any>) -> Unit)? = null

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagePreview: ImageView
        val cardView: CardView

        init {
            // Define click listener for the ViewHolder's View.
            view.alpha = 0.0F
            imagePreview = view.findViewById(R.id.curatedPhotoItem_photoPreview)
            cardView = view.findViewById(R.id.curatedPhotoItemCard)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_item_curated_photo, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Load image to imageView using Glide
        // Fade in view after image is loaded
        val photoUrl = pairList[position][1]
        Glide.with(context).load(photoUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    // Image done loading. Fade in view and disable any listeners
                    viewHolder.itemView.animate().alpha(1.0F).setDuration(300).setListener(null)
                    return false
                }

            })
        .into(viewHolder.imagePreview)

        // Invoke listener when view is clicked, pass the current photo pair
        viewHolder.cardView.setOnClickListener {
            onItemClickListener?.invoke(pairList[position])
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = pairList.size

}