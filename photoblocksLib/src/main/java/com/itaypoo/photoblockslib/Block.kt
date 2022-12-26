package com.itaypoo.photoblockslib

import java.io.Serializable


class Block(

    databaseId: String?,
    creationDayTime: DayTimeStamp,

    var title: String,
    var creatorId: String,
    var coverImageUrl: String,

    var primaryColor: Number,
    var secondaryColor: Number,

    var collageEnabled: Boolean,
    var collageOrderedByLikes: Boolean,
    var collageImageTime: Int

    ) : Serializable, DBEntity(databaseId, creationDayTime) {

    override fun toHashMap(): HashMap<String, Any>{
        var res = hashMapOf<String, Any>(
            "title" to title,
            "creatorId" to creatorId,
            "coverImageUrl" to coverImageUrl,
            "primaryColor" to primaryColor,
            "secondaryColor" to secondaryColor,
            "collageEnabled" to collageEnabled,
            "collageOrderedByLikes" to collageOrderedByLikes,
            "collageImageTime" to collageImageTime
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}