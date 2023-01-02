package com.itaypoo.photoblockslib

import java.io.Serializable
import java.util.Date

open class DBEntity(
    var databaseId: String?,
    val creationTime: Date
) : Serializable {
    open fun toHashMap(): HashMap<String, Any>{
        return hashMapOf(
            "creationTime" to creationTime
        )
    }
}