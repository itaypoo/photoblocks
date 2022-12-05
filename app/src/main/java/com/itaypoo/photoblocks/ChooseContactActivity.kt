package com.itaypoo.photoblocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.adapters.UserContactAdapter
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
        generateContactUserPairList(contactsList)

    }

    private fun contactWhereNumberIs(list: MutableList<ContactModel>, num: String): ContactModel?{
        var res: ContactModel? = null
        for(contact in list){
            if(contact.phoneNumber == num) res = contact
        }
        return res
    }

    private fun generateContactUserPairList(contactList: MutableList<ContactModel>){
        // Get all users that have their phone number in contactList
        val resList: MutableList<Pair<ContactModel, User?>> = mutableListOf()

        database.collection("users").get().addOnFailureListener{

            // Getting users failed
            if(it is FirebaseNetworkException)
                Snackbar.make(this, binding.root, getString(R.string.generic_network_error), Snackbar.LENGTH_LONG).show()
            else Snackbar.make(this, binding.root, getString(R.string.generic_unknown_error), Snackbar.LENGTH_LONG).show()

        }.addOnSuccessListener {

            // Getting users complete

            val contactPhoneNumberList: MutableList<String> = mutableListOf()
            for(contact in contactList) contactPhoneNumberList.add(contact.phoneNumber)

            // Now loop through all users and add users that match a contact to the result list
            for(doc in it){
                val docUser: User = FirebaseUtils.ObjectFromDoc.User(doc)

                if(contactPhoneNumberList.contains(docUser.phoneNumber)){
                    // Add this contact with it's user to the result list, remove it from the numbers list
                    val contactModel: ContactModel = contactWhereNumberIs(contactList, docUser.phoneNumber)!!
                    val pair = Pair(contactModel, docUser)
                    contactPhoneNumberList.remove(docUser.phoneNumber)
                    resList.add(pair)

                    Log.d("Contact with a user", docUser.name)
                }
            }
            // All contacts with users have been added. Now add those without users.
            for(number in contactPhoneNumberList){
                val contactModel: ContactModel = contactWhereNumberIs(contactList, number)!!
                val pair: Pair<ContactModel, User?> = Pair(contactModel, null)
                resList.add(pair)

                Log.d("Contact with NO user", contactModel.displayName)
            }

            // Done getting list. Final step::
            setUpContactRecycler(resList)

        }

    }

    private fun setUpContactRecycler(contactUserPairs: MutableList<Pair<ContactModel, User?>>){
        // Now that we have the contact user list we can finally set up the recyclerView with our adapter.
        val adapter = UserContactAdapter(contactUserPairs, this)
        binding.contactUserRecycler.layoutManager = LinearLayoutManager(this)
        binding.contactUserRecycler.adapter = adapter
    }

}



