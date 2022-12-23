package com.itaypoo.photoblocks

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.ContactsUtils
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityPhoneAuthBinding
import com.itaypoo.photoblockslib.User
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneAuthBinding

    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var credential: PhoneAuthCredential? = null
    private lateinit var auth : FirebaseAuth
    private lateinit var phoneNumber: String
    private lateinit var database: FirebaseFirestore

    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set views visibility
        binding.invalidNumberText.visibility = View.GONE
        binding.invalidCodeText.visibility = View.GONE

        binding.phoneNumberGroup.visibility = View.VISIBLE
        binding.codeGroup.visibility = View.INVISIBLE

        // Get firebase stuff
        auth = Firebase.auth
        database = FirebaseFirestore.getInstance()

        // Create verification callbacks
        createCallbacks()

        // On continue button click (enter phone number)
        binding.continueButton.setOnClickListener{
            // Check input phone number
            var num = binding.phoneNumberEditText.text.toString()
            if(num.isNotEmpty() && num.isNotBlank() && ContactsUtils.validatedPhoneNumber(num) != null){
                // Standardize the phone number format (remove dashes, spaces...)
                num = ContactsUtils.validatedPhoneNumber(num)!!
                // Valid phone number, send verification code
                binding.invalidNumberText.visibility = View.GONE
                phoneNumber = num
                startPhoneVerification()

                // Disable continue button for duplicate requests
                binding.continueButton.isEnabled = false
                binding.loadingCircle.visibility = View.VISIBLE
            }
            else{
                // Invalid phone number entered, force focus number editText
                binding.invalidNumberText.visibility = View.VISIBLE
                binding.invalidNumberText.text = getString(R.string.invalid_phone_number)
                binding.phoneNumberEditText.requestFocus()
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.phoneNumberEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        // On submit code button click (after code sent)
        binding.submitCodeButton.setOnClickListener{
            // Check input code
            val code = binding.codeEditText.text.toString()
            if(code.isNotEmpty() && code.isNotBlank()){
                // Code entered, validate it
                binding.invalidCodeText.visibility = View.GONE
                val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
                signInWithPhoneAuthCredential(credential)

                // Disable submit button for duplicate request
                binding.submitCodeButton.isEnabled = false
                binding.loadingCircle.visibility = View.VISIBLE
            }
            else{
                // Invalid code entered, force focus number editText
                binding.invalidCodeText.visibility = View.VISIBLE
                binding.codeEditText.requestFocus()
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.codeEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun startPhoneVerification(){
        // Create verification options
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)              // Phone number to verify,
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit,
            .setActivity(this)                        // Activity (for callback binding),
            .setCallbacks(callbacks)                  // Callbacks for status updates
            .build()
        // Send verification code
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private fun createCallbacks() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Called when authentication is auto completed (for ex. Via google play)
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // Called on invalid request (for ex. incorrect phone number format)
                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid phone number, let user enter another one
                    val msg = Snackbar.make(binding.root, "Invalid phone number entered, Please try again.", Snackbar.LENGTH_SHORT)
                    msg.setAnchorView(binding.continueButton)
                    msg.show()

                    binding.continueButton.isEnabled = true
                    binding.loadingCircle.visibility = View.INVISIBLE
                }
                else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.d(ContentValues.TAG, "FIREBASE: The SMS quota for the project has been exceeded!")
                    val msg = Snackbar.make(binding.root, "Server error occurred, please try again later", Snackbar.LENGTH_SHORT)
                    msg.setAnchorView(binding.continueButton)
                    msg.show()
                }

            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken
            ) {
                // SMS verification code has been sent to the provided phone number, save verification ID and resending token
                storedVerificationId = verificationId
                resendToken = token

                // Show code entering screen, hide this screen
                binding.phoneNumberGroup.visibility = View.GONE
                binding.codeGroup.visibility = View.VISIBLE
                binding.loadingCircle.visibility = View.INVISIBLE
            }
        }
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    signInComplete()
                }
                else {
                    // Sign in failed
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // incorrect code entered, let user enter another one
                        val msg = Snackbar.make(binding.root, "Incorrect code entered", Snackbar.LENGTH_SHORT)
                        msg.setAnchorView(binding.submitCodeButton)
                        msg.show()

                        binding.codeEditText.requestFocus()
                        val imm: InputMethodManager =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.codeEditText, InputMethodManager.SHOW_IMPLICIT)

                        binding.submitCodeButton.isEnabled = true
                        binding.loadingCircle.visibility = View.INVISIBLE
                    }
                    else{
                        val msg = Snackbar.make(binding.root, "Authentication failed, please try again", Snackbar.LENGTH_SHORT)
                        msg.setAnchorView(binding.submitCodeButton)
                        msg.show()

                        binding.submitCodeButton.isEnabled = true
                        binding.loadingCircle.visibility = View.INVISIBLE
                    }

                }
            }
    }

    private fun signInComplete() {
        // Find if a user with that phone number exists. if it does - login, if it does not - go to user creation screen
        database.collection("users").whereEqualTo("phoneNumber", phoneNumber).get().addOnSuccessListener {
            if(it.isEmpty){
                // A user with this phone number does not exist
                val setupIntent = Intent(this, SetupUserActivity::class.java)
                setupIntent.putExtra("phoneNumber", phoneNumber)
                startActivity(setupIntent)
            }
            else{
                // A user with this phone number exists, load it's data and log in
                val doc = it.documents[0]
                val user = FirebaseUtils.ObjectFromDoc.User(doc, contentResolver)
                AppUtils.currentUser = user

                // Save user id in local for auto log in
                val sharedPref: SharedPreferences = getSharedPreferences(Consts.SharedPrefs.PATH, MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString(Consts.SharedPrefs.SAVED_USER_ID_KEY, user.databaseId)
                editor.apply()

                // Go to home page, tell it an existing user logged in
                val homeIntent = Intent(this, HomeScreenActivity::class.java)
                homeIntent.putExtra(Consts.Extras.SIGNIN_TYPE, Consts.LoginType.EXISTING_USER)
                startActivity(homeIntent)
            }
        }.addOnFailureListener{
            // Failed getting results
            if(it is FirebaseNetworkException) {
                // Network error
                val msg = Snackbar.make(binding.root, "No internet connection, please try again later.", Snackbar.LENGTH_SHORT)
                msg.setAnchorView(binding.submitCodeButton)
                msg.show()
                binding.submitCodeButton.isEnabled = true
                binding.loadingCircle.visibility = View.INVISIBLE
            }
            else{
                // Unexpected error
                val msg = Snackbar.make(binding.root, "Unexpected error, please try again later.", Snackbar.LENGTH_SHORT)
                msg.setAnchorView(binding.submitCodeButton)
                msg.show()
                binding.submitCodeButton.isEnabled = true
                binding.loadingCircle.visibility = View.INVISIBLE
            }
        }

    }

}