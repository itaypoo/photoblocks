package com.itaypoo.photoblocks

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivitySetupUserBinding
import com.itaypoo.photoblockslib.User
import com.itaypoo.photoblockslib.inputCheck
import java.io.File
import java.util.*


class SetupUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupUserBinding
    
    private lateinit var storageRef: StorageReference
    private lateinit var database: FirebaseFirestore

    private var pfpInternalPath: String? = null
    private lateinit var phoneNumber: String
    private lateinit var user: User

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get firebase references
        storageRef = FirebaseStorage.getInstance().reference
        database = Firebase.firestore
        
        // Get passed phone number
        if(intent.getStringExtra("phoneNumber") == null){
            Toast.makeText(this, "Error getting number, please try again (No extra passed)", Toast.LENGTH_SHORT).show()
            finish()
        }
        phoneNumber = intent.getStringExtra("phoneNumber").toString()
        binding.phoneNumberPreview.text = phoneNumber

        // Select an image from the gallery and crop it via ImageCropActivity
        binding.uploadPhotoButton.setOnClickListener {
            val cropIntent = Intent(this, ImageCropActivity::class.java)
            cropIntent.putExtra(Consts.Extras.CROP_INPUT_RATIO, Consts.Extras.RATIO_ONE_TO_ONE)
            startActivityForResult(cropIntent, Consts.RequestCode.CROP_IMAGE_ACTIVITY)
        }

        // Init user object
        user = FirebaseUtils.DefaultObjects.User(phoneNumber)
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data != null && resultCode == RESULT_OK && requestCode == Consts.RequestCode.CROP_IMAGE_ACTIVITY){
            // Get cropped image from temp storage and display it
            val path = data.getStringExtra(Consts.Extras.CROP_OUTPUT_CROPPEDFILENAME)!!
            pfpInternalPath = path
            val bitmap = AppUtils.getBitmapFromPrivateInternal(path, this)
            binding.profilePhotoPreview.setImageBitmap(bitmap)
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    fun onCompleteButtonPressed(view: View){
        binding.invalidNameError.visibility = View.GONE

        user.name = binding.userNameEditText.text.toString()

        // Check if user name is valid
        val validRes = inputCheck.checkUserName(user.name)

        if(validRes == inputCheck.USER_NAME_VALID){
            // Valid name. Upload user to firebase

            // Disable button for duplicate requests
            binding.completeButton.isEnabled = false
            binding.loadingCircle.visibility = View.VISIBLE

            // If user uploaded a pfp from gallery, upload it and continue
            if(pfpInternalPath != null){
                // Upload file under a generated UUID
                val path = getFileStreamPath(pfpInternalPath).absolutePath
                val file = File(path)
                val uri = Uri.fromFile(file)
                val uuid = UUID.randomUUID().toString()
                val uploadTask = storageRef.child("userProfileImages/$uuid").putFile(uri)

                uploadTask.addOnFailureListener{
                    // Uploading image failed, reset chosen image
                    enableButtonWithMessage("Uploading photo falied, please try again.")

                    binding.profilePhotoPreview.setImageResource(R.drawable.default_profile_photo)
                    pfpInternalPath = null

                }.addOnSuccessListener {
                    // Uploading image success, get its url, finish uploading room data to firestore
                    storageRef.child("userProfileImages/$uuid").downloadUrl.addOnSuccessListener {
                        user.profilePhotoUrl = it.toString()
                        uploadUserToFirestore()
                    }.addOnFailureListener{
                        // Unknown error when getting photo url
                        enableButtonWithMessage("Something went wrong, please try again.")
                    }
                }
            }

            // If user did not upload a pfp from gallery, continue
            else{ uploadUserToFirestore() }

        }
        else if(validRes == inputCheck.USER_NAME_TOO_SHORT){
            binding.invalidNameError.visibility = View.VISIBLE
            binding.invalidNameError.text = getString(R.string.invalid_name_too_short)
        }
        else if(validRes == inputCheck.USER_NAME_TOO_LONG){
            binding.invalidNameError.visibility = View.VISIBLE
            binding.invalidNameError.text = getString(R.string.invalid_name_too_long)
        }
    }

    private fun uploadUserToFirestore() {
        val userHashMap = user.toHashMap()
        database.collection("users").add(userHashMap).addOnFailureListener{
            // Uploading user failed. Check if it is caused by user not having an internet connection
            if(it is FirebaseNetworkException){
                enableButtonWithMessage("Uploading failed. Please check your internet connection.")
            }
            else{
                enableButtonWithMessage("Uploading falied, please try again.")
            }
        }.addOnSuccessListener {
            // Uploading user success
            // Save user id in local for auto log in
            val sharedPref: SharedPreferences = getSharedPreferences(Consts.SharedPrefs.PATH, MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString(Consts.SharedPrefs.SAVED_USER_ID_KEY, it.id)
            editor.apply()

            // Save user for later use
            user.databaseId = it.id
            AppUtils.currentUser = user

            // Go to home page, tell it a new user logged in
            val homeIntent = Intent(this, HomeScreenActivity::class.java)
            homeIntent.putExtra(Consts.Extras.SIGNIN_TYPE, Consts.LoginType.NEW_USER)
            startActivity(homeIntent)
        }
    }

    private fun enableButtonWithMessage(text: String){
        binding.completeButton.isEnabled = true
        binding.loadingCircle.visibility = View.INVISIBLE

        val msg = Snackbar.make(binding.completeButton, text, Snackbar.LENGTH_SHORT)
        msg.anchorView = binding.completeButton
        msg.show()
    }
}