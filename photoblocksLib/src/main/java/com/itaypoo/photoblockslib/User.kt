package com.itaypoo.photoblockslib

import java.io.Serializable
import java.util.Date

class User(

    databaseId: String?,
    creationTime: Date,

    var name: String,
    var phoneNumber: String,
    var profilePhotoUrl: String,
    var isPrivate: Boolean

) : Serializable, DBEntity(databaseId, creationTime) {

    override fun toHashMap(): HashMap<String, Any> {
        val res = hashMapOf(
            "name" to name,
            "phoneNumber" to phoneNumber,
            "profilePhotoUrl" to profilePhotoUrl,
            "isPrivate" to isPrivate
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}