package com.itaypoo.helpers

import android.graphics.Color

object Consts {
    internal object Notifs{
        const val FOREGROUND_SERVICE_CHANNEL_ID = "upload_images_service_channel_id"
    }

    // Request Codes ( for startActivityForResult() )
    internal object RequestCode{
        const val GALLERY_PICKER_SINGLE = 10
        const val GALLERY_PICKER_MULTIPLE = 20
        const val CROP_IMAGE_ACTIVITY = 30
        const val CURATED_PHOTOS_ACTIVITY = 40
        const val CHOOSE_CONTACT_USER_ACTIVITY = 50
        const val VIEW_BLOCK_NO_RETURN = 60
    }

    // Default values
    internal object Defaults{
        const val USER_PFP_URL = "https://firebasestorage.googleapis.com/v0/b/photoblocks-1a69c.appspot.com/o/defaults%2FdefaultProfilePicture.png?alt=media&token=7c70bc21-5616-4402-b7d5-ae55758f6f24"
        const val USER_NAME = "photoblocks user"
        const val USER_PRIVACY_MODE = false

        const val BLOCK_NAME = "new block"
        const val BLOCK_COVER_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/photoblocks-1a69c.appspot.com/o/defaults%2FdefaultCoverImage.png?alt=media&token=83dd2244-2c86-49c5-a3f6-5d9cae5c0751"
        val BLOCK_PRIMARY_COLOR = Color.parseColor("#6E7478")
        val BLOCK_SECONDARY_COLOR = Color.parseColor("#51708A")
        const val BLOCK_COLLAGE_ENABLED = true
        const val BLOCK_COLLAGE_ORDERED_BY_LIKES = true
        const val BLOCK_COLLAGE_IMAGE_TIME = 5000

        const val DATE_FORMAT = "MM-dd-yyyy-HH-mm-ss"
    }

    // Shared preferences keys and path
    internal object SharedPrefs{
        const val PATH = "com.itaypoo.photoblocks.sharedprefs"
        const val SAVED_USER_ID_KEY = "savedUserId"
    }

    // ResultActivities inputs/outputs extras
    internal object Extras{
        const val CROP_INPUT_RATIO = "crop_one_to_one"
        const val CROP_OUTPUT_CROPPEDFILENAME = "crop_cropped_image"
        const val CURATED_OUTPUT_DATABASEID = "curated_image_database_id"
        // Crop ratio presets for cropping images
        const val RATIO_NORMAL = 0
        const val RATIO_ONE_TO_ONE = 1
        const val RATIO_BLOCK_COVER = 2

        const val SIGNIN_TYPE = "signin_type"

        const val CHOOSECONTECT_INPUT_CHOOSE_TYPE = "choose_type"
        const val CHOOSECONTACT_OUTPUT_USER = "chosen_user_id"
        const val CHOOSECONTACT_OUTPUT_CONTACT = "chosen_contact"

        const val UPLOADPOST_INPUT_BLOCKID = "upload_block_id"

        const val SERVICE_POSTS_TO_UPLOAD_URI_LIST = "posts_to_upload_uri_list"
        const val SERVICE_POSTS_TO_UPLOAD_STRING_LIST = "posts_to_upload_string_list"
        const val SERVICE_POSTS_BLOCK_ID = "block_id_to_upload_to"

        // Generic extra key for passing classes between activities.
        const val PASSED_BLOCK = "passed_block"
        const val PASSED_USER = "passed_user"
    }

    // Choose types for ChooseContactActivity
    internal object ChooseType{
        const val CHOOSE_ANY_USER = 1
        const val CHOOSE_BLOCK_INVITE = 2
    }

    // Identify how a user logged in (used in home page extras)
    internal object LoginType{
        const val NO_LOGIN = 0
        const val EXISTING_USER = 1
        const val NEW_USER = 2
    }

}