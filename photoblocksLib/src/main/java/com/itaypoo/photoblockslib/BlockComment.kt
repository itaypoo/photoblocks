package com.itaypoo.photoblockslib

class BlockComment(

    databaseId: String?,
    creationDayTime: DayTimeStamp,

    val authorId: String,
    val blockId: String,
    val content: String,

) : DBEntity(databaseId, creationDayTime) {

    override fun toHashMap(): HashMap<String, Any>{
        val res = hashMapOf(
            "authorId" to authorId,
            "blockId" to blockId,
            "content" to content,
        ) + super.toHashMap()
        res as HashMap<String, Any>
        return res
    }

}