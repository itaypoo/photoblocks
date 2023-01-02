package com.itaypoo.photoblockslib

import java.util.Date


class PendingBlockInvitation(

    databaseId: String?,
    creationTime: Date,

    val inviterId: String,
    val phoneNumber: String,
    val blockId: String

): DBEntity(databaseId, creationTime) {

    override fun toHashMap(): HashMap<String, Any> {
        val res = hashMapOf(
            "inviterId" to inviterId,
            "phoneNumber" to phoneNumber,
            "blockId" to blockId
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}