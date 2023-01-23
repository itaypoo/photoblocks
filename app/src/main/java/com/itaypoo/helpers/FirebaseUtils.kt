package com.itaypoo.helpers

import android.content.ContentResolver
import android.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.itaypoo.photoblockslib.*
import java.util.Date

object FirebaseUtils {

    internal object DefaultObjects{

        // Create a default user. Only requirement for creating a User is a phone number.
        fun User(phoneNum: String): User{
            return com.itaypoo.photoblockslib.User(
                null,
                Timestamp.now().toDate(),

                Consts.Defaults.USER_NAME,
                phoneNum,
                Consts.Defaults.USER_PFP_URL,
                Consts.Defaults.USER_PRIVACY_MODE,
                null
            )
        }

        // Create a default block made by the given creator
        fun Block(creator: User): Block{
            return Block(
                null,
                Timestamp.now().toDate(),

                Consts.Defaults.BLOCK_NAME,
                creator.databaseId!!,
                Consts.Defaults.BLOCK_COVER_IMAGE_URL,
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
        fun User(doc: DocumentSnapshot, contentResolver: ContentResolver): User {
            val res = User(
                doc.id,
                (doc.get("creationTime") as Timestamp).toDate(),

                doc.get("name") as String,
                doc.get("phoneNumber") as String,
                doc.get("profilePhotoUrl") as String,
                doc.get("isPrivate") as Boolean,
                doc.get("bannerImageUrl") as String?
            )

            // If this user exists in contacts, replace its database name with its name in the contact list
            // But! if this user is the current user, do not replace its name
            if(res.phoneNumber == AppUtils.currentUser?.phoneNumber){
                val nameInContacts = ContactsUtils.contactsListContainsNumber(res.phoneNumber, contentResolver)
                if(nameInContacts?.displayName != null) res.name = nameInContacts.displayName
            }

            return res
        }

        fun Block(doc: DocumentSnapshot): Block {
            return Block(
                doc.id,
                (doc.get("creationTime") as Timestamp).toDate(),

                doc.get("title") as String,
                doc.get("creatorId") as String,
                doc.get("coverImageUrl") as String,
                doc.get("primaryColor") as Number,
                doc.get("secondaryColor") as Number,
                doc.get("collageEnabled") as Boolean,
                doc.get("collageOrderedByLikes") as Boolean,
                (doc.get("collageImageTime") as Number).toInt()
            )
        }

        fun Notification(doc: DocumentSnapshot): Notification {
            return Notification(
                doc.id,
                (doc.get("creationTime") as Timestamp).toDate(),

                doc.get("recipientId") as String,
                doc.get("senderId") as String,
                (doc.get("type") as Number).toInt(),
                doc.get("content") as String
            )
        }

        fun BlockComment(doc: DocumentSnapshot): BlockComment {
            return BlockComment(
                doc.id,
                (doc.get("creationTime") as Timestamp).toDate(),

                doc.get("authorId") as String,
                doc.get("blockId") as String,
                doc.get("content") as String
            )
        }

        fun BlockPost(doc: DocumentSnapshot): BlockPost{
            return BlockPost(
                doc.id,
                (doc.get("creationTime") as Timestamp).toDate(),

                doc.get("imageUrl") as String,
                doc.get("blockId") as String,
                doc.get("creatorId") as String,
                doc.get("description") as String
            )
        }

        fun PendingBlockInvitation(doc: DocumentSnapshot): PendingBlockInvitation{
            return PendingBlockInvitation(
                doc.id,
                (doc.get("creationTime") as Timestamp).toDate(),

                doc.get("inviterId") as String,
                doc.get("phoneNumber") as String,
                doc.get("blockId") as String
            )
        }

        fun BlockMember(doc: DocumentSnapshot): BlockMember{
            return BlockMember(
                doc.id,
                (doc.get("creationTime") as Timestamp).toDate(),

                doc.get("blockId") as String,
                doc.get("memberId") as String,
                doc.get("isAdmin") as Boolean
            )
        }

    }

}