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

data class Notification(
    val databaseId: String?,
    val recipientId: String,
    val senderId: String,
    val type: Int,
    val content: String
) {

    fun toHashMap(): HashMap<String, Any>{
        return hashMapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to type,
            "content" to content
        )
    }

}