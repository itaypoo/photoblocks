package com.itaypoo.photoblockslib

class PendingBlockInvitation(

    databaseId: String?,
    creationDayTime: DayTimeStamp,

    val inviterId: String,
    val phoneNumber: String,
    val blockId: String

): DBEntity(databaseId, creationDayTime) {

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