package com.itaypoo.photoblockslib

object NotificationType{
    const val BLOCK_INVITATION = 1
}

//////////////////////////////////////////////////////////////////////////////////////////////////

data class Notification(
    val databaseId: String?,
    val recipientId: String,
    val type: Int,
    val content: String
) {

    fun toHashMap(): HashMap<String, Any>{
        return hashMapOf(
            "recipientId" to recipientId,
            "type" to type,
            "content" to content
        )
    }

}

//////////////////////////////////////////////////////////////////////////////////////////////////

object NotificationContent{
    data class BlockInvitation(val blockId: String, val inviterId: String)

    // Parse content from a given notification
    fun parseContent(notif: Notification): Any? {

        if(notif.type == NotificationType.BLOCK_INVITATION){
            // Notification is a block invitation
            val stringList = notif.content.split(" ")
            return BlockInvitation(stringList[0], stringList[1])
        }
        else{
            return null
        }

    }

    fun makeContentBlockInvitation(blockId: String, inviterId: String): String{
        return blockId + " " + inviterId
    }
}