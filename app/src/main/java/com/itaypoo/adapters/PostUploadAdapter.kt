package com.itaypoo.adapters

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.CustomDialogMaker
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.BlockPost
import com.itaypoo.photoblockslib.inputCheck
import kotlin.coroutines.coroutineContext


class PostUploadAdapter(private val imageUriList: MutableList<Uri>, private val context: Context) :
    RecyclerView.Adapter<PostUploadAdapter.ViewHolder>() {

    private lateinit var profilePictureBitmap: Bitmap
    val uriStringList = mutableListOf<UriString>()

    data class UriString(val uri: Uri, var string: String)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        // Init a null description for every post
        for(i in 0 until imageUriList.size){
            uriStringList.add(UriString(imageUriList[i], ""))
        }

        // Load user profile picture to be used in all viewHolders
        Glide.with(context)
            .asBitmap().load(AppUtils.currentUser!!.profilePhotoUrl).placeholder(R.drawable.default_profile_photo)
            .listener(object : RequestListener<Bitmap>{
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean
                ): Boolean {return false}

                override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean {
                    profilePictureBitmap = resource!!
                    return false
                }

            }).submit()
    }

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val descriptionText: TextView
        val profilePicture: ImageView
        val imagePreview: ImageView
        val renameButton: Button

        init {
            // Define click listener for the ViewHolder's View.
            descriptionText = view.findViewById(R.id.uploadPostItem_descriptionText)
            profilePicture = view.findViewById(R.id.uploadPostItem_profilePicture)
            imagePreview = view.findViewById(R.id.uploadPostItem_previewImage)
            renameButton = view.findViewById(R.id.uploadPostItem_renameButton)

        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_item_post_upload, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        var us = uriStringList[position]

        viewHolder.imagePreview.setImageURI(us.uri)
        viewHolder.profilePicture.setImageBitmap(profilePictureBitmap)
        viewHolder.descriptionText.text = us.string

        viewHolder.renameButton.setOnClickListener {
            // Open change text dialog
            val d = CustomDialogMaker.makeTextInputDialog(
                context, context.getString(R.string.edit_description), context.getString(R.string.post_decription_hint),
                false, true, null, null,
                us.string
            )
            d.dialog.show()
            d.cancelButton.setOnClickListener { d.dialog.dismiss() }
            d.doneButton.setOnClickListener {
                d.dialog.dismiss()
                uriStringList[position].string = d.editText.text.toString()
                us = uriStringList[position]
                viewHolder.descriptionText.text = us.string
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = imageUriList.size

    // Return each image Uri with its inputted description text
    fun getImageTextList(): MutableList<Pair<Uri, String>> {
        val res = mutableListOf<Pair<Uri, String>>()
        for(us in uriStringList){
            res.add(Pair(us.uri, us.string))
        }
        return res
    }

}