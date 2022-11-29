package com.itaypoo.photoblockslib

import java.util.Calendar

data class User(

    var databaseId: String?,
    var name: String,
    var phoneNumber: String,
    var profilePhotoUrl: String,
    var creationTime: String,
    var isPrivate: Boolean

) {

    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "name" to name,
            "phoneNumber" to phoneNumber,
            "profilePhotoUrl" to profilePhotoUrl,
            "creationTime" to creationTime,
            "isPrivate" to isPrivate
        )
    }

}