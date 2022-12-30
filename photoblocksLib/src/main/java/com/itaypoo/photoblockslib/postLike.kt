package com.itaypoo.photoblockslib

class postLike(
    documentId: String?,
    creationDayTime: DayTimeStamp,

    val userId: String, // Who liked the post
    val postId: String  // What post they liked
): DBEntity(documentId, creationDayTime) {

    override fun toHashMap(): HashMap<String, Any> {
        val res = hashMapOf<String, Any>(
            "userId" to userId,
            "postId" to postId
        ) + super.toHashMap()
        return res as HashMap<String, Any>
    }

}