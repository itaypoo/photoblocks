package com.itaypoo.photoblockslib

import java.util.Calendar

data class BlockMember(

    var databaseId: String?,
    var blockId: String,
    var memberId: String,
    var memberSinceTime: String

    ) {

    fun toHashMap(): HashMap<String, Any>{
        return hashMapOf(
            "blockId" to blockId,
            "memberId" to memberId,
            "memberSinceTime" to memberSinceTime
        )
    }

}
