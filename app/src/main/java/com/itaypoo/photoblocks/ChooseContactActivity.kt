package com.itaypoo.photoblocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.helpers.ContactModel
import com.itaypoo.helpers.ContactsUtils
import com.itaypoo.photoblocks.databinding.ActivityChooseContactBinding
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ChooseContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseContactBinding

    private lateinit var database: FirebaseFirestore

    private lateinit var contactsList: MutableList<ContactModel>
    private lateinit var contactsThatHaveUser: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.firestore

        contactsList = ContactsUtils.getList(this.contentResolver)
        contactsThatHaveUser = getContactsThatHaveUser(contactsList)


    }

    private fun getContactsThatHaveUser(contactsList: MutableList<ContactModel>): MutableList<String> {
        val res = mutableListOf<String>()

        for(contact in contactsList){
            // Loop through all contacts and separate the ones that have a user in the database
            database.collection("users").whereEqualTo("phoneNumber", contact.phoneNumber).get()
        }

        return res
    }

}



