package com.itaypoo.helpers

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.itaypoo.photoblocks.R

object CustomDialogMaker {
    // A class for easily creating dialogs to be used instead of the default dialogs.
    open class CustomDialog(val dialog: Dialog)

    // LoadingDialog - A dialog with a loading spinner and a message. Cannot be dismissed (!!).
    //region LoadingDialog

    fun makeLoadingDialog(
        context: Context,
        messageText: String
    ): CustomDialog {

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

        // return the dialog with its buttons
        return CustomDialog(dialog)
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

    class TextInputDialog(dialog: Dialog, val editText: EditText, val doneButton: Button, val cancelButton: Button, val errorTextView: TextView): CustomDialog(dialog)

    fun makeTextInputDialog(
        context: Context,
        titleText: String,
        hint: String,
        hideCancelButton: Boolean = false,
        isCancelable: Boolean = true,
        overrideDoneText: String? = null,
        overrideCancelText: String? = null
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
        if(hideCancelButton) cancelButton.visibility = View.GONE
        if(overrideDoneText != null) doneButton.text = overrideDoneText
        if(overrideCancelText != null) cancelButton.text = overrideCancelText

        return TextInputDialog(dialog, editText, doneButton, cancelButton, errorTextView)

    }

    //endregion

}