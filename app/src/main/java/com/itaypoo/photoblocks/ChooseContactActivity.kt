package com.itaypoo.photoblocks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itaypoo.helpers.ContactModel
import com.itaypoo.helpers.ContactsUtils
import com.itaypoo.photoblocks.databinding.ActivityChooseContactBinding
import kotlinx.coroutines.*

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
        //contactsThatHaveUser = getContactsThatHaveUser(contactsList)

        GlobalScope.launch(Dispatchers.IO) {
            for(c in contactsList){
                val res = async { contactHasUser(c) }
                Log.d("This", "Name: ${c.displayName}    , Count: ${res.await()}")
                while(res == null){}
            }

        }
        Log.d("This", "After")
    }

    suspend fun contactHasUser(contact: ContactModel): Int {
        var count = 0

        var complete = false

        // Start a query of counting the users with this contacts phone number
        val countQuery = database.collection("users").whereEqualTo("phoneNumber", contact.phoneNumber).count().get(AggregateSource.SERVER)
        countQuery.addOnSuccessListener {
            count = it.count.toInt()
        }.addOnCompleteListener{
            complete = true
        }

        // Wait until query is complete
        while (!complete) {
            delay(1)
        }


        return count
    }

}



