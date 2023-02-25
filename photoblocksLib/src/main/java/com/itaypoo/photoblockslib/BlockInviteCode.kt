package com.itaypoo.photoblockslib

import java.util.Date

class BlockInviteCode(

    databaseId: String?,
    creationTime: Date,

    val blockId: String,
    val code: String,

    ) : DBEntity(databaseId, creationTime) {

    override fun toHashMap(): HashMap<String, Any>{
        val res = hashMapOf(
            "blockId" to blockId,
            "code" to code,
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}