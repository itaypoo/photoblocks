package com.itaypoo.photoblocks

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.adapters.UserContactAdapter
import com.itaypoo.helpers.*
import com.itaypoo.photoblocks.databinding.ActivityChooseContactBinding
import com.itaypoo.photoblockslib.User

class ChooseContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseContactBinding

    private lateinit var database: FirebaseFirestore
    private var resIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.firestore

        // Get permission from the user to read phone contacts
        requestPermissions(arrayOf("android.permission.READ_CONTACTS"), 80)

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 80){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                permissionGranted()
            }
            if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                permissionDenied()
            }
        }
    }

    private fun permissionGranted(){
        // We have permission! Start the process.
        val contactsList = ContactsUtils.getList(contentResolver)
        generateContactUserPairList(contactsList)
    }

    private fun permissionDenied(){
        // Create a message dialog telling the user that permission is needed
        val d = CustomDialogMaker.makeYesNoDialog(
            this,
            getString(R.string.dialog_title_permission_needed),   // Title text
            getString(R.string.dialog_message_permission_needed), // Message text
            false,
            true,
            null,
            getString(R.string.back_button)
        )

        d.noButton.setOnClickListener {
            d.dialog.dismiss()
        }
        d.dialog.setOnDismissListener {
            // Finish with a bad result when dialog is removed
            setResult(RESULT_CANCELED)
            finish()
        }
        d.dialog.show()

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

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
        
        adapter.onItemClickListener = {
            // Check if the chosen pair has a user
            if(it.second != null){
                resIntent.putExtra(Consts.Extras.CHOOSECONTACT_CHOSEN_USER_ID, it.second!!.databaseId)
                setResult(RESULT_OK, resIntent)
                finish()
            }
            else{
                Toast.makeText(this, "NO USER", Toast.LENGTH_SHORT).show()
            }
        }
    }

}



