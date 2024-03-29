package com.itaypoo.photoblocks

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivitySplashBinding


class SplashActivity : AppCompatActivity() {
    val DEBUG_REMOVE_USER_ON_STARTUP = false

    private lateinit var binding: ActivitySplashBinding
    private lateinit var nextIntent: Intent

    private var queriesDone = false
    private var animationDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set device background colors to fit the UI
        window.statusBarColor = getColor(R.color.primary)
        window.navigationBarColor = getColor(R.color.primary)

        nextIntent = Intent(this, PhoneAuthActivity::class.java)

        val sharedPref: SharedPreferences = getSharedPreferences(Consts.SharedPrefs.PATH, MODE_PRIVATE)

        ////// Remove saved user -- TRUE FOR DEBUGGING PURPOSES ONLY //////
        if(DEBUG_REMOVE_USER_ON_STARTUP){
            val editor = sharedPref.edit()
            editor.remove(Consts.SharedPrefs.SAVED_USER_ID_KEY)
            editor.apply()
        }

        // Check if user login is saved on device
        val savedUserId = sharedPref.getString(Consts.SharedPrefs.SAVED_USER_ID_KEY,  null)

        if(savedUserId != null){
            // Auto login saved user
            val database = Firebase.firestore
            database.collection(Consts.DBPath.users).document(savedUserId).get().addOnSuccessListener {
                // Getting user success
                val user = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                AppUtils.currentUser = user

                nextIntent = Intent(this, HomeScreenActivity::class.java)
                queriesDone = true
                startNextActivity()
            }
                .addOnFailureListener{
                // Getting user failed
                if(it is FirebaseNetworkException){
                    Snackbar.make(binding.titleText, "No internet connection. Please try again later", Snackbar.LENGTH_LONG).show()
                }
                else{
                    queriesDone = true
                    startNextActivity()
                }
            }
        }
        else{
            queriesDone = true
            startNextActivity()
        }

        // Start title text bounce anim
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.splash_screen_bounce)
        binding.titleText.startAnimation(bounceAnim)

        // Set animation end listener
        bounceAnim.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(arg0: Animation) {}
            override fun onAnimationRepeat(arg0: Animation) {}
            override fun onAnimationEnd(arg0: Animation) {
                animationDone = true
                startNextActivity()
            }
        })

        // done
    }

    private fun wl(text: String){
        Log.e("WriteLine", text)
    }

    private fun startNextActivity(){
        // Start next activity only if animations AND queries are done.
        if(queriesDone && animationDone) { startActivity(nextIntent) }
    }

    override fun onBackPressed() {
        // Disable back button
    }
}