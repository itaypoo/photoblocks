package com.itaypoo.photoblockslib

data class Block(
    var databaseId: String?,
    var title: String,
    var creatorId: String,
    var coverImageUrl: String,
    var primaryColor: Number,
    var secondaryColor: Number,
    var creationTime: Number,
    var members: List<String>,
    ) {

    fun toHashMap(): HashMap<String, Any>{
        return hashMapOf<String, Any>(
            "title" to title,
            "creatorId" to creatorId,
            "coverImageUrl" to coverImageUrl,
            "primaryColor" to primaryColor,
            "secondaryColor" to secondaryColor,
            "creationTime" to creationTime,
            "members" to members
        )
    }

}