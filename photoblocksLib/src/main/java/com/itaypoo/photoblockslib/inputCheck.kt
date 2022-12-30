package com.itaypoo.photoblockslib

object inputCheck {

    // Check user name input
    const val USER_NAME_VALID = 0
    const val USER_NAME_TOO_SHORT = 1
    const val USER_NAME_TOO_LONG = 2

    private val userNameMinLength = 2
    private val userNameMaxLength = 15

    fun validateUserName(name: String?): Int{
        if(name != null){
            if(name.length < userNameMinLength) return USER_NAME_TOO_SHORT
            else if(name.length > userNameMaxLength) return USER_NAME_TOO_LONG
            else return USER_NAME_VALID
        }
        else return USER_NAME_TOO_SHORT
    }

    // Check block name input
    const val BLOCK_NAME_VALID = 0
    const val BLOCK_NAME_TOO_SHORT = 1
    const val BLOCK_NAME_TOO_LONG = 2

    private val blockNameMinLength = 4
    private val blockNameMaxLength = 25

    fun validateBlockName(name: String?): Int{
        if(name != null){
            if(name.length < blockNameMinLength) return BLOCK_NAME_TOO_SHORT
            else if(name.length > blockNameMaxLength) return BLOCK_NAME_TOO_LONG
            else return BLOCK_NAME_VALID
        }
        else return BLOCK_NAME_TOO_SHORT
    }

    // Check post description
    const val POST_DESCRIPTION_VALID = 0
    const val POST_DESCRIPTION_TOO_LONG = 1

    private val postDescriptionMaxLength = 30

    fun validatePostDescription(text: String): Int{
        if(text.length > postDescriptionMaxLength){
            return POST_DESCRIPTION_TOO_LONG
        }
        return POST_DESCRIPTION_VALID
    }

}