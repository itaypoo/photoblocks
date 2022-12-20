package com.itaypoo.helpers

import com.google.firebase.firestore.DocumentSnapshot
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.Notification
import com.itaypoo.photoblockslib.User

object FirebaseUtils {

    internal object DefaultObjects{

        // Create a default user. Only requirement for creating a User is a phone number.
        fun User(phoneNum: String): User{
            return User(
                null,
                Consts.Defaults.USER_NAME,
                phoneNum,
                Consts.Defaults.USER_PFP_URL,
                AppUtils.currentTimeString(),
                Consts.Defaults.USER_PRIVACY_MODE
            )
        }

        // Create a default block made by the given creator
        fun Block(creator: User): Block{
            return Block(
                null,
                Consts.Defaults.BLOCK_NAME,
                creator.databaseId!!,
                Consts.Defaults.BLOCK_COVER_IMAGE_URL,
                AppUtils.currentTimeString(),
                Consts.Defaults.BLOCK_PRIMARY_COLOR,
                Consts.Defaults.BLOCK_SECONDARY_COLOR,
                Consts.Defaults.BLOCK_COLLAGE_ENABLED,
                Consts.Defaults.BLOCK_COLLAGE_ORDERED_BY_LIKES,
                Consts.Defaults.BLOCK_COLLAGE_IMAGE_TIME
            )
        }

    }

    internal object ObjectFromDoc{

        // Generate a user object from a firebase DocumentSnapshot
        fun User(doc: DocumentSnapshot): User {
            return User(
                doc.id,
                doc.get("name") as String,
                doc.get("phoneNumber") as String,
                doc.get("profilePhotoUrl") as String,
                doc.get("creationTime") as String,
                doc.get("isPrivate") as Boolean
            )
        }

        fun Block(doc: DocumentSnapshot): Block {
            return Block(
                doc.id,
                doc.get("title") as String,
                doc.get("creatorId") as String,
                doc.get("coverImageUrl") as String,
                doc.get("creationTime") as String,
                doc.get("primaryColor") as Number,
                doc.get("secondaryColor") as Number,
                doc.get("collageEnabled") as Boolean,
                doc.get("collageOrderedByLikes") as Boolean,
                (doc.get("collageImageTime") as Number).toInt()
            )
        }

        fun Notification(doc: DocumentSnapshot): Notification{
            return com.itaypoo.photoblockslib.Notification(
                doc.id,
                doc.get("recipientId") as String,
                (doc.get("type") as Number).toInt(),
                doc.get("content") as String
            )
        }

    }

}