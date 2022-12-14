package com.itaypoo.helpers

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.User

object CustomDialogMaker {
    // A class for easily creating dialogs to be used instead of the default dialogs.
    open class CustomDialog(val dialog: Dialog)

    // LoadingDialog - A dialog with a loading spinner and a message. Cannot be dismissed (!!).
    //region LoadingDialog

    class LoadingDialog(dialog: Dialog, private val progressBar: ProgressBar): CustomDialog(dialog){
        fun setProgressMin(value: Int){
            progressBar.min = value
        }
        fun setProgressMax(value: Int){
            progressBar.max = value
        }
        fun setProgress(value: Int){
            progressBar.progress = value
        }
    }

    fun makeLoadingDialog(
        context: Context,
        messageText: String,
        progressBarEnabled: Boolean = false
    ): LoadingDialog {

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_loading)
        dialog.setCancelable(false)

        // Set dialog window width, height, background and position
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        // Init views
        dialog.findViewById<TextView>(R.id.dialogLoading_text).text = messageText

        // Progress bar
        val progressBar = dialog.findViewById<ProgressBar>(R.id.dialogLoading_progressBar)
        val loadingCircle = dialog.findViewById<ProgressBar>(R.id.dialogLoading_loadingCircle)

        progressBar.visibility = if(progressBarEnabled) { View.VISIBLE } else { View.GONE }
        loadingCircle.visibility = if(!progressBarEnabled) { View.VISIBLE } else { View.GONE }

