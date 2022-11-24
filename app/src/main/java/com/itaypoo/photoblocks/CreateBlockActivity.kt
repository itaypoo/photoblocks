package com.itaypoo.photoblocks

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.Explode
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.photoblocks.databinding.ActivityCreateBlockBinding
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.inputCheck
import java.io.File
import java.util.*


class CreateBlockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBlockBinding

    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private var uploadImageFileName: String? = null
    private lateinit var newBlock: Block

    override fun onCreate(savedInstanceState: Bundle?) {
        setupTransitions()
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init firebase connections
        storageRef = FirebaseStorage.getInstance().reference
        database = Firebase.firestore

        // Current user must exist to continue (Should always be true)
        if(AppUtils.currentUser == null || AppUtils.currentUser?.databaseId == null){
            Toast.makeText(this, "error getting user data.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // OnClick listeners
        binding.blockChangePhotoButton.setOnClickListener {
            showPhotoDialog()
        }
        binding.openBlockButton.setOnClickListener {
            prepareBlock()
        }

        // Init block
        newBlock = Block(
            null,
            getString(R.string.new_block),
            AppUtils.currentUser!!.databaseId!!,
            Consts.Defaults.BLOCK_COVER_IMAGE_URL,
            69,                               // TEMPORARY
            69,                            // TEMPORARY
            69,                            // TEMPORARY
            listOf(AppUtils.currentUser!!.databaseId!!)
        )

    }

    private fun setupTransitions(){
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            // set an exit transition
            enterTransition = Explode()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun showPhotoDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_choose_photo_from)

        // Set dialog window width, height, background and position
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        // Get dialog views
        val cancelButton = dialog.findViewById<Button>(R.id.photoDialog_cancelButton)
        val galleryButton = dialog.findViewById<Button>(R.id.photoDialog_galleryButton)
        val curatedButton = dialog.findViewById<Button>(R.id.photoDialog_curatedButton)

        // Set button click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        curatedButton.setOnClickListener {
            dialog.dismiss()
            val curatedIntent = Intent(this, CuratedPhotosActivity::class.java)
            startActivityForResult(curatedIntent, Consts.RequestCode.CURATED_PHOTOS_ACTIVITY)
        }
        galleryButton.setOnClickListener {
            dialog.dismiss()
            val cropIntent = Intent(this, ImageCropActivity::class.java)
            cropIntent.putExtra(Consts.Extras.CROP_INPUT_RATIO, Consts.Extras.RATIO_BLOCK_COVER)
            startActivityForResult(cropIntent, Consts.RequestCode.CROP_IMAGE_ACTIVITY)
        }

        dialog.show()
    }

    private fun showLoadingDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_loading)

        // Set dialog window width, height, background and position
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        // Get dialog views
        val titleText = dialog.findViewById<TextView>(R.id.dialogLoading_text)

        // Init views
        titleText.text = getString(R.string.creating_block)

        dialog.setCancelable(false)
        dialog.show()
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == Consts.RequestCode.CURATED_PHOTOS_ACTIVITY){
            //
            // Curated photo chosen
            //
            if(resultCode == RESULT_OK && data != null){
                val photoId : String = data.getStringExtra(Consts.Extras.CURATED_OUTPUT_DATABASEID)!!

                // Get the photo from database and its bitmap
                database.collection("curatedPhotos").document(photoId).get().addOnSuccessListener {
                    val photoUrl = it.get("photoUrl") as String
                    newBlock.coverImageUrl = photoUrl
                    // Change photos downloaded times in database
                    val newDownloads: Long = (it.get("downloads") as Long) + 1
                    database.collection("curatedPhotos").document(photoId).update("downloads", newDownloads)

                    // Get photo bitmap
                    Glide.with(this@CreateBlockActivity).asBitmap().load(photoUrl).into(object: CustomTarget<Bitmap>(){
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            // Got image bitmap. Now use it to update the cover image.
                            updateCoverImage(resource)

                            // Since the image is already stored in the database,
                            // there is no need to upload it.
                            uploadImageFileName = null
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}

                    })
                }

            }
        }
        else{
            //
            // Gallery photo chosen
            //
            if(resultCode == RESULT_OK && data != null){
                // Save filename for later uploading to database
                val path = data.getStringExtra(Consts.Extras.CROP_OUTPUT_CROPPEDFILENAME)!!
                uploadImageFileName = path

                // Update UI
                val bitmap = AppUtils.getBitmapFromPrivateInternal(path, this)
                updateCoverImage(bitmap)
            }
        }

    }

    private fun updateCoverImage(bitmap: Bitmap) {
        // Generate a color palette that matches the selected picture
        val palette = Palette.from(bitmap).generate()

        if(palette.lightVibrantSwatch == null) { newBlock.primaryColor = Color.parseColor(Consts.Defaults.BLOCK_PRIMARY_COLOR) }
        else { newBlock.primaryColor = palette.lightVibrantSwatch!!.rgb }

        if(palette.dominantSwatch == null) { newBlock.secondaryColor = Color.parseColor(Consts.Defaults.BLOCK_SECONDARY_COLOR) }
        else { newBlock.secondaryColor = palette.dominantSwatch!!.rgb }

        // Update UI
        binding.blockPreviewImage.setImageBitmap(bitmap)
        binding.blockColorGradient.imageTintList = ColorStateList.valueOf(newBlock.secondaryColor as Int)
        binding.blockChangePhotoButton.backgroundTintList = ColorStateList.valueOf(newBlock.primaryColor as Int)

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun prepareBlock(){
        // Prepare the block for uploading it to the database.
        // Make sure it has all values set, and its cover image is in the database.

        val chosenName = binding.blockTitleEditText.text.toString()
        val nameValidation = inputCheck.checkBlockName(chosenName)

        binding.blockNameError.visibility = View.GONE

        if(nameValidation == inputCheck.BLOCK_NAME_TOO_SHORT){
            // Name too short
            binding.blockNameError.visibility = View.VISIBLE
            binding.blockNameError.text = getString(R.string.invalid_block_name_too_short)
            // Focus edit text and open keyboard
            binding.blockTitleEditText.requestFocus()
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.blockTitleEditText, InputMethodManager.SHOW_IMPLICIT)
        }
        else if(nameValidation == inputCheck.BLOCK_NAME_TOO_LONG){
            // Name too long
            binding.blockNameError.visibility = View.VISIBLE
            binding.blockNameError.text = getString(R.string.invalid_block_name_too_long)
            // Focus edit text and open keyboard
            binding.blockTitleEditText.requestFocus()
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.blockTitleEditText, InputMethodManager.SHOW_IMPLICIT)
        }
        else if(nameValidation == inputCheck.BLOCK_NAME_VALID){
            // Valid block name inputted
            newBlock.title = binding.blockTitleEditText.text.toString()

            // Open loading dialog
            showLoadingDialog()

            // Check if cover image is needed to be uploaded to database
            if(uploadImageFileName != null){
                // Upload cover image to database
                val absPath = getFileStreamPath(uploadImageFileName).absolutePath
                val file = File(absPath)
                val uri = Uri.fromFile(file)
                val uuid = UUID.randomUUID().toString()
                val uploadTask = storageRef.child("blockCoverImage/$uuid").putFile(uri)
                uploadTask.addOnSuccessListener {
                    // uploading image complete
                    // Get the images URL
                    storageRef.child("blockCoverImage/$uuid").downloadUrl.addOnSuccessListener {
                        newBlock.coverImageUrl = it.toString()
                        // Block is ready for upload
                        uploadBlock()
                    }.addOnFailureListener {
                        Snackbar.make(this, binding.root, getString(R.string.block_upload_failed), Snackbar.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    // uploading image failed
                    if(it is FirebaseNetworkException){
                        Snackbar.make(this, binding.root, getString(R.string.block_upload_failed_no_connection), Snackbar.LENGTH_SHORT).show()
                    }
                    else{
                        Snackbar.make(this, binding.root, getString(R.string.block_upload_failed), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            else{
                // No need to upload cover image (It is already stored in database)
                // Now we can upload the block.
                uploadBlock()
            }

        }

    }

    private fun uploadBlock(){
        // Block is ready for upload it, lets do it!
        database.collection("blocks").add(newBlock.toHashMap()).addOnFailureListener {
            // Block failed uploading
            if(it is FirebaseNetworkException){
                Snackbar.make(this, binding.root, getString(R.string.block_upload_failed_no_connection), Snackbar.LENGTH_SHORT).show()
            }
            else{
                Snackbar.make(this, binding.root, getString(R.string.block_upload_failed), Snackbar.LENGTH_SHORT).show()
            }
        }.addOnSuccessListener {
            // Uploading block success!
            Toast.makeText(this, getString(R.string.block_created_message), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

}