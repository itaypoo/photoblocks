package com.itaypoo.photoblockslib

import java.util.Date

class BlockPost(
    databaseId: String?,
    creationTime: Date,

    val imageUrl: String,
    val blockId: String,
    val creatorId: String,
    val description: String
): DBEntity(databaseId, creationTime) {

    override fun toHashMap(): HashMap<String, Any>{
        val res = hashMapOf(
            "imageUrl" to imageUrl,
            "blockId" to blockId,
            "creatorId" to creatorId,
            "description" to description
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}