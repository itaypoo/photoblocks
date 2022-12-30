package com.itaypoo.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.toColor
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.photoblocks.HomeScreenActivity
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.User
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object AppUtils {
    var storageRef: StorageReference
    var currentUser: User? = null
    // If this is true, the home screen will reload its block list once it is on screen.

    lateinit var homeScreenActivity: HomeScreenActivity

    init {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        storageRef = FirebaseStorage.getInstance().reference
    }

    // Save a bitmap to private internal storage
    fun saveBitmapToPrivateInternal(bitmap: Bitmap, fileName: String, context: Context): String? {
        var fileName: String? = fileName // No .png or .jpg needed
        try {
            // Compress bitmap into a ByteArray
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            // Save the ByteArray to private storage with a FileOutputStream
            val outputStream: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            outputStream.write(bytes.toByteArray())
            // Remember close file output
            outputStream.close()
        } catch (exep: Exception) {
            // Catch exception, return null
            exep.printStackTrace()
            fileName = null
        }
        return fileName
    }

    // Get a bitmap that was saved in internal storage (Preferably from saveBitmapToPrivateInternal())
    fun getBitmapFromPrivateInternal(fileName: String, context: Context): Bitmap{
        val inputStream = context.openFileInput(fileName)
        val res = BitmapFactory.decodeStream(inputStream)
        // Remember to close input stream
        inputStream.close()

        return res
    }

    // Invert a color using its RGB values
    fun invertColor(color: Int, alpha: Int): Int{
        val red: Int = ( 255 - color.toColor().red() ).toInt()
        val green: Int = ( 255 - color.toColor().green() ).toInt()
        val blue: Int = ( 255 - color.toColor().blue() ).toInt()

        return Color.argb(alpha, red, green, blue)
    }

    fun getOrdinalNumberAddon(num: Int): String{
        if(num == 1) return "st"
        if(num == 2) return "nd"
        if(num == 3) return "rd"
        else return "th"
    }

}