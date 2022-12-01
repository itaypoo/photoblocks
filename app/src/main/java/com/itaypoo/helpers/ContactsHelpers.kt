package com.itaypoo.helpers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.ContactsContract
import android.util.Log

data class ContactModel(
    // A model class for holding a contacts data
    val displayName: String,
    val phoneNumber: String
)

object ContactsUtils {
    private var contactList: MutableList<ContactModel>? = null

    @SuppressLint("Range")

    fun getList(resolver: ContentResolver): MutableList<ContactModel>?{
        if(contactList != null) return contactList
        // If contact list was not yet generated
        contactList = mutableListOf()

        // Get cursor with contact display name and number
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
                ), null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

        // Loop trough all the contacts in the cursor and generate a
        // ContactModel for each contact
        while(cursor!!.moveToNext()){
            val contactName = cursor.getString( cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) )
            val contactNumber = cursor.getString( cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) )
            val model = ContactModel(contactName, contactNumber)

            if(!contactList!!.contains(model)){
                contactList!!.add(model)
                Log.e("ContactsUtils", model.displayName)
                // TODO: ADD CHECK FOR DUPLICATE CONTACTS
            }
        }
        cursor.close()

        return contactList
    }

}