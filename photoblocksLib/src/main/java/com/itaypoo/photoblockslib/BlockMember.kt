package com.itaypoo.photoblockslib

import java.io.Serializable
import java.util.Date

class BlockMember(

    databaseId: String?,
    creationTime: Date,

    var blockId: String,
    var memberId: String,
    var isAdmin: Boolean

    ) : Serializable, DBEntity(databaseId, creationTime) {

    override fun toHashMap(): HashMap<String, Any>{
        val res = hashMapOf(
            "blockId" to blockId,
            "memberId" to memberId,
            "isAdmin" to isAdmin
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}
