package com.itaypoo.helpers

import android.graphics.Color

object Consts {
    // Request Codes ( for startActivityForResult() )
    internal object RequestCode{
        const val GALLERY_PICKER_SINGLE = 10
        const val GALLERY_PICKER_MULTIPLE = 20
        const val CROP_IMAGE_ACTIVITY = 30
        const val CURATED_PHOTOS_ACTIVITY = 40
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

        const val CHOOSECONTACT_CHOSEN_USER_ID = "chosen_user_id"
    }

    // Identify how a user logged in (used in home page extras)
    internal object LoginType{
        const val NO_LOGIN = 0
        const val EXISTING_USER = 1
        const val NEW_USER = 2
    }

}