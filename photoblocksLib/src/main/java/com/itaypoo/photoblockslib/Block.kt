package com.itaypoo.photoblockslib

import java.io.Serializable


data class Block(

    var databaseId: String?,
    var title: String,
    var creatorId: String,
    var coverImageUrl: String,
    var creationTime: String,

    var primaryColor: Number,
    var secondaryColor: Number,

    var collageEnabled: Boolean,
    var collageOrderedByLikes: Boolean,
    var collageImageTime: Int

    ) : Serializable {

    fun toHashMap(): HashMap<String, Any>{
        return hashMapOf<String, Any>(
            "title" to title,
            "creatorId" to creatorId,
            "coverImageUrl" to coverImageUrl,
            "creationTime" to creationTime,
            "primaryColor" to primaryColor,
            "secondaryColor" to secondaryColor,
            "collageEnabled" to collageEnabled,
            "collageOrderedByLikes" to collageOrderedByLikes,
            "collageImageTime" to collageImageTime
        )
    }

}