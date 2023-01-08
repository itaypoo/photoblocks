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

    var onRemoveButtonClicked: ((Uri) -> Unit)? = null

    val uriStringList = mutableListOf<UriString>()

    data class UriString(val uri: Uri, var string: String)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        // Init a null description for every post
        for(i in 0 until imageUriList.size){
            uriStringList.add(UriString(imageUriList[i], ""))
        }
    }

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val descriptionText: TextView
        val profilePicture: ImageView
        val imagePreview: ImageView
        val removeButton: Button

        init {
            // Define click listener for the ViewHolder's View.
            descriptionText = view.findViewById(R.id.uploadPostItem_descriptionText)
            profilePicture = view.findViewById(R.id.uploadPostItem_profilePicture)
            imagePreview = view.findViewById(R.id.uploadPostItem_previewImage)
            removeButton = view.findViewById(R.id.uploadPostItem_removeButton)
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
        setDescriptionText(viewHolder, us.string)
        Glide.with(context).load(AppUtils.currentUser!!.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(viewHolder.profilePicture)

        viewHolder.descriptionText.setOnClickListener {
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
                setDescriptionText(viewHolder, us.string)
            }
        }

        viewHolder.removeButton.setOnClickListener {
            onRemoveButtonClicked?.invoke(us.uri)

            val index = uriStringList.indexOf(us)
            uriStringList.remove(us)
            notifyItemRemoved(index)
        }
    }

    private fun setDescriptionText(holder: ViewHolder, text: String){
        if(text == ""){
            holder.descriptionText.setTextColor(context.getColor(R.color.on_background_variant))
            holder.descriptionText.text = context.getString(R.string.post_decription_hint)
        }
        else{
            holder.descriptionText.setTextColor(context.getColor(R.color.on_background))
            holder.descriptionText.text = text
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