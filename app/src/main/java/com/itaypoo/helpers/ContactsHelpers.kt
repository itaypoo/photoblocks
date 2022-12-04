package com.itaypoo.helpers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.Log

data class ContactModel(
    // A model class for holding a contacts data
    val displayName: String,
    val phoneNumber: String
)

object ContactsUtils {
    private var contactList: MutableList<ContactModel>? = null


    // Retrun validated phone number
    fun validatedPhoneNumber(phoneNum: String): String?{
        // If phone number is valid (global number +XXX...), returns the normalized number
        // if its not valid, returns null
        if(PhoneNumberUtils.toaFromString(phoneNum) != PhoneNumberUtils.TOA_International) return null
        else return PhoneNumberUtils.normalizeNumber(phoneNum)
    }

    @SuppressLint("Range")
    fun getList(resolver: ContentResolver): MutableList<ContactModel>{
        if(contactList != null) return contactList!!
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

            val validated = validatedPhoneNumber(contactNumber)
            if(validated != null){
                // Phone number is valid
                val model = ContactModel(contactName, validated)
                if(!contactList!!.contains(model)){
                    // Add to list if it doesn't contain this contact
                    contactList!!.add(model)
                }
            }

        }
        cursor.close()

        return contactList!!
    }

}