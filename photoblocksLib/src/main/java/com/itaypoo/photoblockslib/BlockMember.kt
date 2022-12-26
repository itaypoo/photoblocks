package com.itaypoo.photoblockslib

import java.io.Serializable
import java.util.Calendar

class BlockMember(

    databaseId: String?,
    creationDayTime: DayTimeStamp,

    var blockId: String,
    var memberId: String,
    var isAdmin: Boolean

    ) : Serializable, DBEntity(databaseId, creationDayTime) {

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
