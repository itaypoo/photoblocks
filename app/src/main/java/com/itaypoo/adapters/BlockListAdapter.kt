package com.itaypoo.adapters

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
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
import com.itaypoo.helpers.Consts
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.Block

class BlockListAdapter(private val blockList: MutableList<Block>, private val database: FirebaseFirestore, private val context: Context) :
    RecyclerView.Adapter<BlockListAdapter.ViewHolder>() {

    // Listener for item click
    var onItemClickListener: ((Block) -> Unit)? = null

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coverImagePreview: ImageView
        val imageGradient: ImageView
        val membersText: TextView
        val titleText: TextView
        val cardView: CardView
        val dateText: TextView

        init {
            // Get views
            view.alpha = 0.0F
            view.translationY = 100F
            coverImagePreview = view.findViewById(R.id.blockItem_photoPreview)
            imageGradient = view.findViewById(R.id.blockItem_gradient)
            membersText = view.findViewById(R.id.blockItem_membersText)
            titleText = view.findViewById(R.id.blockItem_titleText)
            cardView = view.findViewById(R.id.blockItem_cardView)
            dateText = view.findViewById(R.id.blockItem_dateText)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_item_block, viewGroup, false)
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {

        val block: Block = blockList[position]

        // Init views
        viewHolder.titleText.text = block.title
        viewHolder.titleText.setTextColor( AppUtils.invertColor(block.secondaryColor.toInt(), 255) )
        viewHolder.dateText.setTextColor( AppUtils.invertColor(block.secondaryColor.toInt(), 200) )
        viewHolder.imageGradient.imageTintList = ColorStateList.valueOf( block.secondaryColor.toInt() )

        viewHolder.dateText.text = AppUtils.DateString(block.creationTime).dayMonthText()

        // Load member amount
        database.collection(Consts.DBPath.blockMembers).whereEqualTo("blockId", block.databaseId).count().get(AggregateSource.SERVER).addOnSuccessListener {
            viewHolder.membersText.text = it.count.toString()
        }

        // Load image to imageView using Glide
        // Fade in view after image is loaded
        val photoUrl = block.coverImageUrl
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
                    fadeInView(viewHolder.itemView, position)
                    return false
                }

            })
            .into(viewHolder.coverImagePreview)

        // Invoke listener when view is clicked, pass the current photo pair
        viewHolder.cardView.setOnClickListener {
            onItemClickListener?.invoke(block)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = blockList.size

    private fun fadeInView(view: View, position: Int){
        ObjectAnimator.ofFloat(view, "alpha", 1.0F).apply {
            duration = 300
            startDelay = (position*70).toLong()
            interpolator = DecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(view, "translationY", 0.0F).apply {
            duration = 300
            startDelay = (position*70).toLong()
            interpolator = DecelerateInterpolator()
            start()
        }
    }

}