        // return the dialog with its buttons
        return LoadingDialog(dialog, progressBar)
    }

    //endregion

    // YesNoDialog - A simple dialog containing a message, and a yes and a no button.
    //region YesNoDialog

    class YesNoDialog(dialog: Dialog, val yesButton: Button, val noButton: Button): CustomDialog(dialog)

    fun makeYesNoDialog(
        context: Context,
        titleText: String,
        messageText: String,
        hideNoButton: Boolean = false,
        hideYesButton: Boolean = false,
        overrideYesText: String? = null,
        overrideNoText: String? = null
    ): YesNoDialog {

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_yesno)

        // Set dialog window width, height, background and position
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        // Get dialog views
        val yesButton = dialog.findViewById<Button>(R.id.yesnoDialog_yesButton)
        val noButton = dialog.findViewById<Button>(R.id.yesnoDialog_noButton)

        // Init views
        dialog.findViewById<TextView>(R.id.yesnoDialog_titleText).text = titleText
        dialog.findViewById<TextView>(R.id.yesnoDialog_contentText).text = messageText

        if (hideYesButton) yesButton.visibility = View.GONE
        if (hideNoButton) noButton.visibility = View.GONE

        if (overrideYesText != null) yesButton.text = overrideYesText
        if (overrideNoText != null) noButton.text = overrideNoText

        // return the dialog with its buttons
        return YesNoDialog(dialog, yesButton, noButton)

    }

    //endregion

    // TextInputDialog - A dialog for getting text from the user.
    //region TextInputDialog

    class TextInputDialog(dialog: Dialog, val editText: EditText, val doneButton: Button, val cancelButton: Button, private val errorTextView: TextView): CustomDialog(dialog){
        fun setError(text: String){
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = text
        }
        fun hideError(){
            errorTextView.visibility = View.GONE
        }
    }

    fun makeTextInputDialog(
        context: Context,
        titleText: String,
        hint: String,
        hideCancelButton: Boolean = false,
        isCancelable: Boolean = true,
        overrideDoneText: String? = null,
        overrideCancelText: String? = null,
        editTextStartText: String = "",
    ): TextInputDialog {

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_text_input)
        dialog.setCancelable(isCancelable)

        // Set dialog window width, height, background and position
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        // Get dialog views
        val editText = dialog.findViewById<EditText>(R.id.inputDialog_editText)
        val doneButton = dialog.findViewById<Button>(R.id.inputDialog_doneButton)
        val cancelButton = dialog.findViewById<Button>(R.id.inputDialog_cancelButton)
        val errorTextView = dialog.findViewById<TextView>(R.id.inputDialog_errorText)

        // Init views
        dialog.findViewById<TextView>(R.id.inputDialog_titleText).text = titleText
        errorTextView.visibility = View.GONE
        editText.hint = hint
        editText.setText(editTextStartText)
        if(hideCancelButton) cancelButton.visibility = View.GONE
        if(overrideDoneText != null) doneButton.text = overrideDoneText
        if(overrideCancelText != null) cancelButton.text = overrideCancelText

        return TextInputDialog(dialog, editText, doneButton, cancelButton, errorTextView)

    }

    //endregion

    // UserProfileDialog - A dialog that displays a users pfp, name and phone number with a yes and a no button.
    //region UserProfileDialog

    fun makeUserProfileDialog(
        context: Context,
        user: User,
        hideNoButton: Boolean = false,
        hideYesButton: Boolean = false,
        overrideYesText: String? = null,
        overrideNoText: String? = null,
        overrideNameText: String? = null
    ): YesNoDialog {

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_user_profile)

        // Set dialog window width, height, background and position
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        // Get dialog views
        val yesButton = dialog.findViewById<Button>(R.id.userProfileDialog_yesButton)
        val noButton = dialog.findViewById<Button>(R.id.userProfileDialog_noButton)
        val nameText = dialog.findViewById<TextView>(R.id.userProfileDialog_userNameText)
        val profileImageView = dialog.findViewById<ImageView>(R.id.userProfileDialog_pfpImage)

        // Init views
        dialog.findViewById<TextView>(R.id.userProfileDialog_numberText).text = user.phoneNumber
        nameText.text = user.name

        // Load user pfp
        Glide.with(context).load(user.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(profileImageView)

        if (hideYesButton) yesButton.visibility = View.GONE
        if (hideNoButton) noButton.visibility = View.GONE

        if(overrideNameText != null) nameText.text = overrideNameText
        if (overrideYesText != null) yesButton.text = overrideYesText
        if (overrideNoText != null) noButton.text = overrideNoText

        // return the dialog with its buttons
        return YesNoDialog(dialog, yesButton, noButton)

    }

    //endregion

    // BlockViewDialog - A dialog that displays a block with a yes and a no button.
    //region UserProfileDialog

    fun makeBlockViewDialog(
        context: Context,
        block: Block,
        hideNoButton: Boolean = false,
        hideYesButton: Boolean = false,
        overrideYesText: String? = null,
        overrideNoText: String? = null,
    ): YesNoDialog {

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_block_view)

        // Set dialog window width, height, background and position
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        // Get dialog views
        val yesButton = dialog.findViewById<Button>(R.id.blockViewDialog_yesButton)
        val noButton = dialog.findViewById<Button>(R.id.blockViewDialog_noButton)
        val blockTitleText = dialog.findViewById<TextView>(R.id.blockViewDialog_blockTitleText)
        val blockImage = dialog.findViewById<ImageView>(R.id.blockViewDialog_blockImage)
        val blockGradient = dialog.findViewById<ImageView>(R.id.blockViewDialog_blockGradient)

        // Init views
        blockTitleText.text = block.title
        blockGradient.imageTintList = ColorStateList.valueOf( block.secondaryColor.toInt() )

        // Load block image
        Glide.with(context).load(block.coverImageUrl).placeholder(R.drawable.default_block_cover).into(blockImage)

        if (hideYesButton) yesButton.visibility = View.GONE
        if (hideNoButton) noButton.visibility = View.GONE

        if (overrideYesText != null) yesButton.text = overrideYesText
        if (overrideNoText != null) noButton.text = overrideNoText

        // return the dialog with its buttons
        return YesNoDialog(dialog, yesButton, noButton)

    }

    //endregion
}