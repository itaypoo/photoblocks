package com.itaypoo.photoblockslib

import java.util.Date

class BlockComment(

    databaseId: String?,
    creationTime: Date,

    val authorId: String,
    val blockId: String,
    val content: String,

) : DBEntity(databaseId, creationTime) {

    override fun toHashMap(): HashMap<String, Any>{
        val res = hashMapOf(
            "authorId" to authorId,
            "blockId" to blockId,
            "content" to content,
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}