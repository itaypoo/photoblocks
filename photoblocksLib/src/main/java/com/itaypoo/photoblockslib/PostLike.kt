package com.itaypoo.photoblockslib

import java.util.Date


class PostLike(
    documentId: String?,
    creationTime: Date,

    val userId: String, // Who liked the post
    val postId: String  // What post they liked
): DBEntity(documentId, creationTime) {

    override fun toHashMap(): HashMap<String, Any> {
        val res = hashMapOf<String, Any>(
            "userId" to userId,
            "postId" to postId
        ) + super.toHashMap()
        return res as HashMap<String, Any>
    }

}