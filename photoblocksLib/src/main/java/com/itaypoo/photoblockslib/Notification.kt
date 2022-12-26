package com.itaypoo.photoblockslib

object NotificationType{
    // Invitation to a block
    // Content: Comment ID
    const val BLOCK_COMMENT = 1

    // Invitation to a block
    // Content: Block ID
    const val BLOCK_INVITATION = 2

    // Post liked notification
    // Content: Post ID
    const val POST_LIKE = 3
}

//////////////////////////////////////////////////////////////////////////////////////////////////

class Notification(

    databaseId: String?,
    creationDayTime: DayTimeStamp,

    val recipientId: String,
    val senderId: String,
    val type: Int,
    val content: String

) : DBEntity(databaseId, creationDayTime) {

    override fun toHashMap(): HashMap<String, Any>{
        val res = hashMapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to type,
            "content" to content
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}