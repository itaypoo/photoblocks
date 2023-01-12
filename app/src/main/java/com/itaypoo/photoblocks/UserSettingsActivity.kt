package com.itaypoo.photoblocks

import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.ChangeImageTransform
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.CustomDialogMaker
import com.itaypoo.photoblocks.databinding.ActivityUserSettingsBinding
import com.itaypoo.photoblockslib.inputCheck
import java.io.File
import java.util.*

class UserSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserSettingsBinding

    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        setupTransitions()
        super.onCreate(savedInstanceState)
        binding = ActivityUserSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.firestore
        storageRef = FirebaseStorage.getInstance().reference

        binding.namePreview.text = AppUtils.currentUser?.name
        Glide.with(this).load(AppUtils.currentUser?.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(binding.profilePhotoPreviewImage)

        // Get user statistics
        getStats()

        val isP = AppUtils.currentUser!!.isPrivate
        if(!isP){
            binding.lockClosedIcon.visibility = View.INVISIBLE
            binding.lockOpenIcon.visibility = View.VISIBLE
            binding.privateButton.setBackgroundColor(getColor(R.color.tertiary))
        }
        else{
            binding.lockClosedIcon.visibility = View.VISIBLE
            binding.lockOpenIcon.visibility = View.INVISIBLE
            binding.privateButton.setBackgroundColor(getColor(R.color.error))
        }

        // Set on click listeners for buttons
        binding.cardChangeName.setOnClickListener { openChangeNameDialog() }
        binding.cardChangeImage.setOnClickListener { changeProfilePhoto() }
        binding.privateButton.setOnClickListener { togglePrivateMode() }
        binding.logOutButton.setOnClickListener {

            // Show confirmation dialog before logging out
            val d = CustomDialogMaker.makeYesNoDialog(
                this,
                getString(R.string.confirm_log_out),
                getString(R.string.confirm_log_out_desc),
                false,
                false,
                getString(R.string.log_out) // Change the "yes" button text to "log out"
            )
            d.noButton.setOnClickListener { d.dialog.dismiss() }
            d.yesButton.setOnClickListener { logOut() }
            d.dialog.show()

        }

        // back button
        binding.usetSettingsBackButton.setOnClickListener {
            finish()
        }
    }

    private fun togglePrivateMode() {
        var isP = AppUtils.currentUser!!.isPrivate

        val d = CustomDialogMaker.makeYesNoDialog(this,
        getString(R.string.change_private_title), getString(R.string.change_private_desc))
        d.dialog.show()

        d.noButton.setOnClickListener { d.dialog.dismiss() }
        d.yesButton.setOnClickListener {
            // Flip isPrivate value
            d.dialog.dismiss()
            isP = !isP

            // Play bounce anim
            val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_down)
            binding.privateCard.startAnimation(bounceAnim)

            database.collection(Consts.BDPath.users).document(AppUtils.currentUser!!.databaseId!!).update("isPrivate", isP).addOnSuccessListener {
                AppUtils.currentUser!!.isPrivate = isP
                // Update UI
                if(!isP){
                    binding.lockClosedIcon.visibility = View.INVISIBLE
                    binding.lockOpenIcon.visibility = View.VISIBLE
                    binding.privateButton.setBackgroundColor(getColor(R.color.tertiary))
                }
                else{
                    binding.lockClosedIcon.visibility = View.VISIBLE
                    binding.lockOpenIcon.visibility = View.INVISIBLE
                    binding.privateButton.setBackgroundColor(getColor(R.color.error))
                }
            }
        }
    }

    private fun setupTransitions(){
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            // set an exit transition
            enterTransition = ChangeImageTransform()
        }
    }

    private fun viewScaleAnimation(view: View, duration: Long){
        val interpolator = DecelerateInterpolator()
        // Animate a float from 0 to 1
        val scaleAnim = ValueAnimator.ofFloat(0.0F, 1.0F)
        scaleAnim.duration = duration
        scaleAnim.interpolator = interpolator

        // Change view scale.y to that float in layout, every tick of the value change
        scaleAnim.addUpdateListener {
            val animatedValue = scaleAnim.animatedValue as Float
            view.scaleY = animatedValue
        }

        // Start scale animation
        scaleAnim.start()
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun getStats(){
        // Init views
        binding.joinDateText.text = ""
        binding.blockCountText.text = ""
        binding.imageCountText.text = ""
        binding.commentCountText.text = ""

        // Show when the user joined the app
        val dateString = AppUtils.DateString(AppUtils.currentUser!!.creationTime)
        binding.joinDateText.text = buildString {
            append(getString(R.string.joined))
            append(" ")
            append(dateString.dayMonthText())
            append(", ")
            append(dateString.year)
        }

        // Count blocks the user is in
        val query = database.collection(Consts.BDPath.blockMembers).whereEqualTo("memberId", AppUtils.currentUser!!.databaseId).count().get(AggregateSource.SERVER)
        query.addOnSuccessListener {
            binding.blockCountText.text = it.count.toString() + " " + getString(R.string.stats_blocks_joined)
        }

        // Count posts uploaded
        val query2 = database.collection(Consts.BDPath.blockPosts).whereEqualTo("creatorId", AppUtils.currentUser!!.databaseId).count().get(AggregateSource.SERVER)
        query2.addOnSuccessListener {
            binding.imageCountText.text = it.count.toString() + " " + getString(R.string.images_uploaded)
        }

        // Count comments written
        val query3 = database.collection(Consts.BDPath.blockComments).whereEqualTo("authorId", AppUtils.currentUser!!.databaseId).count().get(AggregateSource.SERVER)
        query3.addOnSuccessListener {
            binding.commentCountText.text = it.count.toString() + " " + getString(R.string.comments_written)
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun openChangeNameDialog(){
        val d = CustomDialogMaker.makeTextInputDialog(
            this,
            getString(R.string.change_name),
            getString(R.string.new_name),
        )

        // Set cancel onclick and done onclick
        d.cancelButton.setOnClickListener {
            d.dialog.dismiss()
        }
        d.doneButton.setOnClickListener {
            // Check if inputted name is valid
            val text = d.editText.text.toString()
            val valid = inputCheck.validateUserName(text)

            d.hideError()
            when(valid){
                inputCheck.USER_NAME_TOO_SHORT -> {
                    // Name is too short
                    d.setError(getString(R.string.invalid_name_too_short))
                }
                inputCheck.USER_NAME_TOO_LONG -> {
                    // Name is too long
                    d.setError(getString(R.string.invalid_name_too_long))
                }
                inputCheck.USER_NAME_VALID -> {
                    // Name is valid
                    changeUserName(text)
                    d.dialog.dismiss()
                }
            }
        }

        d.dialog.show()
    }

    private fun changeUserName(newName: String){
        if(AppUtils.currentUser != null && AppUtils.currentUser!!.databaseId != null){
            database.collection(Consts.BDPath.users).document(AppUtils.currentUser!!.databaseId!!).update("name", newName).addOnSuccessListener {
                // Name update success, update UI
                AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.name_changed_alert))
                AppUtils.currentUser!!.name = newName
                binding.namePreview.text = newName
            }.addOnFailureListener {
                // Name update failed
                if(it is FirebaseNetworkException){
                    AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.name_change_failed_network_error))
                }
                else{
                    AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.name_change_failed_unknown))
                }
            }
        }
        else{
            // No logged in user?? Error
            AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.unexpected_error_relog))
        }
    }


    private fun changeProfilePhoto(){
        if(AppUtils.currentUser != null && AppUtils.currentUser!!.databaseId != null){
            val cropIntent = Intent(this, ImageCropActivity::class.java)
            cropIntent.putExtra(Consts.Extras.CROP_INPUT_RATIO, Consts.Extras.RATIO_ONE_TO_ONE)
            startActivityForResult(cropIntent, Consts.RequestCode.CROP_IMAGE_ACTIVITY)
        }
        else{
            // No logged in user?? Error
            AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.unexpected_error_relog))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == Consts.RequestCode.CROP_IMAGE_ACTIVITY && resultCode == RESULT_OK && data != null){
            // User chose an image and cropped it. Now get the image and update upload it to storage.
            val path = data.getStringExtra(Consts.Extras.CROP_OUTPUT_CROPPEDFILENAME).toString()
            AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.uploading_image))

            // Delete previous profile photo from storage
            // Current user cannot be null - checked before
            // IMPORTENT - do not delete old photo if it is the default one!
            // default photo is only stored once in the database thus must not be deleted
            val oldRef = Firebase.storage.getReferenceFromUrl(AppUtils.currentUser!!.profilePhotoUrl)
            if((AppUtils.currentUser!!.profilePhotoUrl) != Consts.Defaults.USER_PFP_URL){
                oldRef.delete()
            }

            // Upload new profile photo under a generated UUID
            val absPath = getFileStreamPath(path).absolutePath
            val file = File(absPath)
            val uri = Uri.fromFile(file)
            val uuid = UUID.randomUUID().toString()
            val uploadTask = storageRef.child("userProfileImages/$uuid").putFile(uri)

            uploadTask.addOnFailureListener{
                // Uploading image failed, reset chosen image
                AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.uploading_image_failed))

            }.addOnSuccessListener {
                // Uploading image success, get its url, finish uploading room data to firestore
                storageRef.child("userProfileImages/$uuid").downloadUrl.
                addOnSuccessListener {
                    // Save new image url and update UI
                    AppUtils.currentUser?.profilePhotoUrl = it.toString()
                    val bitmap = AppUtils.getBitmapFromPrivateInternal(path, this)
                    binding.profilePhotoPreviewImage.setImageBitmap(bitmap)
                    AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.pfp_changed_alert))

                    // Update firestore user with the new image url
                    database.collection(Consts.BDPath.users).document(AppUtils.currentUser!!.databaseId!!).update("profilePhotoUrl", it.toString())
                }.
                addOnFailureListener{
                    // Unknown error when getting photo url
                    AppUtils.makeCancelableSnackbar(binding.root, getString(R.string.unexpected_error))
                }
            }
        }
    }

    private fun logOut(){
        // Delete user data from AppUtils and SharedPreferences
        AppUtils.currentUser = null

        val sharedPref: SharedPreferences = getSharedPreferences(Consts.SharedPrefs.PATH, MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove(Consts.SharedPrefs.SAVED_USER_ID_KEY)
        editor.apply()

        // Go to splash screen
        startActivity(Intent(this, SplashActivity::class.java))
    }

}