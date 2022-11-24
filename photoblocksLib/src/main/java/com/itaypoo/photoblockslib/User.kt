package com.itaypoo.photoblockslib

data class User(
    var databaseId: String?,

    var name: String,
    var phoneNumber: String,
    var profilePhotoUrl: String,

    var creationYear: Number,
    var creationDay: Number,
    var creationMinute: Number,

    var isPrivate: Boolean,

    var blocksJoined: List<String>,
    var blockInvitations: List<String>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (databaseId != other.databaseId) return false
        if (name != other.name) return false
        if (phoneNumber != other.phoneNumber) return false
        if (profilePhotoUrl != other.profilePhotoUrl) return false
        if (creationYear != other.creationYear) return false
        if (creationDay != other.creationDay) return false
        if (creationMinute != other.creationMinute) return false
        if (isPrivate != other.isPrivate) return false
        if (!blocksJoined.equals(other.blocksJoined)) return false
        if (!blockInvitations.equals(other.blockInvitations)) return false

        return true
    }

    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "name" to name,
            "phoneNumber" to phoneNumber,
            "profilePhotoUrl" to profilePhotoUrl,
            "creationYear" to creationYear,
            "creationDay" to creationDay,
            "creationMinute" to creationMinute,
            "isPrivate" to isPrivate,
            "blocksJoined" to blocksJoined,
            "blockInvitations" to blockInvitations
        )
    }

}