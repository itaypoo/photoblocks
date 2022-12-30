package com.itaypoo.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblocks.UploadPostActivity
import com.itaypoo.photoblockslib.inputCheck

class PostUploadAdapter(private val imageUriList: MutableList<Uri>, private val context: Context) :
    RecyclerView.Adapter<PostUploadAdapter.ViewHolder>() {

    val viewHolderUriMap = hashMapOf<Uri, ViewHolder?>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        for(uri in imageUriList){
            viewHolderUriMap[uri] = null
        }
    }

    // Class for a viewHolder in the recyclerView
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagePreview: ImageView
        val desctiptionEditText: EditText
        val errorText: TextView

        init {
            // Define click listener for the ViewHolder's View.
            imagePreview = view.findViewById(R.id.uploadPostItem_previewImage)
            desctiptionEditText = view.findViewById(R.id.uploadPostItem_descEditText)
            errorText = view.findViewById(R.id.uploadPostItem_errorText)
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
        val uri = imageUriList[position]
        viewHolder.imagePreview.setImageURI(uri)
        viewHolderUriMap[uri] = viewHolder
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = imageUriList.size

    // Get each image Uri with its inputted description text
    fun getImagesAndDescriptions(): MutableList<UploadPostActivity.UriStringPair> {
        val resPairList = mutableListOf<UploadPostActivity.UriStringPair>()

        for(kv in viewHolderUriMap){
            // Loop through all of the items in the recycler
            val text: String
            if(kv.value == null) text = ""
            else text = kv.value!!.desctiptionEditText.text.toString()
            val res = inputCheck.validatePostDescription(text)

            if(res == inputCheck.POST_DESCRIPTION_TOO_LONG){
                // Text is too long
                kv.value?.errorText?.visibility = View.VISIBLE
                kv.value?.errorText?.text = context.getString(R.string.post_decription_too_long)
            }
            else if(res == inputCheck.POST_DESCRIPTION_VALID){
                // Text is valid
                // Add this to the list
                val uri = kv.key
                resPairList.add(UploadPostActivity.UriStringPair(uri, text))
            }
        }

        return resPairList
    }

}