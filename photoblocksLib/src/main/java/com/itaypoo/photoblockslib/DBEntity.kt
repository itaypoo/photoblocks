package com.itaypoo.photoblockslib

import java.io.Serializable

open class DBEntity(
    var databaseId: String?,
    val creationDayTime: DayTimeStamp
) : Serializable {
    open fun toHashMap(): HashMap<String, Any>{
        return hashMapOf(
            "creationDayTime" to creationDayTime.toString()
        )
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////

fun earlierDate(e1: DayTimeStamp, e2: DayTimeStamp): DayTimeStamp {

    if(e2.year < e1.year){
        return e2
    }
    else if(e2.dayOfYear < e1.dayOfYear){
        return e2
    }
    else if(e2.secondOfDay < e1.secondOfDay){
        return e2
    }
    else{
        return e1
    }

}