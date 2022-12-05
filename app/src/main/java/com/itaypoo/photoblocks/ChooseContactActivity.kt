package com.itaypoo.photoblocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.helpers.ContactModel
import com.itaypoo.helpers.ContactsUtils
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.databinding.ActivityChooseContactBinding
import com.itaypoo.photoblockslib.User
import kotlinx.coroutines.*

class ChooseContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseContactBinding

    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.firestore

        // Get the contacts list
        val contactsList = ContactsUtils.getList(this.contentResolver)

        // First, get all users so we can match them with the contacts list
        val contactUserList = getContactsThatHaveUser(contactsList)

    }

    private fun getContactsThatHaveUser(contactList: MutableList<ContactModel>): MutableList<User>{
        // Get all users that have their phone number in contactList
        val resList: MutableList<User> = mutableListOf()

        database.collection("users").get().addOnFailureListener{

            // Getting users failed
            if(it is FirebaseNetworkException)
                Snackbar.make(this, binding.root, getString(R.string.generic_network_error), Snackbar.LENGTH_LONG).show()
            else Snackbar.make(this, binding.root, getString(R.string.generic_unknown_error), Snackbar.LENGTH_LONG).show()

        }.addOnSuccessListener {

            // Getting users complete

            val contactPhoneNumberList: MutableList<String> = mutableListOf()
            for(contact in contactList) contactPhoneNumberList.add(contact.phoneNumber)

            // Now loop through all users and add users that match a contact to contactsThatHaveUser
            for(doc in it){
                val docUser: User = FirebaseUtils.ObjectFromDoc.User(doc)
                if(contactPhoneNumberList.contains(docUser.phoneNumber)){
                    resList.add(docUser)
                    Log.d("USER CONTACT", docUser.name)
                }
            }

        }

        return resList
    }

}



